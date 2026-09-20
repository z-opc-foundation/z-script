package com.zifang.z.script.scene.http.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 链路中单个步骤的定义（纯 DTO，无副作用）。
 *
 * <p>设计原则：
 * <ul>
 *   <li>表达「模板」（含 ${VAR} 占位符），与执行后的实际结果分离，溯源友好。</li>
 *   <li>支持「真实服务」/「Mock 端点」两种目标模式（targetMode）。</li>
 *   <li>支持 JSONPath 变量提取 + 简单断言规则。</li>
 *   <li>不依赖 Spring，纯 POJO，方便上层业务（z-qa 等）直接组装。</li>
 * </ul>
 *
 * <p>典型用例参见 FEATURE052 §3.3。
 */
public class StepDefinition {

    /**
     * 步骤序号（链路内唯一，从小到大执行）。
     */
    private int stepNo;

    /**
     * 步骤名称（人类可读，例如「登录」）。
     */
    private String stepName;

    /**
     * HTTP 方法：GET / POST / PUT / DELETE / PATCH。
     */
    private String method = "GET";

    /**
     * 请求 URL（支持 ${VAR} 替换，相对路径拼接到 env.baseUrl）。
     */
    private String url;

    /**
     * 请求 Header 列表（保留顺序；key 含 ${VAR} 替换）。
     */
    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * Query 参数列表（保留顺序；value 含 ${VAR} 替换）。
     */
    private Map<String, String> query = new LinkedHashMap<>();

    /**
     * 请求体（JSON 字符串，支持 ${VAR} 替换）。
     */
    private String body;

    /**
     * 目标模式：real=真实服务 / mock=Mock 端点。
     */
    private String targetMode = "real";

    /**
     * Mock 端点编码（targetMode=mock 时必填）。
     */
    private String mockEndpointCode;

    /**
     * 变量提取规则列表：[{name, path}]]。
     */
    private List<ExtractRule> extractRules = new ArrayList<>();

    /**
     * 断言规则列表：[{field, op, value}]]。field 以 $. 开头走 JSONPath；其他为字面量字段（status/headers 之类）。
     */
    private List<AssertionRule> assertions = new ArrayList<>();

    /**
     * 单步失败是否中止整条链路（默认 true）。
     */
    private boolean stopOnFail = true;

    /**
     * 最大重试次数（默认 0，不重试）。
     */
    private int maxRetries = 0;

    /**
     * 请求超时（毫秒，默认 10000）。
     */
    private int timeoutMs = 10000;

    // ---------- nested types ----------

    public int getStepNo() {
        return stepNo;
    }

    public void setStepNo(int stepNo) {
        this.stepNo = stepNo;
    }

    // ---------- getters / setters ----------

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public void setQuery(Map<String, String> query) {
        this.query = query;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTargetMode() {
        return targetMode;
    }

    public void setTargetMode(String targetMode) {
        this.targetMode = targetMode;
    }

    public String getMockEndpointCode() {
        return mockEndpointCode;
    }

    public void setMockEndpointCode(String mockEndpointCode) {
        this.mockEndpointCode = mockEndpointCode;
    }

    public List<ExtractRule> getExtractRules() {
        return extractRules;
    }

    public void setExtractRules(List<ExtractRule> extractRules) {
        this.extractRules = extractRules;
    }

    public List<AssertionRule> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<AssertionRule> assertions) {
        this.assertions = assertions;
    }

    public boolean isStopOnFail() {
        return stopOnFail;
    }

    public void setStopOnFail(boolean stopOnFail) {
        this.stopOnFail = stopOnFail;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * 变量提取规则：JSONPath 从响应体里抽取值写入 ${name}。
     */
    public static class ExtractRule {
        private String name;
        private String path; // JSONPath, e.g. "$.orderNo" 或 "$.data.token"
        private String defaultValue; // 提取失败时使用的兜底值

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    /**
     * 断言规则：field + op + expectedValue。
     */
    public static class AssertionRule {
        private String field;     // "status" / "$.code" / "$.data.amount" / "${VAR}" / raw header name
        private String op;        // eq / ne / gt / lt / contains / regex / exists / not_exists
        private Object value;     // 期望值

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOp() {
            return op == null ? "eq" : op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
