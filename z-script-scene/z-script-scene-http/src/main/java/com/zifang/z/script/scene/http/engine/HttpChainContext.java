package com.zifang.z.script.scene.http.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次链路执行的上下文（POJO）。
 *
 * <p>调用方传入，由 HttpChainExecutor 内部使用和修改；调用方拿到结果后可丢弃。
 *
 * <p>变量上下文 variables 使用 Map<String,Object>，顺序为插入顺序（LinkedHashMap），
 * 便于调试和序列化输出。
 */
public class HttpChainContext {

    /**
     * 链路执行 ID（便于日志关联）。
     */
    private String chainId;

    /**
     * baseUrl（用于把步骤的相对 URL 拼接为绝对 URL）。
     */
    private String baseUrl;

    /**
     * Mock 端点编码 -> 预设响应 JSON（targetMode=mock 时通过 code 查表）。可选。
     */
    private Map<String, String> mockEndpointResponses = new LinkedHashMap<>();

    /**
     * 链路初始变量（执行过程中会被覆写）。
     */
    private Map<String, Object> variables = new LinkedHashMap<>();

    /**
     * 是否在第一个失败后中止整条链路（默认 true）。任何步骤都可以按需覆盖。
     */
    private boolean abortOnFirstFail = true;

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Map<String, String> getMockEndpointResponses() {
        return mockEndpointResponses;
    }

    public void setMockEndpointResponses(Map<String, String> mockEndpointResponses) {
        this.mockEndpointResponses = mockEndpointResponses;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public boolean isAbortOnFirstFail() {
        return abortOnFirstFail;
    }

    public void setAbortOnFirstFail(boolean abortOnFirstFail) {
        this.abortOnFirstFail = abortOnFirstFail;
    }

    /**
     * 读取变量（在 attemptLog 中显示成 ${NAME}）。
     */
    public Object getVariable(String name) {
        return variables == null ? null : variables.get(name);
    }

    /**
     * 写入变量（覆写或新增）。
     */
    public void putVariable(String name, Object value) {
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        variables.put(name, value);
    }
}
