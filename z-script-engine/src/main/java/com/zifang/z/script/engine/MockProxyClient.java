package com.zifang.z.script.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.core.domain.mapper.MockEnvironmentMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;

/**
 * 真实服务代理客户端 (用于 MIXED 环境/未匹配时转发)
 * <p>
 * 重构后: 内部委托给 DynamicApiExecutor（统一走 OkHttp），
 * 享受异步、SSE、文件上传、Bearer/Basic Auth 等能力。
 */
public class MockProxyClient {

    private static final Logger log = LogManager.getLogger(MockProxyClient.class);

    @Autowired
    private DynamicApiExecutor executor;
    /**
     * MockEndpointLoader 实现 - 桥到 z-script-core 的 mapper
     */
    @Autowired
    private MockEndpointMapper endpointMapper;
    @Autowired
    private MockEnvironmentMapper environmentMapper;

    @SuppressWarnings("unchecked")
    private static Map<String, String> parseJson(String s) {
        if (s == null || s.isEmpty()) {
            return new java.util.HashMap<>();
        }
        try {
            return JsonUtil.fromJson(s, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String s) {
        if (s == null || s.isEmpty()) {
            return new java.util.HashMap<>();
        }
        try {
            return JsonUtil.fromJson(s, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    /**
     * 旧签名 - 保留兼容 MockEngine 调用
     */
    public MockEngine.MockResponse proxy(String baseUrl, String method, String path, String query,
                                         Map<String, String> headers, String body, long start) throws IOException {
        StringBuilder url = new StringBuilder();
        if (baseUrl != null) {
            String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            url.append(b);
        }
        if (path != null && !path.startsWith("/")) {
            url.append("/");
        }

        if (path != null) {
            url.append(path);
        }

        if (query != null && !query.isEmpty()) {
            url.append("?").append(query);
        }


        DynamicApiDefinition def = DynamicApiDefinition.of(method, url.toString());
        if (headers != null) {
            def.getHeaders().putAll(headers);
        }

        if (body != null && !body.isEmpty()) {
            def.setBody(body);
        }

        def.setSource("PROXY");
        def.setConnectTimeoutMs(5000);
        def.setReadTimeoutMs(10_000);
        def.setWriteTimeoutMs(5_000);

        log.info("Proxying {} {} -> {}", method, path, url);
        ApiExecutionResult r = executor.execute(def);
        return MockEngine.MockResponse.proxied(r.getStatus(), r.getBody(), start);
    }

    /**
     * 新签名 - 拿 ApiExecutionResult，更通用
     */
    public ApiExecutionResult proxyAdvanced(String baseUrl, String method, String path, String query,
                                            Map<String, String> headers, String body) {
        StringBuilder url = new StringBuilder();
        if (baseUrl != null) {
            String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            url.append(b);
        }
        if (path != null && !path.startsWith("/")) {
            url.append("/");
        }

        if (path != null) {
            url.append(path);
        }

        if (query != null && !query.isEmpty()) {
            url.append("?").append(query);
        }


        DynamicApiDefinition def = DynamicApiDefinition.of(method, url.toString());
        if (headers != null) {
            def.getHeaders().putAll(headers);
        }

        if (body != null && !body.isEmpty()) {
            def.setBody(body);
        }

        def.setSource("PROXY");
        def.setConnectTimeoutMs(5000);
        def.setReadTimeoutMs(10_000);
        def.setWriteTimeoutMs(5_000);

        return executor.execute(def);
    }

    /**
     * 通过 mockCode 调真实服务（先拉 endpoint 配置，再代理）
     */
    public ApiExecutionResult proxyByCode(String mockCode) {
        executor.setEndpointLoader(this::loadAsDefinition);
        return executor.executeByCode(mockCode);
    }

    public DynamicApiDefinition loadAsDefinition(String mockCode) {
        MockEndpoint ep = endpointMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockEndpoint>()
                        .eq("mock_code", mockCode));
        if (ep == null) {
            return null;
        }


        DynamicApiDefinition d = new DynamicApiDefinition();
        d.setCode(ep.getMockCode());
        d.setName(ep.getMockName());
        d.setMethod(ep.getMethod() == null ? "GET" : ep.getMethod());
        d.setPath(ep.getPath());
        // 取环境 baseUrl
        if (ep.getEnvCode() != null) {
            try {
                com.zifang.z.script.core.domain.entity.MockEnvironment env = environmentMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.zifang.z.script.core.domain.entity.MockEnvironment>()
                                .eq("env_code", ep.getEnvCode()));
                if (env != null && env.getBaseUrl() != null) d.setHost(env.getBaseUrl());
            } catch (Exception ignored) {
            }
        }
        if (ep.getMatchHeaders() != null) d.getHeaders().putAll(parseJson(ep.getMatchHeaders()));

        if (ep.getMatchQuery() != null) d.getQueryParams().putAll(parseJsonObject(ep.getMatchQuery()));

        if (ep.getMatchBody() != null) d.setBody(ep.getMatchBody());

        d.setMockCode(ep.getMockCode());
        d.setSource("DB");
        return d;
    }
}
