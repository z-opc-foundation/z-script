package com.zifang.z.script.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock 故障注入引擎
 * <p>
 * 支持的故障类型:
 * - NONE: 不注入
 * - TIMEOUT: 模拟超时 (不返回响应)
 * - EMPTY_RESPONSE: 返回空响应
 * - RANDOM_DROP: 随机返回错误 (配置: errorRate, errorStatus)
 * - MALFORMED: 返回畸形响应 (配置: malformedType: chunked|invalid|partial)
 * - CHUNKED: 分块响应 (模拟慢速流式)
 * - 500_ERROR: 直接返回 500
 * - RATE_LIMIT: 限流 (配置: qps, exceededStatus)
 */
public class FaultInjectionEngine {

    private static final Logger log = LogManager.getLogger(FaultInjectionEngine.class);
    private static final Random RANDOM = new Random();

    // Rate limiter storage: endpointCode -> (window start ms, counter)
    private static final Map<String, AtomicLong> RATE_COUNTERS = new ConcurrentHashMap<>();
    private static final Map<String, Long> RATE_WINDOW = new ConcurrentHashMap<>();

    public FaultDecision decide(MockEndpoint endpoint) {
        if (endpoint == null || endpoint.getFaultType() == null
                || "NONE".equalsIgnoreCase(endpoint.getFaultType())) {
            return FaultDecision.proceed();
        }

        String type = endpoint.getFaultType();
        Map<String, Object> config = parseConfig(endpoint.getFaultConfig());

        switch (type.toUpperCase()) {
            case "TIMEOUT":
                log.info("Fault: TIMEOUT for endpoint {}", endpoint.getMockCode());
                return FaultDecision.timeout(config);

            case "EMPTY_RESPONSE":
                return FaultDecision.empty();

            case "MALFORMED":
                String malType = (String) config.getOrDefault("malformedType", "invalid");
                return FaultDecision.malformed(malType);

            case "500_ERROR":
                return FaultDecision.error(500, "Internal Server Error (fault injected)");

            case "RANDOM_DROP": {
                double rate = toDouble(config.get("errorRate"), 0.5);
                int errStatus = toInt(config.get("errorStatus"), 500);
                if (RANDOM.nextDouble() < rate) {
                    return FaultDecision.error(errStatus, "Random error (rate=" + rate + ")");
                }
                return FaultDecision.proceed();
            }

            case "RATE_LIMIT": {
                int qps = toInt(config.get("qps"), 10);
                int exceededStatus = toInt(config.get("exceededStatus"), 429);
                if (!checkRateLimit(endpoint.getMockCode(), qps)) {
                    return FaultDecision.error(exceededStatus, "Rate limit exceeded (qps=" + qps + ")");
                }
                return FaultDecision.proceed();
            }

            case "CHUNKED":
                return FaultDecision.chunked(config);

            default:
                log.warn("Unknown fault type: {}", type);
                return FaultDecision.proceed();
        }
    }

    /**
     * 滑动窗口限流
     */
    private boolean checkRateLimit(String key, int qps) {
        long now = System.currentTimeMillis();
        long windowStart = RATE_WINDOW.getOrDefault(key, 0L);
        if (now - windowStart >= 1000) {
            RATE_WINDOW.put(key, now);
            RATE_COUNTERS.put(key, new AtomicLong(0));
        }
        AtomicLong counter = RATE_COUNTERS.get(key);
        if (counter == null) {
            counter = new AtomicLong(0);
            RATE_COUNTERS.put(key, counter);
        }
        return counter.incrementAndGet() <= qps;
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private double toDouble(Object o, double def) {
        if (o == null) {
            return def;
        }

        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return def;
        }
    }

    private int toInt(Object o, int def) {
        if (o == null) {
            return def;
        }

        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * 故障决策结果
     */
    public static class FaultDecision {
        private final Action action;
        private final int statusCode;
        private final String body;
        private final Map<String, Object> config;

        private FaultDecision(Action action, int statusCode, String body, Map<String, Object> config) {
            this.action = action;
            this.statusCode = statusCode;
            this.body = body;
            this.config = config == null ? new HashMap<>() : config;
        }

        public static FaultDecision proceed() {
            return new FaultDecision(Action.PROCEED, 200, null, null);
        }

        public static FaultDecision timeout(Map<String, Object> cfg) {
            return new FaultDecision(Action.TIMEOUT, 504, null, cfg);
        }

        public static FaultDecision empty() {
            return new FaultDecision(Action.EMPTY, 204, null, null);
        }

        public static FaultDecision error(int status, String body) {
            return new FaultDecision(Action.ERROR, status, body, null);
        }

        public static FaultDecision malformed(String type) {
            Map<String, Object> cfg = new HashMap<>();
            cfg.put("type", type);
            return new FaultDecision(Action.MALFORMED, 200, null, cfg);
        }

        public static FaultDecision chunked(Map<String, Object> cfg) {
            return new FaultDecision(Action.CHUNKED, 200, null, cfg);
        }

        public Action getAction() {
            return action;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public boolean shouldInject() {
            return action != Action.PROCEED;
        }

        public enum Action {PROCEED, TIMEOUT, EMPTY, ERROR, MALFORMED, CHUNKED}
    }
}
