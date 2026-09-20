package com.zifang.z.script.core.domain.service;

import com.zifang.z.script.core.domain.entity.ApiKeyDO;

/**
 * FEATURE051 - t17 失败重试策略
 * <p>
 * 提供:
 * 1. 客户端 HTTP 重试包装 (前端 fetch 自动重试)
 * 2. 服务端自动重试 (Interceptor 检测失败后异步重试)
 * 3. 熔断器 (连续失败 N 次后临时禁用 Key)
 */
public interface RetryPolicyService {

    /**
     * 判定本次调用是否应该触发重试
     *
     * @param apiKey       API Key 实体
     * @param httpStatus   本次调用 HTTP 状态码
     * @param errorMessage 错误信息 (用于判断 TIMEOUT)
     * @return true=应该重试
     */
    boolean shouldRetry(ApiKeyDO apiKey, int httpStatus, String errorMessage);

    /**
     * 计算下次重试前的等待时间 (毫秒)
     * <p>
     * 固定间隔: backoff_ms
     * 指数退避 (TODO): backoff_ms * 2^retryNo
     */
    long computeBackoffMs(ApiKeyDO apiKey, int retryNo);

    /**
     * 熔断器: 累计连续失败次数
     */
    int recordFailure(Long apiKeyId);

    /**
     * 熔断器: 重置连续失败计数 (调用成功时)
     */
    void resetFailures(Long apiKeyId);

    /**
     * 熔断器: 是否已经熔断 (>= threshold)
     */
    boolean isCircuitOpen(Long apiKeyId);

    /**
     * 更新 API Key 的重试配置
     */
    void updatePolicy(Long apiKeyId, Integer maxRetry, Integer backoffMs, String retryOn, Integer breakerThreshold);
}
