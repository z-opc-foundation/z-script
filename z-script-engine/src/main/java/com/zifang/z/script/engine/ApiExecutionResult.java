package com.zifang.z.script.engine;

import com.zifang.util.http.client.HttpExecutionResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 HTTP 执行结果 - 任何 HTTP 调用最终都返回这个
 * <p>
 * 不管是:
 * - 同步执行
 * - 异步执行
 * - SSE 流式
 * - 文件上传
 * - 代理到真实服务
 * - 直接 curl 解析执行
 * 都会归一化到此结果，调用方可以无差别处理。
 * <p>
 * 与 ExecutionResult 的区别:
 * - ExecutionResult: 脚本执行结果（data=脚本输出对象）
 * - ApiExecutionResult: HTTP 执行结果（status/headers/body）
 */
public class ApiExecutionResult {

    /**
     * 成功/失败标记（业务层判断，比如 HTTP 200 也算 success=true）
     */
    private boolean success;

    /**
     * HTTP 状态码
     */
    private int status;

    /**
     * 响应头
     */
    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * 响应体（String）
     */
    private String body;

    /**
     * 响应体反序列化（如果 returnType 指定了）
     */
    private Object bodyObject;

    /**
     * 耗时（毫秒）
     */
    private long durationMs;

    /**
     * 错误信息（success=false 时填充）
     */
    private String error;

    /**
     * 错误类型（如 ConnectException / SocketTimeoutException / JsonSyntaxException）
     */
    private String errorType;

    /**
     * 原始异常
     */
    private Throwable exception;

    /**
     * 来源（PROXY / MOCK / CURL / TESTCASE / RECORDING / SSE）
     */
    private String source;

    /**
     * 关联的 Mock endpoint code（如果来源是 MOCK）
     */
    private String mockCode;

    /**
     * 录制 code（如果是边跑边录）
     */
    private String recordingCode;

    /**
     * 重定向历史（OkHttp 可追踪）
     */
    private java.util.List<String> redirectChain;

    /**
     * 响应体大小（字节）
     */
    private long bodySize;

    /**
     * 额外上下文（业务可放任何东西）
     */
    private Map<String, Object> context = new LinkedHashMap<>();

    public static ApiExecutionResult ok(int status, Map<String, String> headers, String body, long durationMs) {
        ApiExecutionResult r = new ApiExecutionResult();
        r.success = status >= 200 && status < 400;
        r.status = status;
        r.headers = headers == null ? new LinkedHashMap<>() : headers;
        r.body = body;
        r.durationMs = durationMs;
        r.bodySize = body == null ? 0 : body.getBytes().length;
        return r;
    }

    public static ApiExecutionResult fail(String error, Throwable ex) {
        ApiExecutionResult r = new ApiExecutionResult();
        r.success = false;
        r.error = error;
        r.exception = ex;
        r.errorType = ex == null ? null : ex.getClass().getName();
        return r;
    }

    public static ApiExecutionResult fail(String error, String errorType) {
        ApiExecutionResult r = new ApiExecutionResult();
        r.success = false;
        r.error = error;
        r.errorType = errorType;
        return r;
    }

    /**
     * SSE 专用 - 把一行事件包成 result
     */
    public static ApiExecutionResult sseEvent(String event, String data) {
        ApiExecutionResult r = new ApiExecutionResult();
        r.success = true;
        r.status = 200;
        r.context.put("event", event);
        r.body = data;
        return r;
    }

    /**
     * 从 z-util-http 的标准结果转过来 - 复制通用字段，mockCode/recordingCode 由调用方补上
     */
    public static ApiExecutionResult from(HttpExecutionResult raw) {
        if (raw == null) {
            return fail("Underlying HttpExecutionResult is null", "NULL_RESULT");
        }
        ApiExecutionResult r = new ApiExecutionResult();
        r.success = raw.isSuccess();
        r.status = raw.getStatus();
        r.headers = raw.getHeaders() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(raw.getHeaders());
        r.body = raw.getBody();
        r.bodySize = raw.getBodySize();
        r.durationMs = raw.getDurationMs();
        r.error = raw.getError();
        r.errorType = raw.getErrorType();
        r.exception = raw.getException();
        r.source = raw.getSource();
        r.redirectChain = raw.getRedirectChain();
        r.context = raw.getContext() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(raw.getContext());
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean v) {
        this.success = v;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int v) {
        this.status = v;
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
        this.bodySize = v == null ? 0 : v.getBytes().length;
    }

    public Object getBodyObject() {
        return bodyObject;
    }

    public void setBodyObject(Object v) {
        this.bodyObject = v;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long v) {
        this.durationMs = v;
    }

    public String getError() {
        return error;
    }

    public void setError(String v) {
        this.error = v;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String v) {
        this.errorType = v;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable v) {
        this.exception = v;
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

    public String getRecordingCode() {
        return recordingCode;
    }

    public void setRecordingCode(String v) {
        this.recordingCode = v;
    }

    public java.util.List<String> getRedirectChain() {
        return redirectChain;
    }

    public void setRedirectChain(java.util.List<String> v) {
        this.redirectChain = v;
    }

    public long getBodySize() {
        return bodySize;
    }

    public void setBodySize(long v) {
        this.bodySize = v;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> v) {
        this.context = v;
    }
}
