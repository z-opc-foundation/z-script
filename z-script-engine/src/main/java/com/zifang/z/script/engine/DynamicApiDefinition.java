package com.zifang.z.script.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态 API 定义 - 完全数据化的接口描述
 * <p>
 * 把 Java 接口里的注解（@RestController/@GetMapping/@PostMapping/@PathVariable/...）
 * 全部转化为 POJO 字段。这样调用方完全不需要写 Java 接口，所有信息可来自:
 * - DB 行 (z_mock_endpoint / z_mock_test_case / z_mock_recording_request)
 * - UI 表单
 * - Curl 字符串（经 CurlParser 转换）
 * - OpenAPI spec
 * - 录制回放
 * - 直接 Java 代码构造
 * <p>
 * 配合 DynamicApiExecutor 使用：所有入口最终都归一化到此对象，再统一执行。
 */
public class DynamicApiDefinition {

    /**
     * API 唯一编码（业务主键，如 api.user.get）
     */
    private String code;

    /**
     * 人类可读名称（"获取用户详情"）
     */
    private String name;

    /**
     * 基础地址（https://api.example.com）
     */
    private String host;

    /**
     * 完整 URL（如果已拼接 host+path+query，可直接用 url 跳过 host 拼接）
     */
    private String url;

    /**
     * HTTP 方法：GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS
     */
    private String method;

    /**
     * 路径模板：/users/{id}，配合 pathVars 做替换
     */
    private String path;

    /**
     * 路径变量：id -> 123
     */
    private Map<String, Object> pathVars = new LinkedHashMap<>();

    /**
     * Query 参数：page -> 1
     */
    private Map<String, Object> queryParams = new LinkedHashMap<>();

    /**
     * 请求头：Authorization -> Bearer xxx
     */
    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * 请求体（已序列化为 String，可以是 JSON/表单/XML）
     */
    private String body;

    /**
     * 请求体类型：JSON / FORM / XML / TEXT / BINARY
     */
    private String bodyType = "JSON";

    /**
     * Cookie：JSESSIONID -> abc123
     */
    private Map<String, String> cookies = new LinkedHashMap<>();

    /**
     * Basic Auth：user:password
     */
    private String basicAuth;

    /**
     * Bearer Token
     */
    private String bearerToken;

    /**
     * 超时（毫秒）
     */
    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 15000;
    private Integer writeTimeoutMs = 10000;

    /**
     * 期望返回类型（仅用于反序列化提示）
     */
    private String returnType;

    /**
     * 是否跟随重定向
     */
    private Boolean followRedirects = true;

    /**
     * 是否记录到日志（默认 true）
     */
    private Boolean recordToLog = true;

    /**
     * 录制 ID（可选）
     */
    private String recordingCode;

    /**
     * 来源标签（CURL / DB / UI / OPENAPI），执行时透传到 ApiExecutionResult
     */
    private String source;

    /**
     * 关联的 Mock endpoint code（执行时透传到 ApiExecutionResult）
     */
    private String mockCode;

    /**
     * 标签/分类
     */
    private String tags;

    /**
     * 备注
     */
    private String description;

    public DynamicApiDefinition() {
    }

    /**
     * 快速构造（最常用场景）
     */
    public static DynamicApiDefinition of(String method, String url) {
        DynamicApiDefinition d = new DynamicApiDefinition();
        d.setMethod(method);
        d.setUrl(url);
        return d;
    }

    public static DynamicApiDefinition of(String method, String host, String path) {
        DynamicApiDefinition d = new DynamicApiDefinition();
        d.setMethod(method);
        d.setHost(host);
        d.setPath(path);
        return d;
    }

    // ========== Fluent setters (builder style) ==========

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    public DynamicApiDefinition code(String v) {
        this.code = v;
        return this;
    }

    public DynamicApiDefinition name(String v) {
        this.name = v;
        return this;
    }

    public DynamicApiDefinition host(String v) {
        this.host = v;
        return this;
    }

    public DynamicApiDefinition url(String v) {
        this.url = v;
        return this;
    }

    public DynamicApiDefinition method(String v) {
        this.method = v;
        return this;
    }

    public DynamicApiDefinition path(String v) {
        this.path = v;
        return this;
    }

    public DynamicApiDefinition body(String v) {
        this.body = v;
        this.bodyType = v == null ? "JSON" : (v.trim().startsWith("{") || v.trim().startsWith("[") ? "JSON" : "TEXT");
        return this;
    }

