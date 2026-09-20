package com.zifang.z.script.engine;

import com.zifang.util.http.base.pojo.HttpRequestBody;
import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.http.base.pojo.HttpRequestHeader;
import com.zifang.util.http.base.pojo.HttpRequestLine;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 动态 API 执行器 - z-script 对 z-util-http HttpExecutor 的薄适配
 * <p>
 * 职责:
 * 1. 把 z-script 业务领域的 DynamicApiDefinition 转成 z-util-http 标准的 HttpRequestDefinition
 * 2. 委托给 z-util-http 的 HttpExecutor（拿到 OkHttp + SSE + 异步 + 上传全套能力）
 * 3. 把 z-util-http 的 HttpExecutionResult 包成 z-script 业务领域的 ApiExecutionResult
 * （附加 z-script 特有字段：mockCode / recordingCode / source）
 * <p>
 * 真正的 HTTP 执行逻辑（OkHttp 连接池、sse 监听、multipart 上传等）都在 z-util-http 那边，
 * 这里不重复实现。
 */
@Component
public class DynamicApiExecutor {

    private static final Logger log = LogManager.getLogger(DynamicApiExecutor.class);

    /**
     * 委托给 z-util-http 的标准执行器（共享单例，避免重复创建 OkHttpClient）
     */
    private final HttpExecutor httpExecutor = HttpExecutor.getDefault();

    /**
     * 桥接器 - 由 MockProxyClient 注入实现，避免 engine 反向依赖 core 的 mapper
     */
    private MockEndpointLoader endpointLoader;

    public void setEndpointLoader(MockEndpointLoader loader) {
        this.endpointLoader = loader;
    }

    // =================================================================
    // 5 个入口（每个都委托给 HttpExecutor）
    // =================================================================

    /**
     * 入口1: 直接拿 DynamicApiDefinition 跑
     */
    public ApiExecutionResult execute(DynamicApiDefinition def) {
        HttpRequestDefinition std = toStandardDefinition(def);
        HttpExecutionResult raw = httpExecutor.execute(std);
        return toApiExecutionResult(raw, def);
    }

    /**
     * 入口2: 用 curl 字符串跑（透传到底层 HttpExecutor.executeByCurl）
     */
    public ApiExecutionResult executeByCurl(String curlCommand) {
        HttpExecutionResult raw = httpExecutor.executeByCurl(curlCommand);
        return ApiExecutionResult.from(raw);
    }

    /**
     * 入口3: 拿 mockCode 从 DB 拉 Mock endpoint 跑（注入依赖）
     */
    public ApiExecutionResult executeByCode(String mockCode) {
        if (endpointLoader == null) {
            return ApiExecutionResult.fail("EndpointLoader not configured", "CONFIG_ERROR");
        }
        DynamicApiDefinition def = endpointLoader.loadAsDefinition(mockCode);
        if (def == null) {
            return ApiExecutionResult.fail("Mock endpoint not found: " + mockCode, "NOT_FOUND");
        }
        return execute(def);
    }

    /**
     * 入口4: 用 method+url 直接跑（透传到底层）
     */
    public ApiExecutionResult executeByMethodUrl(String method, String url,
                                                 Map<String, String> headers, String body) {
        HttpExecutionResult raw = httpExecutor.executeByMethodUrl(method, url, headers, body);
        return ApiExecutionResult.from(raw);
    }

    /**
     * 入口5: 从 OpenAPI spec 跑（暂未实现，留作后续 P2）
     */
    public ApiExecutionResult executeByOpenApi(String openApiSpec, String operationId, Map<String, Object> params) {
        return ApiExecutionResult.fail("OpenAPI executor not implemented yet, see P2 task", "NOT_IMPLEMENTED");
    }

    // =================================================================
    // 4 种执行模式（透传）
    // =================================================================

    public ApiExecutionResult send(DynamicApiDefinition def) {
        return execute(def);
    }

    public java.util.concurrent.CompletableFuture<ApiExecutionResult> sendAsync(DynamicApiDefinition def) {
        HttpRequestDefinition std = toStandardDefinition(def);
        return httpExecutor.sendAsync(std).thenApply(raw -> toApiExecutionResult(raw, def));
    }

