package com.zifang.z.script.scene.http.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonArray;
import com.zifang.z.script.engine.JsonPathExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP 链路编排执行引擎 — FEATURE052 Phase 1 的核心组件。
 *
 * <p>职责：
 * <ul>
 *   <li>按 stepNo 顺序遍历所有 StepDefinition</li>
 *   <li>每步：变量替换 → real/mock 分流 → HTTP 调用 / 取 mock 响应 → JSONPath 提取 → 断言</li>
 *   <li>完整溯源（template* / actual* / extractions / assertions / variablesBefore&After / attemptLog / durationMs）</li>
 *   <li>失败处理：单步失败 + stopOnFail → 后续步骤标 skip；abortOnFirstFail 全局中止</li>
 *   <li>重试：maxRetries（默认 0）</li>
 * </ul>
 *
 * <p>实现细节：
 * <ul>
 *   <li>真实 HTTP 调用使用 JDK 自带 {@link HttpURLConnection}，避免引入第三方 HTTP 客户端</li>
 *   <li>变量复用 z-script-engine 已有的 {@link JsonPathExtractor}（极简 JSONPath 子集）</li>
 *   <li>断言自己实现（轻量级，避免依赖 z-script-engine 中面向不同场景的 AssertionEngine）</li>
 * </ul>
 *
 * <p>线程安全：所有内部状态来自调用方参数；该类无成员状态，可多线程调用。
 */
@Component
public class HttpChainExecutor {

    private static final Logger log = LogManager.getLogger(HttpChainExecutor.class);