    public DynamicApiDefinition bodyType(String v) {
        this.bodyType = v;
        return this;
    }

    public DynamicApiDefinition bearerToken(String v) {
        this.bearerToken = v;
        return this;
    }

    public DynamicApiDefinition basicAuth(String v) {
        this.basicAuth = v;
        return this;
    }

    public DynamicApiDefinition timeout(int connectMs, int readMs, int writeMs) {
        this.connectTimeoutMs = connectMs;
        this.readTimeoutMs = readMs;
        this.writeTimeoutMs = writeMs;
        return this;
    }

    public DynamicApiDefinition recordingCode(String v) {
        this.recordingCode = v;
        return this;
    }

    public DynamicApiDefinition addHeader(String k, String v) {
        this.headers.put(k, v);
        return this;
    }

    public DynamicApiDefinition addQuery(String k, Object v) {
        this.queryParams.put(k, v);
        return this;
    }

    public DynamicApiDefinition addPathVar(String k, Object v) {
        this.pathVars.put(k, v);
        return this;
    }

    public DynamicApiDefinition addCookie(String k, String v) {
        this.cookies.put(k, v);
        return this;
    }

    /**
     * 渲染最终 URL（host + path + pathVars + queryParams）
     */
    public String renderUrl() {
        if (url != null && !url.isEmpty()) {
            return appendQuery(url);
        }
        StringBuilder sb = new StringBuilder();
        if (host != null) {
            sb.append(host);
        }

        if (path != null) {
            if (!path.startsWith("/") && sb.length() > 0) sb.append("/");

            String p = path;
            // 替换 {var}
            if (pathVars != null) {
                for (Map.Entry<String, Object> e : pathVars.entrySet()) {
                    p = p.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
            }
            sb.append(p);
        }
        return appendQuery(sb.toString());
    }

    private String appendQuery(String base) {
        if (queryParams == null || queryParams.isEmpty()) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base);
        sb.append(base.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, Object> e : queryParams.entrySet()) {
            if (!first) {
                sb.append("&");
            }

            sb.append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue() == null ? "" : String.valueOf(e.getValue())));
            first = false;
        }
        return sb.toString();
    }

    /**
     * 把所有 header 合在一起（含 BearerToken / BasicAuth）
     */
    public Map<String, String> effectiveHeaders() {
        Map<String, String> all = new LinkedHashMap<>();
        if (headers != null) {
            all.putAll(headers);
        }

        if (bearerToken != null && !bearerToken.isEmpty()) {
            all.put("Authorization", "Bearer " + bearerToken);
        }
        if (basicAuth != null && !basicAuth.isEmpty() && !basicAuth.contains(" ")) {
            all.put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(basicAuth.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        return all;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String v) {
        this.code = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        this.name = v;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String v) {
        this.host = v;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String v) {
        this.url = v;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String v) {
        this.method = v;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String v) {
        this.path = v;
    }

    public Map<String, Object> getPathVars() {
        return pathVars;
    }

    public void setPathVars(Map<String, Object> v) {
        this.pathVars = v;
    }

    public Map<String, Object> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, Object> v) {
        this.queryParams = v;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> v) {
        this.headers = v;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String v) {
        this.body = v;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String v) {
        this.bodyType = v;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> v) {
        this.cookies = v;
    }

    public String getBasicAuth() {
        return basicAuth;
    }

    public void setBasicAuth(String v) {
        this.basicAuth = v;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String v) {
        this.bearerToken = v;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(Integer v) {
        this.connectTimeoutMs = v;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(Integer v) {
        this.readTimeoutMs = v;
    }

    public Integer getWriteTimeoutMs() {
        return writeTimeoutMs;
    }

    public void setWriteTimeoutMs(Integer v) {
        this.writeTimeoutMs = v;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String v) {
        this.returnType = v;
    }

    public Boolean getFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(Boolean v) {
        this.followRedirects = v;
    }

    public Boolean getRecordToLog() {
        return recordToLog;
    }

    public void setRecordToLog(Boolean v) {
        this.recordToLog = v;
    }

    public String getRecordingCode() {
        return recordingCode;
    }

    public void setRecordingCode(String v) {
        this.recordingCode = v;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String v) {
        this.source = v;
    }

    public String getMockCode() {
        return mockCode;
    }

    public void setMockCode(String v) {
        this.mockCode = v;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String v) {
        this.tags = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }
}
