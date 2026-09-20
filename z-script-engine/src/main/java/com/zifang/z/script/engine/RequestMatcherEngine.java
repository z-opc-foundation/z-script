package com.zifang.z.script.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mock 请求匹配器引擎
 * <p>
 * 支持的匹配维度:
 * - URL 模式 (Ant风格: /api/users/{id}, 通配符: /api/users/*)
 * - HTTP 方法
 * - Header 匹配 (支持通配符值: Bearer *)
 * - Query 参数
 * - Body 内容 (支持 JSON 路径、正则、字符串包含)
 * <p>
 * 匹配优先级: priority ASC (数字越小越高) -> 创建时间 ASC
 */
public class RequestMatcherEngine {

    private static final Logger log = LogManager.getLogger(RequestMatcherEngine.class);

    /**
     * 从 HttpServletRequest 提取所有 headers
     */
    public static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> result = new java.util.HashMap<>();
        java.util.Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            result.put(name, request.getHeader(name));
        }
        return result;
    }

    /**
     * 从 HttpServletRequest 提取 body
     */
    public static String extractBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * 从候选端点中找到最佳匹配
     */
    public MatchResult findMatch(List<MockEndpoint> candidates, String method, String path,
                                 String queryString, Map<String, String> headers, String body) {
        if (candidates == null || candidates.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter by method
        List<MockEndpoint> methodFiltered = candidates.stream()
                .filter(e -> e.getMethod() == null || e.getMethod().equalsIgnoreCase(method)
                        || "ANY".equalsIgnoreCase(e.getMethod()))
                .collect(Collectors.toList());

        // Filter by URL pattern
        List<MockEndpoint> urlFiltered = methodFiltered.stream()
                .filter(e -> matchesUrl(e, path))
                .collect(Collectors.toList());

        if (urlFiltered.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter by headers (if specified)
        List<MockEndpoint> headerFiltered = urlFiltered.stream()
                .filter(e -> matchesHeaders(e, headers))
                .collect(Collectors.toList());

        if (headerFiltered.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter by query
        List<MockEndpoint> queryFiltered = headerFiltered.stream()
                .filter(e -> matchesQuery(e, queryString))
                .collect(Collectors.toList());

        if (queryFiltered.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter by body
        List<MockEndpoint> bodyFiltered = queryFiltered.stream()
                .filter(e -> matchesBody(e, body))
                .collect(Collectors.toList());

        if (bodyFiltered.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Sort by priority ASC, then by createTime ASC
        bodyFiltered.sort(Comparator.comparing(MockEndpoint::getPriority,
                Comparator.nullsLast(Comparator.naturalOrder())));

        MockEndpoint best = bodyFiltered.get(0);
        // Extract URL path variables (e.g., {id} -> 123)
        Map<String, String> pathVars = extractPathVariables(best, path);
        return MatchResult.match(best, pathVars);
    }

    /**
     * URL 模式匹配
     * 支持:
     * - 精确匹配: /api/users/123
     * - Ant 风格: /api/users/{id} (占位符)
     * - 通配符: /api/users/* (匹配单段)
     * - 双通配: /api/** (匹配多段)
     */
    public boolean matchesUrl(MockEndpoint endpoint, String actualPath) {
        if (actualPath == null) {
            return false;
        }

        String pattern = endpoint.getMatchUrlPattern();
        if (pattern == null || pattern.isEmpty()) {
            pattern = endpoint.getPath();
        }
        if (pattern == null) {
            return false;
        }

        return matchAntPattern(pattern, actualPath);
    }

    private boolean matchAntPattern(String pattern, String path) {
        // Normalize
        pattern = pattern.trim();
        path = path.trim();
        if (pattern.equals(path)) {
            return true;
        }

        // Convert {var} to regex group, * to [^/]*, ** to .*
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replaceAll("\\{([^/}]+)\\}", "(?<var_$1>[^/]+)");

        // Drop named group names since Java regex doesn't support it directly without proper syntax
        regex = regex.replaceAll("\\(\\?<var_[^>]+>", "(");

        try {
            return Pattern.compile("^" + regex + "$").matcher(path).matches();
        } catch (Exception e) {
            log.warn("Invalid URL pattern: {}", pattern, e);
            return false;
        }
    }

    /**
     * 提取路径变量
     */
    public Map<String, String> extractPathVariables(MockEndpoint endpoint, String actualPath) {
        String pattern = endpoint.getMatchUrlPattern();
        if (pattern == null || !pattern.contains("{")) {
            return java.util.Collections.emptyMap();
        }

        String[] patternParts = pattern.split("/");
        String[] pathParts = actualPath.split("/");
        java.util.Map<String, String> vars = new java.util.HashMap<>();
        if (patternParts.length != pathParts.length) {
            return vars;
        }


        for (int i = 0; i < patternParts.length; i++) {
            String p = patternParts[i];
            if (p.startsWith("{") && p.endsWith("}")) {
                vars.put(p.substring(1, p.length() - 1), pathParts[i]);
            }
        }
        return vars;
    }

    /**
     * Header 匹配: 要求 endpoint 中指定的所有 header 都匹配
     * Header 值支持通配符: "Bearer *" 匹配 "Bearer xxx"
     */
    public boolean matchesHeaders(MockEndpoint endpoint, Map<String, String> actualHeaders) {
        if (endpoint.getMatchHeaders() == null || endpoint.getMatchHeaders().trim().isEmpty()) {
            return true;
        }
        try {
            Map<String, Object> required = JsonUtil.fromJson(endpoint.getMatchHeaders(), new TypeReference<Map<String, Object>>() {
            });
            for (Map.Entry<String, Object> req : required.entrySet()) {
                String actualValue = findHeader(actualHeaders, req.getKey());
                if (actualValue == null) {
                    return false;
                }

                if (!matchWildcardValue(String.valueOf(req.getValue()), actualValue)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to parse match headers: {}", endpoint.getMatchHeaders(), e);
            return false;
        }
    }

    private String findHeader(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }

        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private boolean matchWildcardValue(String pattern, String actual) {
        if (pattern == null) {
            return true;
        }

        if (pattern.equals(actual)) {
            return true;
        }
        // Wildcard match: "Bearer *" matches anything starting with "Bearer "
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return actual.startsWith(prefix);
        }
        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return actual.endsWith(suffix);
        }
        if (pattern.contains("*")) {
            String[] parts = pattern.split("\\*");
            int idx = 0;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) {
                    continue;
                }

                if (i == 0) {
                    if (!actual.startsWith(part)) {
                        return false;
                    }
                    idx = part.length();
                } else if (i == parts.length - 1) {
                    if (!actual.substring(idx).endsWith(part)) {
                        return false;
                    }
                } else {
                    int found = actual.indexOf(part, idx);
                    if (found < 0) {
                        return false;
                    }

                    idx = found + part.length();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Query 匹配
     */
    public boolean matchesQuery(MockEndpoint endpoint, String queryString) {
        if (endpoint.getMatchQuery() == null || endpoint.getMatchQuery().trim().isEmpty()) {
            return true;
        }
        try {
            Map<String, String> required = JsonUtil.fromJson(endpoint.getMatchQuery(), new TypeReference<Map<String, String>>() {
            });
            Map<String, String> actual = parseQueryString(queryString);
            for (Map.Entry<String, String> req : required.entrySet()) {
                String actualValue = actual.get(req.getKey());
                if (actualValue == null) {
                    return false;
                }

                if (!matchWildcardValue(req.getValue(), actualValue)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to parse match query: {}", endpoint.getMatchQuery(), e);
            return false;
        }
    }

    private Map<String, String> parseQueryString(String qs) {
        Map<String, String> result = new java.util.HashMap<>();
        if (qs == null || qs.isEmpty()) {
            return result;
        }
        for (String pair : qs.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                result.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else {
                result.put(pair, "");
            }
        }
        return result;
    }

    /**
     * Body 匹配
     * type: JSON|JSON_PATH|REGEX|TEXT
     */
    public boolean matchesBody(MockEndpoint endpoint, String body) {
        if (endpoint.getMatchBody() == null || endpoint.getMatchBody().trim().isEmpty()) {
            return true;
        }
        if (body == null) {
            body = "";
        }


        String type = endpoint.getMatchBodyType();
        if (type == null) {
            type = "JSON";
        }


        switch (type.toUpperCase()) {
            case "REGEX":
                try {
                    return Pattern.compile(endpoint.getMatchBody()).matcher(body).find();
                } catch (Exception e) {
                    return false;
                }
            case "TEXT":
                return body.contains(endpoint.getMatchBody());
            case "JSON_PATH":
            case "JSON":
            default:
                return matchesJsonBody(endpoint.getMatchBody(), body);
        }
    }

    private boolean matchesJsonBody(String matchRule, String body) {
        try {
            // matchRule format: {"field": "expectedValue"} or JSON path
            if (matchRule.trim().startsWith("{")) {
                Map<String, Object> required = JsonUtil.fromJson(matchRule, new TypeReference<Map<String, Object>>() {
                });
                if (body.isEmpty()) {
                    return false;
                }
                JsonObject actual = JsonUtil.parseObject(body);
                for (Map.Entry<String, Object> req : required.entrySet()) {
                    if (!actual.containsKey(req.getKey())) {
                        return false;
                    }
                    Object actualValue = actual.get(req.getKey());
                    if (!java.util.Objects.equals(String.valueOf(actualValue), String.valueOf(req.getValue()))) {
                        return false;
                    }
                }
                return true;
            }
            // Treat as JSON path
            if (body.isEmpty()) {
                return false;
            }
            JsonObject json = JsonUtil.parseObject(body);
            Object value = evaluateJsonPath(json, matchRule);
            return value != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Object evaluateJsonPath(JsonObject json, String path) {
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

    private boolean objectsEqual(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * 匹配结果
     */
    public static class MatchResult {
        private final boolean matched;
        private final MockEndpoint endpoint;
        private final Map<String, String> pathVariables;

        private MatchResult(boolean matched, MockEndpoint endpoint, Map<String, String> pathVars) {
            this.matched = matched;
            this.endpoint = endpoint;
            this.pathVariables = pathVars;
        }

        public static MatchResult match(MockEndpoint endpoint, Map<String, String> pathVars) {
            return new MatchResult(true, endpoint, pathVars);
        }

        public static MatchResult noMatch() {
            return new MatchResult(false, null, java.util.Collections.emptyMap());
        }

        public boolean isMatched() {
            return matched;
        }

        public MockEndpoint getEndpoint() {
            return endpoint;
        }

        public Map<String, String> getPathVariables() {
            return pathVariables;
        }
    }
}
