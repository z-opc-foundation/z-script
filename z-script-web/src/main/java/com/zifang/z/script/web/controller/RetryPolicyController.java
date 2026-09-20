package com.zifang.z.script.web.controller;

import com.zifang.z.script.core.domain.service.impl.RetryPolicyServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * FEATURE051 - t17 重试策略 Controller
 * <p>
 * 提供:
 * - POST /retry/policy/{apiKeyId}      更新策略
 * - GET  /retry/policy/{apiKeyId}      查询策略
 * - GET  /retry/circuit/{apiKeyId}     熔断器状态
 * - POST /retry/circuit/{apiKeyId}/reset 重置熔断器
 */
@RestController
@RequestMapping("/api/script/retry")
public class RetryPolicyController {

    @Autowired
    private RetryPolicyServiceImpl retryPolicyService;

    @PostMapping("/policy/{apiKeyId}")
    public Map<String, Object> updatePolicy(@PathVariable Long apiKeyId,
                                            @RequestParam(required = false, defaultValue = "0") Integer maxRetry,
                                            @RequestParam(required = false, defaultValue = "1000") Integer backoffMs,
                                            @RequestParam(required = false, defaultValue = "HTTP_5XX") String retryOn,
                                            @RequestParam(required = false, defaultValue = "0") Integer breakerThreshold) {
        retryPolicyService.updatePolicy(apiKeyId, maxRetry, backoffMs, retryOn, breakerThreshold);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "重试策略已更新");
        result.put("maxRetry", maxRetry);
        result.put("backoffMs", backoffMs);
        result.put("retryOn", retryOn);
        result.put("breakerThreshold", breakerThreshold);
        return result;
    }

    @GetMapping("/policy/{apiKeyId}")
    public Map<String, Object> getPolicy(@PathVariable Long apiKeyId) {
        // 这里从 ApiKeyService 取 maxRetry 等 (简化返回)
        Map<String, Object> result = new HashMap<>();
        result.put("apiKeyId", apiKeyId);
        result.put("circuitOpen", retryPolicyService.isCircuitOpen(apiKeyId));
        return result;
    }

    @GetMapping("/circuit/{apiKeyId}")
    public Map<String, Object> circuitStatus(@PathVariable Long apiKeyId) {
        Map<String, Object> result = new HashMap<>();
        result.put("apiKeyId", apiKeyId);
        result.put("circuitOpen", retryPolicyService.isCircuitOpen(apiKeyId));
        return result;
    }

    @PostMapping("/circuit/{apiKeyId}/reset")
    public Map<String, Object> resetCircuit(@PathVariable Long apiKeyId) {
        retryPolicyService.resetFailures(apiKeyId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "熔断器已重置");
        return result;
    }
}
