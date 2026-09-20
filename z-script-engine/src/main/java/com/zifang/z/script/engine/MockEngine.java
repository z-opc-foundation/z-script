package com.zifang.z.script.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.entity.MockEnvironment;
import com.zifang.z.script.core.domain.entity.MockRequestLog;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.core.domain.mapper.MockEnvironmentMapper;
import com.zifang.z.script.core.domain.mapper.MockRequestLogMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统一 Mock 引擎 - 整合匹配/场景/故障/响应渲染
 * <p>
 * 请求处理流程:
 * 1. 解析请求 (method/path/query/headers/body)
 * 2. 加载当前环境的 mock endpoints
 * 3. 匹配器找到最佳 endpoint
 * 4. 检查场景状态约束
 * 5. 故障注入判定
 * 6. 模板渲染响应
 * 7. 状态转换
 * 8. 记录命中
 */
@Component
public class MockEngine {

    private static final Logger log = LogManager.getLogger(MockEngine.class);

    @Autowired
    private MockEndpointMapper endpointMapper;
    @Autowired
    private MockEnvironmentMapper environmentMapper;
    @Autowired
    private RequestMatcherEngine matcher;
    @Autowired
    private ScenarioStateMachine scenarioEngine;
    @Autowired
    private FaultInjectionEngine faultEngine;
    @Autowired
    private MockTemplateEngine templateEngine;
    @Autowired
    private MockProxyClient proxyClient;
    @Autowired
    private RecorderEngine recorder;
    @Autowired
    private MockRequestLogMapper requestLogMapper;

    /**
     * 处理 Mock 请求
     */
    public MockResponse handleRequest(String envCode, HttpServletRequest request) {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        Map<String, String> headers = RequestMatcherEngine.extractHeaders(request);
        String body = RequestMatcherEngine.extractBody(request);

        log.debug("Mock request: {} {} (env={})", method, path, envCode);

        // 1. Get environment
        MockEnvironment env = resolveEnv(envCode);

        // 2. Load candidates
        List<MockEndpoint> candidates = endpointMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockEndpoint>()
                        .eq("status", 1)
                        .and(c -> c.eq("env_code", envCode).or().eq("env_code", "default"))
        );

        // 3. Find best match
        RequestMatcherEngine.MatchResult match = matcher.findMatch(candidates, method, path, query, headers, body);

        if (!match.isMatched()) {
            return handleNoMatch(env, method, path, query, headers, body, start);
        }

        MockEndpoint endpoint = match.getEndpoint();

        // 4. Check scenario state constraint
        String instanceKey = extractInstanceKey(headers, request);
        if (!scenarioEngine.checkStateConstraint(endpoint, instanceKey)) {
            return MockResponse.error(409, "Scenario state mismatch: required="
                    + endpoint.getRequiredState() + ", current="
                    + scenarioEngine.getCurrentState(endpoint.getScenarioCode(), instanceKey), start);
        }

        // 5. Fault injection check
        FaultInjectionEngine.FaultDecision fault = faultEngine.decide(endpoint);
        if (fault.shouldInject()) {
            return applyFault(fault, start);
        }