    private static String joinBaseUrl(String url, String base) {
        if (url == null) {
            url = "";
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (base == null || base.isEmpty()) {
            return url;
        }
        if (base.endsWith("/") && url.startsWith("/")) {
            return base + url.substring(1);
        }
        if (!base.endsWith("/") && !url.startsWith("/")) {
            return base + "/" + url;
        }
        return base + url;
    }

    // ===== 单步执行 =====

    private static String firstHeader(Map<String, String> headers, String key, String def) {
        if (headers == null) {
            return def;
        }
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return def;
    }

    private static String urlEncode(String s) {
        if (s == null) {
            return "";
        }

        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private static String readStream(java.io.InputStream in) throws IOException {
        if (in == null) {
            return "";
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    /**
     * 提供给上层业务（z-qa）将 DB JSON 反序列化为步骤定义。
     */
    public static List<StepDefinition> parseSteps(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JsonArray arr = JsonUtil.parseArray(json);
            List<StepDefinition> list = new ArrayList<>(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                Object item = arr.get(i);
                list.add(JsonUtil.fromJson(JsonUtil.toJson(item), StepDefinition.class));
            }
            return list;
        } catch (Exception ex) {
            throw new RuntimeException("parse steps failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 序列化步骤定义为 JSON（前端编辑后保存回 DB 时使用）。
     */
    public static String toJson(List<StepDefinition> steps) {
        if (steps == null) {
            return "[]";
        }

        return JsonUtil.toJson(steps);
    }

    /**
     * 收集单步响应（key=stepName, value=responseBody），方便上层做 ad-hoc 校验。
     */
    public static Map<String, String> stepBodies(ChainExecutionResult chain) {
        Map<String, String> out = new LinkedHashMap<>();
        for (StepExecutionResult s : chain.getSteps()) {
            out.put(s.getStepName() == null ? "step-" + s.getStepNo() : s.getStepName(),
                    s.getResponseBody());
        }
        return out;
    }

    /**
     * 同步执行整条链路。
     *
     * @param steps   步骤定义列表（顺序敏感，会按 stepNo 排序）
     * @param context 执行上下文（变量、baseUrl、mock 端点响应等）
     * @return 整条链路执行结果（含每步全量溯源）
     */
    public ChainExecutionResult execute(List<StepDefinition> steps, HttpChainContext context) {
        long chainStart = System.currentTimeMillis();
        ChainExecutionResult chain = new ChainExecutionResult();
        chain.setChainId(context.getChainId());

        if (steps == null || steps.isEmpty()) {
            chain.setResult("pass");
            chain.setDurationMs(0);
            return chain;
        }

        // 按 stepNo 排序（保险，调用方传错顺序也能正确执行）
        List<StepDefinition> sorted = new ArrayList<>(steps);
        sorted.sort((a, b) -> Integer.compare(a.getStepNo(), b.getStepNo()));
        chain.setTotalSteps(sorted.size());

        boolean aborted = false;
        String abortReason = null;

        for (StepDefinition step : sorted) {
            if (aborted) {
                // 后续步骤全部 skip
                StepExecutionResult sk = markSkip(step);
                chain.getSteps().add(sk);
                chain.setSkipSteps(chain.getSkipSteps() + 1);
                continue;
            }
            StepExecutionResult stepResult = executeOne(step, context);
            chain.getSteps().add(stepResult);

            String r = stepResult.getResult();
            if ("pass".equals(r)) {
                chain.setPassSteps(chain.getPassSteps() + 1);
            } else if ("skip".equals(r)) {
                chain.setSkipSteps(chain.getSkipSteps() + 1);
            } else {
                // fail / error
                chain.setFailSteps(chain.getFailSteps() + 1);
                if ((step.isStopOnFail() && context.isAbortOnFirstFail()) || context.isAbortOnFirstFail()) {
                    aborted = true;
                    abortReason = stepResult.getErrorMessage() != null
                            ? stepResult.getErrorMessage()
                            : ("step " + step.getStepNo() + " failed");
                }
            }
        }

        chain.setVariablesFinal(new LinkedHashMap<>(context.getVariables()));
        chain.setDurationMs(System.currentTimeMillis() - chainStart);

        // 整条链路结果：fail>0 → fail；否则全部跳过 → aborted；否则 pass
        if (chain.getFailSteps() > 0) {
            chain.setResult("fail");
            chain.setErrorMessage(abortReason);
        } else if (chain.getPassSteps() == 0 && chain.getSkipSteps() > 0) {
            chain.setResult("aborted");
        } else {
            chain.setResult("pass");
        }

        log.info("HttpChain[{}] finished: steps={}/{} pass, {} fail, {} skip, {} ms, result={}",
                context.getChainId(),
                chain.getPassSteps(), chain.getTotalSteps(), chain.getFailSteps(),
                chain.getSkipSteps(), chain.getDurationMs(), chain.getResult());

        return chain;
    }

    private StepExecutionResult executeOne(StepDefinition step, HttpChainContext context) {
        StepExecutionResult r = new StepExecutionResult();
        r.setStepNo(step.getStepNo());
        r.setStepName(step.getStepName());
        r.setTemplateMethod(step.getMethod());
        r.setTemplateUrl(step.getUrl());
        r.setTemplateHeaders(new LinkedHashMap<>(step.getHeaders()));
        r.setTemplateQuery(new LinkedHashMap<>(step.getQuery()));
        r.setTemplateBody(step.getBody());
        r.setTargetMode(step.getTargetMode());
        r.setMockEndpointCode(step.getMockEndpointCode());

        // 步骤前的变量快照
        r.setVariablesBefore(new LinkedHashMap<>(context.getVariables()));

        long t0 = System.currentTimeMillis();
        r.setStartedAt(t0);
        r.setRetryCount(0);
        boolean anyAttemptOk = false;
        String lastError = null;

        // 重试循环
        int totalAttempts = Math.max(1, step.getMaxRetries() + 1);
        for (int attemptIdx = 0; attemptIdx < totalAttempts; attemptIdx++) {
            StepExecutionResult.AttemptLog logEntry = new StepExecutionResult.AttemptLog();
            logEntry.setAttempt(attemptIdx + 1);
            long attemptStart = System.currentTimeMillis();

            try {
                // 1. 替换变量（每次重试都重新替换，便于在第一步提取变量后立刻生效）
                applyReplace(step, context, r);

                // 2. 真实 / Mock 分流
                if ("mock".equalsIgnoreCase(step.getTargetMode())) {
                    dispatchMock(step, context, r);
                } else {
                    dispatchReal(step, context, r, attemptStart);
                }

                // 3. 提取变量
                doExtractions(step, context, r);

                // 4. 断言
                List<StepExecutionResult.AssertionRecord> assertionRecs =
                        doAssertions(step, r, System.currentTimeMillis() - attemptStart);
                r.getAssertions().addAll(assertionRecs);
                long attemptDuration = System.currentTimeMillis() - attemptStart;
                logEntry.setDurationMs(attemptDuration);
                logEntry.setStatus(r.getResponseStatus());
                r.getAttemptLog().add(logEntry);

                long t1 = System.currentTimeMillis();
                r.setFinishedAt(t1);
                r.setDurationMs(t1 - t0);

                if (r.getResponseStatus() != null && r.getResponseStatus() >= 200 && r.getResponseStatus() < 300) {
                    boolean anyAssertionFail = false;
                    for (StepExecutionResult.AssertionRecord a : r.getAssertions()) {
                        if (!a.isPass()) {
                            anyAssertionFail = true;
                            break;
                        }
                    }
                    if (!anyAssertionFail) {
                        r.setResult("pass");
                        anyAttemptOk = true;
                        break;
                    }
                    lastError = "assertion failed";
                } else {
                    lastError = "http status " + r.getResponseStatus();
                }
            } catch (Exception ex) {
                long attemptDuration = System.currentTimeMillis() - attemptStart;
                logEntry.setDurationMs(attemptDuration);
                logEntry.setErrorMessage(ex.getMessage());
                r.getAttemptLog().add(logEntry);
                lastError = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                log.warn("step[{}] attempt[{}] failed: {}", step.getStepNo(), attemptIdx + 1, lastError);
            }
            r.setRetryCount(r.getRetryCount() == null ? 1 : r.getRetryCount() + 1);
        }

        if (!anyAttemptOk) {
            r.setResult("fail");
            r.setErrorMessage(lastError == null ? "unknown error" : lastError);
        }
        r.setVariablesAfter(new LinkedHashMap<>(context.getVariables()));
        return r;
    }

    /**
     * 应用变量替换：把 template* 写到 actual*。
     */
    private void applyReplace(StepDefinition step, HttpChainContext context, StepExecutionResult r) {
        Map<String, Object> v = context.getVariables();
        r.setActualUrl(VariableReplacer.replaceDeep(step.getUrl(), v));
        r.setActualQuery(new LinkedHashMap<>());
        for (Map.Entry<String, String> e : step.getQuery().entrySet()) {
            r.getActualQuery().put(e.getKey(), VariableReplacer.replaceDeep(e.getValue(), v));
        }
        r.setActualHeaders(new LinkedHashMap<>());
        for (Map.Entry<String, String> e : step.getHeaders().entrySet()) {
            r.getActualHeaders().put(e.getKey(), VariableReplacer.replaceDeep(e.getValue(), v));
        }
        r.setActualBody(VariableReplacer.replaceDeep(step.getBody(), v));
    }

    // ===== utils =====

    /**
     * 真实 HTTP 调用。
     */
    private void dispatchReal(StepDefinition step, HttpChainContext context, StepExecutionResult r, long attemptStart) throws IOException {
        String fullUrl = joinBaseUrl(r.getActualUrl(), context.getBaseUrl());
        if (!fullUrl.contains("?")) {
            StringBuilder qs = new StringBuilder();
            for (Map.Entry<String, String> e : r.getActualQuery().entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }

                qs.append(qs.length() == 0 ? "?" : "&")
                        .append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue()));
            }
            fullUrl = fullUrl + qs.toString();
        } else {
            StringBuilder qs = new StringBuilder();
            for (Map.Entry<String, String> e : r.getActualQuery().entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                qs.append("&").append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue()));
            }
            fullUrl = fullUrl + qs.toString();
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        try {
            conn.setRequestMethod(step.getMethod() == null ? "GET" : step.getMethod().toUpperCase());
            conn.setConnectTimeout(step.getTimeoutMs());
            conn.setReadTimeout(step.getTimeoutMs());
            conn.setDoInput(true);
            if (step.getBody() != null && !step.getBody().isEmpty()
                    && !"GET".equalsIgnoreCase(step.getMethod())) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", firstHeader(r.getActualHeaders(), "Content-Type", "application/json"));
            }
            for (Map.Entry<String, String> e : r.getActualHeaders().entrySet()) {
                if ("Content-Type".equalsIgnoreCase(e.getKey())) {
                    continue;
                }

                conn.setRequestProperty(e.getKey(), e.getValue());
            }
            conn.connect();

            if (conn.getDoOutput()) {
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] body = r.getActualBody() == null ? new byte[0] : r.getActualBody().getBytes(StandardCharsets.UTF_8);
                    os.write(body);
                }
            }

            int status = conn.getResponseCode();
            r.setResponseStatus(status);
            Map<String, String> respHeaders = new LinkedHashMap<>();
            conn.getHeaderFields().forEach((k, v2) -> {
                if (k != null && v2 != null && !v2.isEmpty()) {
                    respHeaders.put(k, v2.get(0));
                }

            });
            r.setResponseHeaders(respHeaders);

            String body = readStream(status >= 400 ? conn.getErrorStream() : conn.getInputStream());
            r.setResponseBody(body);
        } finally {
            try {
                conn.disconnect();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Mock 分流：从 context.mockEndpointResponses 取预设响应。
     */
    private void dispatchMock(StepDefinition step, HttpChainContext context, StepExecutionResult r) {
        String code = step.getMockEndpointCode();
        if (code == null || code.isEmpty()) {
            r.setResult("error");
            r.setErrorMessage("mock mode but mockEndpointCode is empty");
            r.setMockMatched(false);
            return;
        }
        String mocked = context.getMockEndpointResponses() == null ? null : context.getMockEndpointResponses().get(code);
        if (mocked == null) {
            r.setResult("error");
            r.setErrorMessage("mock endpoint not registered: " + code);
            r.setMockMatched(false);
            return;
        }
        r.setMockMatched(true);
        r.setResponseStatus(200);
        Map<String, String> respHeaders = new LinkedHashMap<>();
        respHeaders.put("Content-Type", "application/json");
        respHeaders.put("X-Mock-Source", code);
        r.setResponseHeaders(respHeaders);
        r.setResponseBody(mocked);
    }

    /**
     * 提取变量：根据 step.extractRules 从响应体取 JSONPath。
     */
    @SuppressWarnings("unchecked")
    private void doExtractions(StepDefinition step, HttpChainContext context, StepExecutionResult r) {
        if (step.getExtractRules() == null) {
            return;
        }

        for (StepDefinition.ExtractRule rule : step.getExtractRules()) {
            StepExecutionResult.ExtractionRecord er = new StepExecutionResult.ExtractionRecord();
            er.setName(rule.getName());
            er.setPath(rule.getPath());
            try {
                Object val;
                if (rule.getPath() == null || !rule.getPath().startsWith("$")) {
                    // 非 JSONPath 写法：直接走 status / headers / raw body
                    if ("status".equalsIgnoreCase(rule.getPath())) {
                        val = r.getResponseStatus();
                    } else if ("body".equalsIgnoreCase(rule.getPath())) {
                        val = r.getResponseBody();
                    } else {
                        val = r.getResponseHeaders() == null ? null : r.getResponseHeaders().get(rule.getPath());
                    }
                } else {
                    val = JsonPathExtractor.extractFromJson(r.getResponseBody(), rule.getPath());
                }
                if (val == null && rule.getDefaultValue() != null) {
                    val = rule.getDefaultValue();
                    er.setSuccess(true);
                } else if (val != null) {
                    er.setSuccess(true);
                } else {
                    er.setSuccess(false);
                    er.setErrorMessage("extracted null");
                }
                er.setRawValue(val);
                if (val != null) {
                    context.putVariable(rule.getName(), val);
                }
            } catch (Exception ex) {
                er.setSuccess(false);
                er.setErrorMessage(ex.getMessage());
            }
            r.getExtractions().add(er);
        }
    }

    /**
     * 执行断言（支持 eq/ne/gt/lt/contains/regex/exists/not_exists）。
     * field 以 $. 开头走 JSONPath；status/headers.<name>/rawBody 是特殊关键字。
     */
    private List<StepExecutionResult.AssertionRecord> doAssertions(StepDefinition step, StepExecutionResult r, long durationMs) {
        List<StepExecutionResult.AssertionRecord> out = new ArrayList<>();
        if (step.getAssertions() == null) {
            return out;
        }

        for (StepDefinition.AssertionRule rule : step.getAssertions()) {
            StepExecutionResult.AssertionRecord rec = new StepExecutionResult.AssertionRecord();
            rec.setField(rule.getField());
            rec.setOp(rule.getOp());
            Object expected = rule.getValue();
            rec.setExpected(expected);
            Object actual;
            try {
                if ("status".equalsIgnoreCase(rule.getField())) {
                    actual = r.getResponseStatus();
                } else if (rule.getField() != null && rule.getField().startsWith("$")) {
                    actual = JsonPathExtractor.extractFromJson(r.getResponseBody(), rule.getField());
                } else if (rule.getField() != null && rule.getField().startsWith("headers.")) {
                    String hk = rule.getField().substring("headers.".length());
                    actual = r.getResponseHeaders() == null ? null : r.getResponseHeaders().get(hk);
                } else {
                    actual = JsonPathExtractor.extractFromJson(r.getResponseBody(), "$." + rule.getField());
                }
                rec.setActual(actual);
                boolean pass = compare(rule.getOp(), actual, expected);
                rec.setPass(pass);
                if (!pass) {
                    rec.setErrorMessage("expected " + expected + " but actual " + actual);
                }

            } catch (Exception ex) {
                rec.setPass(false);
                rec.setErrorMessage(ex.getMessage());
            }
            out.add(rec);
        }
        return out;
    }

    // ===== 静态便捷方法：解析 JSON 字符串为 List<StepDefinition> =====

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean compare(String op, Object actual, Object expected) {
        if (op == null) {
            op = "eq";
        }

        switch (op) {
            case "exists":
                return actual != null;
            case "not_exists":
                return actual == null;
            case "ne":
                if (actual == null) {
                    return expected != null;
                }

                return !actual.equals(expected);
            case "eq":
                if (actual == null) {
                    return expected == null || "null".equals(expected);
                }

                if (expected == null) {
                    return false;
                }

                if (actual instanceof Number && expected instanceof Number) {
                    return Double.compare(((Number) actual).doubleValue(), ((Number) expected).doubleValue()) == 0;
                }
                return actual.toString().equals(expected.toString());
            case "gt":
                return compareNumeric(actual, expected) > 0;
            case "lt":
                return compareNumeric(actual, expected) < 0;
            case "contains":
                if (actual == null) {
                    return false;
                }

                return actual.toString().contains(expected == null ? "" : expected.toString());
            case "regex": {
                if (actual == null) {
                    return false;
                }

                return java.util.regex.Pattern
                        .compile(expected == null ? "" : expected.toString())
                        .matcher(actual.toString()).find();
            }
            default:
                return false;
        }
    }

    private int compareNumeric(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            return Double.compare(((Number) actual).doubleValue(), ((Number) expected).doubleValue());
        }
        try {
            double a = Double.parseDouble(actual == null ? "" : actual.toString());
            double b = Double.parseDouble(expected == null ? "" : expected.toString());
            return Double.compare(a, b);
        } catch (Exception ex) {
            return 0;
        }
    }

    private StepExecutionResult markSkip(StepDefinition step) {
        StepExecutionResult r = new StepExecutionResult();
        r.setStepNo(step.getStepNo());
        r.setStepName(step.getStepName());
        r.setTemplateMethod(step.getMethod());
        r.setTemplateUrl(step.getUrl());
        r.setTargetMode(step.getTargetMode());
        r.setMockEndpointCode(step.getMockEndpointCode());
        r.setResult("skip");
        r.setErrorMessage("aborted by previous step failure");
        return r;
    }
}
