package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zifang.z.script.core.domain.entity.ApiKeyDO;
import com.zifang.z.script.core.domain.mapper.ApiKeyMapper;
import com.zifang.z.script.core.domain.service.RetryPolicyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FEATURE051 - t17 失败重试策略实现
 * <p>
 * 重试触发策略 (retry_on 字段):
 * - HTTP_5XX: 仅 5xx 状态码重试 (推荐)
 * - TIMEOUT:  仅 timeout 异常重试
 * - ALL:      任何非 2xx 都重试
 * - NONE:     不重试
 * <p>
 * 熔断器: 连续失败 N 次后, 后续调用直接拒绝 (直到下一次 resetFailures)
 */
@Service
public class RetryPolicyServiceImpl implements RetryPolicyService {

    private static final Logger log = LogManager.getLogger(RetryPolicyServiceImpl.class);
    /**
     * 连续失败计数 (内存, 重启后清空 - 简单实用)
     */
    private final ConcurrentHashMap<Long, AtomicInteger> failureCounter = new ConcurrentHashMap<>();
    @Autowired
    private ApiKeyMapper apiKeyMapper;

    @Override
    public boolean shouldRetry(ApiKeyDO apiKey, int httpStatus, String errorMessage) {
        if (apiKey == null) {
            return false;
        }

        Integer maxRetry = apiKey.getMaxRetry();
        if (maxRetry == null || maxRetry <= 0) {
            return false;
        }


        String retryOn = apiKey.getRetryOn();
        if (retryOn == null) {
            retryOn = "HTTP_5XX";
        }


        switch (retryOn.toUpperCase()) {
            case "ALL":
                return httpStatus < 200 || httpStatus >= 300;
            case "TIMEOUT":
                return errorMessage != null && errorMessage.toLowerCase().contains("timeout");
            case "NONE":
                return false;
            case "HTTP_5XX":
            default:
                return httpStatus >= 500 && httpStatus < 600;
        }
    }

    @Override
    public long computeBackoffMs(ApiKeyDO apiKey, int retryNo) {
        if (apiKey == null || apiKey.getRetryBackoffMs() == null) return 1000;

        // 简单指数退避: backoff * 2^(retryNo-1)
        // 第 1 次: backoff; 第 2 次: backoff*2; 第 3 次: backoff*4
        int base = apiKey.getRetryBackoffMs();
        return (long) (base * Math.pow(2, Math.max(0, retryNo - 1)));
    }

    @Override
    public int recordFailure(Long apiKeyId) {
        AtomicInteger counter = failureCounter.computeIfAbsent(apiKeyId, k -> new AtomicInteger(0));
        int failures = counter.incrementAndGet();
        log.warn("[FEATURE051] API Key {} 连续失败 {} 次", apiKeyId, failures);
        return failures;
    }

    @Override
    public void resetFailures(Long apiKeyId) {
        failureCounter.computeIfAbsent(apiKeyId, k -> new AtomicInteger(0)).set(0);
    }

    @Override
    public boolean isCircuitOpen(Long apiKeyId) {
        ApiKeyDO apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            return false;
        }

        Integer threshold = apiKey.getCircuitBreakerThreshold();
        if (threshold == null || threshold <= 0) {
            return false;
        }

        AtomicInteger counter = failureCounter.get(apiKeyId);
        return counter != null && counter.get() >= threshold;
    }

    @Override
    public void updatePolicy(Long apiKeyId, Integer maxRetry, Integer backoffMs, String retryOn, Integer breakerThreshold) {
        ApiKeyDO apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            log.warn("[FEATURE051] updatePolicy: API Key {} 不存在", apiKeyId);
            return;
        }
        apiKeyMapper.update(null, new LambdaUpdateWrapper<ApiKeyDO>()
                .set(ApiKeyDO::getMaxRetry, maxRetry)
                .set(ApiKeyDO::getRetryBackoffMs, backoffMs)
                .set(ApiKeyDO::getRetryOn, retryOn)
                .set(ApiKeyDO::getCircuitBreakerThreshold, breakerThreshold)
                .eq(ApiKeyDO::getId, apiKeyId));
        log.info("[FEATURE051] API Key {} 重试策略已更新: maxRetry={}, backoff={}ms, retryOn={}, breaker={}",
                apiKeyId, maxRetry, backoffMs, retryOn, breakerThreshold);
    }
}