    public void sendSse(DynamicApiDefinition def, java.util.function.Consumer<ApiExecutionResult> onEvent) {
        HttpRequestDefinition std = toStandardDefinition(def);
        httpExecutor.sendSse(std, raw -> onEvent.accept(ApiExecutionResult.from(raw)));
    }

    public ApiExecutionResult sendUpload(DynamicApiDefinition def, String fileField, java.io.File file) {
        HttpRequestDefinition std = toStandardDefinition(def);
        HttpExecutionResult raw = httpExecutor.sendUpload(std, fileField, file);
        return ApiExecutionResult.from(raw);
    }

    // =================================================================
    // 转换工具
    // =================================================================

    /**
     * DynamicApiDefinition → HttpRequestDefinition
     */
    public HttpRequestDefinition toStandardDefinition(DynamicApiDefinition d) {
        HttpRequestDefinition def = new HttpRequestDefinition();

        HttpRequestLine rl = new HttpRequestLine();
        String method = d.getMethod() == null ? "GET" : d.getMethod().toUpperCase();
        try {
            rl.setRequestMethod(com.zifang.util.http.base.define.RequestMethod.valueOf(method));
        } catch (Exception e) {
            rl.setRequestMethod(com.zifang.util.http.base.define.RequestMethod.GET);
        }
        rl.setUrl(d.renderUrl());
        def.setHttpRequestLine(rl);

        if (d.effectiveHeaders() != null && !d.effectiveHeaders().isEmpty()) {
            HttpRequestHeader hh = new HttpRequestHeader();
            for (Map.Entry<String, String> e : d.effectiveHeaders().entrySet()) {
                hh.put(e.getKey(), e.getValue());
            }
            // Cookies 拼成 Cookie 头
            if (d.getCookies() != null && !d.getCookies().isEmpty()) {
                StringBuilder cookie = new StringBuilder();
                for (Map.Entry<String, String> e : d.getCookies().entrySet()) {
                    if (cookie.length() > 0) cookie.append("; ");
                    cookie.append(e.getKey()).append("=").append(e.getValue());
                }
                hh.put("Cookie", cookie.toString());
            }
            def.setHttpRequestHeader(hh);
        }
        if (d.getBody() != null) {
            HttpRequestBody b = new HttpRequestBody();
            b.setBody(d.getBody().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            def.setHttpRequestBody(b);
        }
        return def;
    }

    /**
     * HttpRequestDefinition → DynamicApiDefinition（反方向，方便用户复用 CurlParser）
     */
    public DynamicApiDefinition fromStandardDefinition(HttpRequestDefinition src) {
        DynamicApiDefinition d = new DynamicApiDefinition();
        if (src.getHttpRequestLine() != null) {
            d.setMethod(src.getHttpRequestLine().getRequestMethod() == null
                    ? "GET" : src.getHttpRequestLine().getRequestMethod().name());
            d.setUrl(src.getHttpRequestLine().getUrl());
        }
        if (src.getHttpRequestHeader() != null) {
            for (Map.Entry<String, String> e : src.getHttpRequestHeader().entrySet()) {
                d.addHeader(e.getKey(), e.getValue());
            }
        }
        if (src.getHttpRequestBody() != null && src.getHttpRequestBody().getBody() != null) {
            d.setBody(new String(src.getHttpRequestBody().getBody(), java.nio.charset.StandardCharsets.UTF_8));
        }
        return d;
    }

    /**
     * 把 z-util-http 的结果包成 z-script 业务结果（附加 mockCode / recordingCode）
     */
    private ApiExecutionResult toApiExecutionResult(HttpExecutionResult raw, DynamicApiDefinition def) {
        ApiExecutionResult r = ApiExecutionResult.from(raw);
        if (def.getMockCode() != null) r.setMockCode(def.getMockCode());

        if (def.getRecordingCode() != null) r.setRecordingCode(def.getRecordingCode());

        if (def.getSource() != null && r.getSource() == null) r.setSource(def.getSource());

        return r;
    }

    // =================================================================
    // 桥接器接口（由 MockProxyClient / PlaybackEngine 提供实现）
    // =================================================================

    @FunctionalInterface
    public interface MockEndpointLoader {
        DynamicApiDefinition loadAsDefinition(String mockCode);
    }
}
