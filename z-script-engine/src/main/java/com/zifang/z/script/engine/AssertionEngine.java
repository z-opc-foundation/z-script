package com.zifang.z.script.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 响应断言引擎
 * <p>
 * 支持的断言类型:
 * - STATUS: 断言HTTP状态码
 * - HEADER: 断言响应头 (name + expected)
 * - BODY: 断言响应体 (EXACT/CONTAINS)
 * - JSON_PATH: 断言JSON字段 ($.code == 200)
 * - REGEX: 正则匹配
 * - SCHEMA: JSON Schema (简化版)
 * - RESPONSE_TIME: 响应时间 < ms
 */
public class AssertionEngine {

    private static final Logger log = LogManager.getLogger(AssertionEngine.class);

    /**
     * 执行断言列表
     *
     * @return 断言结果
     */
    public List<AssertionResult> assertAll(List<Map<String, Object>> assertions,
                                           int actualStatus,
                                           Map<String, String> actualHeaders,
                                           String actualBody,
                                           long durationMs) {
        List<AssertionResult> results = new ArrayList<>();
        if (assertions == null || assertions.isEmpty()) {
            return results;
        }
        for (Map<String, Object> assertion : assertions) {
            results.add(assertOne(assertion, actualStatus, actualHeaders, actualBody, durationMs));
        }
        return results;
    }

    public AssertionResult assertOne(Map<String, Object> assertion, int actualStatus,
                                     Map<String, String> actualHeaders, String actualBody, long durationMs) {
        String type = (String) assertion.getOrDefault("type", "JSON_PATH");
        String expression = (String) assertion.get("expression");
        Object expected = assertion.get("expected");
        String comparator = (String) assertion.getOrDefault("comparator", "EQUALS");

        try {
            boolean passed = false;
            String actualValue = null;

            switch (type.toUpperCase()) {
                case "STATUS": {
                    int expStatus = toInt(expected, -1);
                    actualValue = String.valueOf(actualStatus);
                    passed = compare(actualStatus, expStatus, comparator);
                    break;
                }
                case "HEADER": {
                    String headerName = (String) assertion.get("name");
                    String expValue = String.valueOf(expected);
                    String actValue = actualHeaders != null ? actualHeaders.get(headerName) : null;
                    actualValue = actValue;
                    passed = compare(actValue, expValue, comparator);
                    break;
                }
                case "BODY": {
                    actualValue = actualBody;
                    if ("CONTAINS".equalsIgnoreCase(comparator)) {
                        passed = actualBody != null && actualBody.contains(String.valueOf(expected));
                    } else {
                        passed = actualBody != null && actualBody.equals(String.valueOf(expected));
                    }
                    break;
                }
                case "JSON_PATH": {
                    if (actualBody == null || actualBody.isEmpty()) {
                        passed = false;
                        actualValue = "(empty body)";
                    } else {
                        JsonObject json = JsonUtil.parseObject(actualBody);
                        Object actual = evaluateJsonPath(json, expression);
                        actualValue = String.valueOf(actual);
                        passed = compare(actual, expected, comparator);
                    }
                    break;
                }
                case "REGEX": {
                    if (actualBody == null) {
                        passed = false;
                        actualValue = "(empty)";
                        break;
                    }
                    String regex = String.valueOf(expected);
                    Pattern p = Pattern.compile(regex);
                    java.util.regex.Matcher m = p.matcher(actualBody);
                    boolean found = m.find();
                    actualValue = found ? m.group() : "(no match)";
                    passed = "MATCHES".equalsIgnoreCase(comparator) ? found
                            : !"MATCHES".equalsIgnoreCase(comparator);
                    break;
                }
                case "RESPONSE_TIME": {
                    long limit = toLong(expected, 1000);
                    actualValue = durationMs + "ms";
                    passed = durationMs <= limit;
                    break;
                }
                case "SCHEMA": {
                    // Simplified schema check: just verify it's valid JSON
                    try {
                        JsonUtil.parseObject(actualBody);
                        passed = true;
                        actualValue = "valid JSON";
                    } catch (Exception e) {
                        passed = false;
                        actualValue = "invalid JSON: " + e.getMessage();
                    }
                    break;
                }
                default:
                    return AssertionResult.fail(type, "Unknown assertion type: " + type);
            }

            AssertionResult result = new AssertionResult();
            result.setType(type);
            result.setExpression(expression);
            result.setExpected(String.valueOf(expected));
            result.setActual(actualValue);
            result.setPassed(passed);
            result.setComparator(comparator);
            return result;

        } catch (Exception e) {
            log.warn("Assertion failed: {}", assertion, e);
            return AssertionResult.fail(type, "Exception: " + e.getMessage());
        }
    }

    private Object evaluateJsonPath(JsonObject json, String path) {
        if (path == null) {
            return null;
        }

        String[] parts = path.replace("$.", "").split("\\.");
        Object current = json;
        for (String part : parts) {
            if (current instanceof JsonObject) {
                current = ((JsonObject) current).get(part);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }

        }
        return current;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean compare(Object actual, Object expected, String comparator) {
        if ("EQUALS".equalsIgnoreCase(comparator)) {
            if (actual == null) {
                return expected == null || "null".equals(String.valueOf(expected));
            }

            return String.valueOf(actual).equals(String.valueOf(expected));
        }
        if ("NOT_EQUALS".equalsIgnoreCase(comparator)) {
            return !String.valueOf(actual).equals(String.valueOf(expected));
        }
        if ("CONTAINS".equalsIgnoreCase(comparator)) {
            return String.valueOf(actual).contains(String.valueOf(expected));
        }
        // GT/LT for numbers
        try {
            double a = Double.parseDouble(String.valueOf(actual));
            double e = Double.parseDouble(String.valueOf(expected));
            if ("GT".equalsIgnoreCase(comparator)) {
                return a > e;
            }
            if ("LT".equalsIgnoreCase(comparator)) {
                return a < e;
            }
            if ("GTE".equalsIgnoreCase(comparator)) {
                return a >= e;
            }
            if ("LTE".equalsIgnoreCase(comparator)) {
                return a <= e;
            }
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }

    private int toInt(Object o, int def) {
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return def;
        }
    }

    private long toLong(Object o, long def) {
        try {
            return Long.parseLong(o.toString());
        } catch (Exception e) {
            return def;
        }
    }

    public static class AssertionResult {
        private String type;
        private String expression;
        private String expected;
        private String actual;
        private String comparator;
        private boolean passed;
        private String errorMessage;

        public static AssertionResult fail(String type, String msg) {
            AssertionResult r = new AssertionResult();
            r.setType(type);
            r.setPassed(false);
            r.setErrorMessage(msg);
            return r;
        }

        public String getType() {
            return type;
        }

        public void setType(String v) {
            this.type = v;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String v) {
            this.expression = v;
        }

        public String getExpected() {
            return expected;
        }

        public void setExpected(String v) {
            this.expected = v;
        }

        public String getActual() {
            return actual;
        }

        public void setActual(String v) {
            this.actual = v;
        }

        public String getComparator() {
            return comparator;
        }

        public void setComparator(String v) {
            this.comparator = v;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean v) {
            this.passed = v;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String v) {
            this.errorMessage = v;
        }
    }
}