        // 6. Apply delay
        if (endpoint.getDelayMs() != null && endpoint.getDelayMs() > 0) {
            try {
                Thread.sleep(endpoint.getDelayMs());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        // 7. Render response
        Map<String, Object> renderCtx = new java.util.HashMap<>();
        renderCtx.put("path", match.getPathVariables());
        renderCtx.put("query", parseQuery(query));
        renderCtx.put("headers", headers);
        renderCtx.put("body", body);
        Object rendered = templateEngine.render(endpoint.getResponseTemplate(), renderCtx);
        String responseBody = rendered instanceof String ? (String) rendered : JsonUtil.toJson(rendered);

        // 8. Transition state
        scenarioEngine.transitionState(endpoint, instanceKey);

        // 9. Increment hit count
        incrementHit(endpoint);

        // 10. Recording (active session with matching env)
        captureIfRecording(envCode, endpoint, method, path, query, headers, body,
                endpoint.getStatusCode() == null ? 200 : endpoint.getStatusCode(),
                responseBody, start);

        log.info("Mock matched: {} -> {} ({} ms)", path, endpoint.getMockCode(),
                System.currentTimeMillis() - start);
        MockResponse resp = MockResponse.success(endpoint.getStatusCode() == null ? 200 : endpoint.getStatusCode(),
                responseBody, endpoint.getResponseHeaders(), start);
        resp.setMockCode(endpoint.getMockCode());
        logRequest(envCode, method, path, query, headers, body, resp, start, "MOCK", endpoint.getMockCode(),
                scenarioEngine.getCurrentState(endpoint.getScenarioCode(), instanceKey));
        return resp;
    }

    private MockEnvironment resolveEnv(String envCode) {
        if (envCode == null || envCode.isEmpty()) {
            envCode = "default";
        }
        // 1. Try exact
        MockEnvironment env = environmentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockEnvironment>()
                        .eq("env_code", envCode));
        if (env != null) {
            return env;
        }

        // 2. Default
        return environmentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockEnvironment>()
                        .eq("is_default", 1));
    }

    private MockResponse handleNoMatch(MockEnvironment env, String method, String path, String query,
                                       Map<String, String> headers, String body, long start) {
        if (env == null || "MOCK".equalsIgnoreCase(env.getEnvType())) {
            captureIfRecording(env == null ? "default" : env.getEnvCode(), null, method, path,
                    query, headers, body, 404, "No mock endpoint matched: " + method + " " + path, start);
            MockResponse mr = MockResponse.error(404, "No mock endpoint matched: " + method + " " + path, start);
            logRequest(env == null ? "default" : env.getEnvCode(), method, path, query, headers, body, mr, start, "404", null, null);
            return mr;
        }
        if (env.getBaseUrl() != null && !env.getBaseUrl().isEmpty()) {
            // Proxy to real
            try {
                MockResponse proxied = proxyClient.proxy(env.getBaseUrl(), method, path, query, headers, body, start);
                captureIfRecording(env.getEnvCode(), null, method, path, query, headers, body,
                        proxied.getStatusCode(), proxied.getBody(), start);
                proxied.setMockCode("PROXY:" + env.getBaseUrl());
                logRequest(env.getEnvCode(), method, path, query, headers, body, proxied, start, "PROXY", null, null);
                return proxied;
            } catch (Exception e) {
                log.warn("Proxy failed, falling back to 502: {}", e.getMessage());
                MockResponse mr = MockResponse.error(502, "Proxy failed: " + e.getMessage(), start);
                logRequest(env.getEnvCode(), method, path, query, headers, body, mr, start, "502", null, null);
                return mr;
            }
        }
        MockResponse mr = MockResponse.error(404, "No mock matched and no real service configured", start);
        logRequest(env.getEnvCode(), method, path, query, headers, body, mr, start, "404", null, null);
        return mr;
    }

    /**
     * 记录请求日志（异步、不影响主流程）
     */
    private void logRequest(String envCode, String method, String path, String query,
                            Map<String, String> headers, String body,
                            MockResponse resp, long start, String fallback, String mockCode, String scenarioState) {
        try {
            MockRequestLog log = new MockRequestLog();
            log.setEnvCode(envCode);
            log.setRequestMethod(method);
            log.setRequestPath(path + (query == null || query.isEmpty() ? "" : "?" + query));
            log.setRequestHeaders(headers == null ? null : JsonUtil.toJson(headers));
            log.setRequestBody(body);
            log.setResponseStatus(resp.getStatusCode());
            log.setResponseBody(resp.getBody());
            log.setMatched(mockCode != null ? 1 : 0);
            log.setFallbackType(fallback);
            log.setMockCode(mockCode);
            log.setDurationMs(System.currentTimeMillis() - start);
            log.setScenarioState(scenarioState);
            log.setCreateTime(new Date());
            requestLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志不应影响主流程
        }
    }

    /**
     * 如果当前环境处于录制状态，把请求/响应快照写入录制会话
     */
    private void captureIfRecording(String envCode, MockEndpoint endpoint, String method,
                                    String path, String query, Map<String, String> headers,
                                    String body, int statusCode, String responseBody, long start) {
        try {
            String recordingCode = envCode + "-recording";
            if (recorder == null) {
                return;
            }
            recorder.captureRequest(recordingCode, method, path + (query == null || query.isEmpty() ? "" : "?" + query),
                    headers, body, statusCode, null, responseBody,
                    System.currentTimeMillis() - start,
                    endpoint == null ? null : endpoint.getMockCode());
        } catch (Exception ignored) {
            // 录制不应影响主流程
        }
    }

    private MockResponse applyFault(FaultInjectionEngine.FaultDecision fault, long start) {
        switch (fault.getAction()) {
            case TIMEOUT:
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ignored) {
                }
                return MockResponse.error(504, "Timeout (fault injected)", start);
            case EMPTY:
                return MockResponse.empty(204, start);
            case MALFORMED:
                return MockResponse.malformed(fault.getConfig().get("type").toString(), start);
            case ERROR:
            default:
                return MockResponse.error(fault.getStatusCode(), fault.getBody(), start);
        }
    }

    private void incrementHit(MockEndpoint endpoint) {
        try {
            MockEndpoint update = new MockEndpoint();
            update.setId(endpoint.getId());
            update.setHitCount((endpoint.getHitCount() == null ? 0 : endpoint.getHitCount()) + 1);
            update.setLastHitTime(new Date());
            endpointMapper.updateById(update);
        } catch (Exception e) {
            log.warn("Failed to increment hit count: {}", e.getMessage());
        }
    }

    private String extractInstanceKey(Map<String, String> headers, HttpServletRequest request) {
        // Use X-Session-Id, or X-Trace-Id, or fall back to a header-derived key
        String key = headers.get("X-Session-Id");
        if (key != null && !key.isEmpty()) {
            return key;
        }
        key = headers.get("X-Trace-Id");
        if (key != null && !key.isEmpty()) {
            return key;
        }
        return request.getRemoteAddr() + ":" + headers.getOrDefault("User-Agent", "");
    }

    private Map<String, String> parseQuery(String qs) {
        Map<String, String> result = new java.util.HashMap<>();
        if (qs == null || qs.isEmpty()) {
            return result;
        }
        for (String pair : qs.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                result.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else result.put(pair, "");
        }
        return result;
    }

    /**
     * Mock 响应
     */
    public static class MockResponse {
        private Kind kind;
        private int statusCode;
        private String body;
        private String responseHeaders;
        private long durationMs;
        private String mockCode;
        private boolean matched;

        public static MockResponse success(int status, String body, String headers, long start) {
            MockResponse r = new MockResponse();
            r.kind = Kind.SUCCESS;
            r.statusCode = status;
            r.body = body;
            r.responseHeaders = headers;
            r.durationMs = System.currentTimeMillis() - start;
            r.matched = true;
            return r;
        }

        public static MockResponse error(int status, String body, long start) {
            MockResponse r = new MockResponse();
            r.kind = Kind.ERROR;
            r.statusCode = status;
            r.body = body;
            r.durationMs = System.currentTimeMillis() - start;
            r.matched = false;
            return r;
        }

        public static MockResponse empty(int status, long start) {
            MockResponse r = new MockResponse();
            r.kind = Kind.EMPTY;
            r.statusCode = status;
            r.durationMs = System.currentTimeMillis() - start;
            return r;
        }

        public static MockResponse malformed(String type, long start) {
            MockResponse r = new MockResponse();
            r.kind = Kind.MALFORMED;
            r.statusCode = 200;
            r.body = type.equals("chunked")
                    ? "5\r\nXXXXX\r\n0\r\n\r\n"
                    : type.equals("invalid")
                    ? "{this is not valid json"
                    : "{\"partial\":";
            r.durationMs = System.currentTimeMillis() - start;
            return r;
        }

        public static MockResponse proxied(int status, String body, long start) {
            MockResponse r = new MockResponse();
            r.kind = Kind.PROXIED;
            r.statusCode = status;
            r.body = body;
            r.durationMs = System.currentTimeMillis() - start;
            return r;
        }

        public Kind getKind() {
            return kind;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public String getResponseHeaders() {
            return responseHeaders;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public String getMockCode() {
            return mockCode;
        }

        public void setMockCode(String v) {
            this.mockCode = v;
        }

        public boolean isMatched() {
            return matched;
        }

        public enum Kind {SUCCESS, ERROR, EMPTY, MALFORMED, PROXIED}
    }
}
