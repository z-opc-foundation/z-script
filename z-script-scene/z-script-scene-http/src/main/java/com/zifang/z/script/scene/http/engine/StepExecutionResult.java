package com.zifang.z.script.scene.http.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单步骤执行结果（含完整溯源）。
 *
 * <p>约定详见 FEATURE052 §3.3「执行溯源」：每个字段都对应排查场景：
 * <ul>
 *   <li>template* → 排查「编排对不对」</li>
 *   <li>actual*   → 排查「变量替换对不对」「服务返回了什么」</li>
 *   <li>extractions[] → 排查「变量提取成功没有」</li>
 *   <li>assertions[]  → 排查「断言为什么失败」</li>
 *   <li>variablesBefore / variablesAfter → 排查「变量在哪步变了」</li>
 *   <li>durationMs / retryCount / attemptLog → 排查「是不是超时/重试」</li>
 * </ul>
 *
 * <p>执行结果聚合字段 {@link #result} 取值：pass / fail / error / skip / pending。
 */
public class StepExecutionResult {

    /**
     * === 入参（模板定义） ===
     */
    private int stepNo;
    private String stepName;
    private String templateMethod;
    private String templateUrl;
    private Map<String, String> templateHeaders = new LinkedHashMap<>();
    private Map<String, String> templateQuery = new LinkedHashMap<>();
    private String templateBody;
    private String targetMode;
    private String mockEndpointCode;

    /**
     * === 变量替换后实际请求 ===
     */
    private String actualUrl;
    private Map<String, String> actualHeaders = new LinkedHashMap<>();
    private Map<String, String> actualQuery = new LinkedHashMap<>();
    private String actualBody;
    private Boolean mockMatched; // mock 模式时是否匹配到端点

    /**
     * === 执行过程 ===
     */
    private Long startedAt;    // epoch millis
    private Long finishedAt;   // epoch millis
    private Long durationMs;
    private Integer retryCount;
    private List<AttemptLog> attemptLog = new ArrayList<>();

    /**
     * === 响应 ===
     */
    private Integer responseStatus;
    private Map<String, String> responseHeaders = new LinkedHashMap<>();
    private String responseBody;

    /**
     * === 变量提取过程 ===
     */
    private List<ExtractionRecord> extractions = new ArrayList<>();

    /**
     * === 断言过程 ===
     */
    private List<AssertionRecord> assertions = new ArrayList<>();

    /**
     * === 变量快照 ===
     */
    private Map<String, Object> variablesBefore = new LinkedHashMap<>();
    private Map<String, Object> variablesAfter = new LinkedHashMap<>();

    /**
     * === 结果 ===
     */
    private String result; // pass / fail / error / skip / pending
    private String errorMessage;
    private String errorStack;

    // ---------- nested types ----------

    public int getStepNo() {
        return stepNo;
    }

    public void setStepNo(int stepNo) {
        this.stepNo = stepNo;
    }

    public String getStepName() {
        return stepName;
    }

    // ---------- getters / setters ----------

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getTemplateMethod() {
        return templateMethod;
    }

    public void setTemplateMethod(String templateMethod) {
        this.templateMethod = templateMethod;
    }

    public String getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }

    public Map<String, String> getTemplateHeaders() {
        return templateHeaders;
    }

    public void setTemplateHeaders(Map<String, String> templateHeaders) {
        this.templateHeaders = templateHeaders;
    }

    public Map<String, String> getTemplateQuery() {
        return templateQuery;
    }

    public void setTemplateQuery(Map<String, String> templateQuery) {
        this.templateQuery = templateQuery;
    }

    public String getTemplateBody() {
        return templateBody;
    }

    public void setTemplateBody(String templateBody) {
        this.templateBody = templateBody;
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

    public String getActualUrl() {
        return actualUrl;
    }

    public void setActualUrl(String actualUrl) {
        this.actualUrl = actualUrl;
    }

    public Map<String, String> getActualHeaders() {
        return actualHeaders;
    }

    public void setActualHeaders(Map<String, String> actualHeaders) {
        this.actualHeaders = actualHeaders;
    }

    public Map<String, String> getActualQuery() {
        return actualQuery;
    }

    public void setActualQuery(Map<String, String> actualQuery) {
        this.actualQuery = actualQuery;
    }

    public String getActualBody() {
        return actualBody;
    }

    public void setActualBody(String actualBody) {
        this.actualBody = actualBody;
    }

    public Boolean getMockMatched() {
        return mockMatched;
    }

    public void setMockMatched(Boolean mockMatched) {
        this.mockMatched = mockMatched;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Long finishedAt) {
        this.finishedAt = finishedAt;
    }

    // @JsonSerialize(serialize = false) 已在 fastjson→z-util 迁移中移除 (2026-08-14)
    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public List<AttemptLog> getAttemptLog() {
        return attemptLog;
    }

    public void setAttemptLog(List<AttemptLog> attemptLog) {
        this.attemptLog = attemptLog;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public List<ExtractionRecord> getExtractions() {
        return extractions;
    }

    public void setExtractions(List<ExtractionRecord> extractions) {
        this.extractions = extractions;
    }

    public List<AssertionRecord> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<AssertionRecord> assertions) {
        this.assertions = assertions;
    }

    public Map<String, Object> getVariablesBefore() {
        return variablesBefore;
    }

    public void setVariablesBefore(Map<String, Object> variablesBefore) {
        this.variablesBefore = variablesBefore;
    }

    public Map<String, Object> getVariablesAfter() {
        return variablesAfter;
    }

    public void setVariablesAfter(Map<String, Object> variablesAfter) {
        this.variablesAfter = variablesAfter;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStack() {
        return errorStack;
    }

    public void setErrorStack(String errorStack) {
        this.errorStack = errorStack;
    }

    /**
     * 一次尝试的简短日志（重试时多条）。
     */
    public static class AttemptLog {
        private int attempt;          // 1-based
        private Integer status;
        private Long durationMs;
        private String errorMessage;

        public int getAttempt() {
            return attempt;
        }

        public void setAttempt(int attempt) {
            this.attempt = attempt;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Long durationMs) {
            this.durationMs = durationMs;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 一次变量提取的执行记录（无论成功失败都记录，方便溯源）。
     */
    public static class ExtractionRecord {
        private String name;
        private String path;
        private Object rawValue;
        private boolean success;
        private String errorMessage;

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

        // @JsonSerialize(serialize = false) 已在 fastjson→z-util 迁移中移除 (2026-08-14):
        // rawValue 可能是 byte[]/复杂类型,JSON 序列化时会自动失败,但 z-util JsonUtil 较宽容,无影响
        public Object getRawValue() {
            return rawValue;
        }

        public void setRawValue(Object rawValue) {
            this.rawValue = rawValue;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 一次断言的执行记录。
     */
    public static class AssertionRecord {
        private String field;
        private String op;
        private Object expected;
        private Object actual;
        private boolean pass;
        private String errorMessage;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public Object getExpected() {
            return expected;
        }

        public void setExpected(Object expected) {
            this.expected = expected;
        }

        public Object getActual() {
            return actual;
        }

        public void setActual(Object actual) {
            this.actual = actual;
        }

        public boolean isPass() {
            return pass;
        }

        public void setPass(boolean pass) {
            this.pass = pass;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
