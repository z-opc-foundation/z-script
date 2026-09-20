package com.zifang.z.script.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonArray;
import com.zifang.util.json.model.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API 规约解析器 — 把 OpenAPI 3.x / Swagger 2.0 / Postman v2.1 统一解析为 ParsedEndpoint 列表
 * <p>
 * 设计原则：
 * 1. 不引入第三方依赖（swagger-parser 等），纯 fastjson 树形遍历
 * 2. 输出统一的 ParsedEndpoint 形态，便于后端入库 / 前端展示
 * 3. 自动识别格式，按需走对应分支
 * <p>
 * 使用：
 * List<ParsedEndpoint> eps = ApiSpecParser.parse(jsonText);
 * String fmt = ApiSpecParser.detectFormat(jsonText);
 */
public class ApiSpecParser {

    /**
     * 解析入口（自动识别）
     */
    public static List<ParsedEndpoint> parse(String jsonText) {
        Format fmt = detectFormat(jsonText);
        switch (fmt) {
            case OPENAPI_3:
                return parseOpenApi3(jsonText);
            case SWAGGER_2:
                return parseSwagger2(jsonText);
            case POSTMAN_21:
                return parsePostman(jsonText);
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 格式探测（按字段特征）
     */
    public static Format detectFormat(String jsonText) {
        if (jsonText == null) {
            return Format.UNKNOWN;
        }

        String trimmed = jsonText.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return Format.UNKNOWN;

        try {
            JsonObject root = JsonUtil.parseObject(trimmed);
            if (root != null) {
                if (root.containsKey("openapi")) {
                    return Format.OPENAPI_3;
                }
                if (root.containsKey("swagger")) {
                    return Format.SWAGGER_2;
                }
                JsonObject info = root.getJsonObject("info");
                if (info != null && info.containsKey("_postman_id")) {
                    return Format.POSTMAN_21;
                }
                if (root.containsKey("item") && root.containsKey("info")) {
                    return Format.POSTMAN_21;
                }

            }
        } catch (Exception ignored) {
        }
        return Format.UNKNOWN;
    }

    // ========================================================================
    // OpenAPI 3.x
    // ========================================================================
    public static List<ParsedEndpoint> parseOpenApi3(String jsonText) {
        List<ParsedEndpoint> out = new ArrayList<>();
        JsonObject root = JsonUtil.parseObject(jsonText);
        JsonObject paths = root.getJsonObject("paths");
        if (paths == null) {
            return out;
        }


        String[] methods = {"get", "post", "put", "delete", "patch", "head", "options"};
        for (Map.Entry<String, Object> pathEntry : paths.getAllKeyValue()) {
            String path = pathEntry.getKey();
            Object pathObj = pathEntry.getValue();
            if (!(pathObj instanceof JsonObject)) {
                continue;
            }

            JsonObject pathJson = (JsonObject) pathObj;
            // 路径级参数
            List<Map<String, String>> pathParams = extractParameters(pathJson.getJsonArray("parameters"));

            for (String method : methods) {
                JsonObject op = pathJson.getJsonObject(method);
                if (op == null) {
                    continue;
                }
                ParsedEndpoint ep = new ParsedEndpoint();
                ep.method = method.toUpperCase();
                ep.path = path;
                ep.operationId = op.getString("operationId");
                ep.summary = op.getString("summary");
                ep.description = op.getString("description");
                JsonArray tagsArr = op.getJsonArray("tags");
                ep.tags = new ArrayList<>();
                if (tagsArr != null) {
                    for (int i = 0; i < tagsArr.size(); i++) {
                        String tag = tagsArr.getString(i);
                        if (tag != null) {
                            ep.tags.add(tag);
                        }

                    }
                }
                ep.raw = op.getAllKeyValue() != null ? new LinkedHashMap<>(op.getAllKeyValue().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))) : null;
                ep.parameters.addAll(pathParams);
                ep.parameters.addAll(extractParameters(op.getJsonArray("parameters")));

                // requestBody
                JsonObject rb = op.getJsonObject("requestBody");
                if (rb != null) {
                    JsonObject content = rb.getJsonObject("content");
                    if (content != null) {
                        if (content.containsKey("application/json")) {
                            ep.contentType = "application/json";
                            ep.requestBody = extractExampleOrSchema(content.getJsonObject("application/json"));
                        } else if (content.containsKey("application/x-www-form-urlencoded")) {
                            ep.contentType = "application/x-www-form-urlencoded";
                        } else if (content.containsKey("multipart/form-data")) {
                            ep.contentType = "multipart/form-data";
                        }
                    }
                }

                // responses (取 200/201 第一个)
                JsonObject responses = op.getJsonObject("responses");
                if (responses != null) {
                    for (String code : new String[]{"200", "201", "default"}) {
                        JsonObject r = responses.getJsonObject(code);
                        if (r != null) {
                            try {
                                ep.statusCode = Integer.parseInt(code);
                            } catch (Exception ignored) {
                            }
                            JsonObject c = r.getJsonObject("content");
                            if (c != null && c.containsKey("application/json")) {
                                ep.responseExample = extractExampleOrSchema(c.getJsonObject("application/json"));
                            }
                            break;
                        }
                    }
                }
                if (ep.statusCode == null) {
                    ep.statusCode = 200;
                }

                out.add(ep);
            }
        }
        return out;
    }

    private static List<Map<String, String>> extractParameters(JsonArray arr) {
        List<Map<String, String>> out = new ArrayList<>();
        if (arr == null) {
            return out;
        }

        for (int i = 0; i < arr.size(); i++) {
            JsonObject p = arr.getJsonObject(i);
            if (p == null) {
                continue;
            }
            Map<String, String> m = new LinkedHashMap<>();
            m.put("name", p.getString("name"));
            m.put("in", p.getString("in"));
            m.put("required", String.valueOf(Boolean.TRUE.equals(p.getBoolean("required"))));
            JsonObject schema = p.getJsonObject("schema");
            if (schema != null) {
                m.put("type", schema.getString("type"));
            }

            m.put("description", p.getString("description"));
            out.add(m);
        }
        return out;
    }

    private static String extractExampleOrSchema(JsonObject media) {
        if (media == null) {
            return null;
        }

        if (media.containsKey("example")) {
            Object example = media.get("example");
            return example instanceof String
                    ? media.getString("example")
                    : JsonUtil.toJson(media.get("example"));
        }
        if (media.containsKey("examples")) {
            JsonObject examples = media.getJsonObject("examples");
            if (examples != null && !examples.isEmpty()) {
                String first = null;
                for (Map.Entry<String, Object> entry : examples.getAllKeyValue()) {
                    first = entry.getKey();
                    break;
                }
                if (first != null) {
                    JsonObject firstExample = examples.getJsonObject(first);
                    Object v = firstExample != null ? firstExample.get("value") : null;
                    if (v != null) {
                        return v instanceof String ? (String) v : JsonUtil.toJson(v);
                    }

                }
            }
        }
        if (media.containsKey("schema")) {
            return JsonUtil.toJson(media.get("schema"));
        }
        return null;
    }

    // ========================================================================
    // Swagger 2.0（简化实现）
    // ========================================================================
    public static List<ParsedEndpoint> parseSwagger2(String jsonText) {
        List<ParsedEndpoint> out = new ArrayList<>();
        JsonObject root = JsonUtil.parseObject(jsonText);
        JsonObject paths = root.getJsonObject("paths");
        if (paths == null) {
            return out;
        }

        String[] methods = {"get", "post", "put", "delete", "patch", "head", "options"};
        for (Map.Entry<String, Object> pathEntry : paths.getAllKeyValue()) {
            String path = pathEntry.getKey();
            Object pathObj = pathEntry.getValue();
            if (!(pathObj instanceof JsonObject)) {
                continue;
            }

            JsonObject pathJson = (JsonObject) pathObj;
            List<Map<String, String>> pathParams = extractParameters(pathJson.getJsonArray("parameters"));
            for (String method : methods) {
                JsonObject op = pathJson.getJsonObject(method);
                if (op == null) {
                    continue;
                }
                ParsedEndpoint ep = new ParsedEndpoint();
                ep.method = method.toUpperCase();
                ep.path = path;
                ep.operationId = op.getString("operationId");
                ep.summary = op.getString("summary");
                ep.description = op.getString("description");
                JsonArray tagsArr = op.getJsonArray("tags");
                ep.tags = new ArrayList<>();
                if (tagsArr != null) {
                    for (int i = 0; i < tagsArr.size(); i++) {
                        String tag = tagsArr.getString(i);
                        if (tag != null) {
                            ep.tags.add(tag);
                        }

                    }
                }
                ep.parameters.addAll(pathParams);
                ep.parameters.addAll(extractParameters(op.getJsonArray("parameters")));
                ep.contentType = "application/json";
                ep.statusCode = 200;
                ep.raw = op.getAllKeyValue() != null ? new LinkedHashMap<>(op.getAllKeyValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))) : null;
                out.add(ep);
            }
        }
        return out;
    }

    // ========================================================================
    // Postman v2.1
    // ========================================================================
    public static List<ParsedEndpoint> parsePostman(String jsonText) {
        List<ParsedEndpoint> out = new ArrayList<>();
        JsonObject root = JsonUtil.parseObject(jsonText);
        walkPostmanItems(root.getJsonArray("item"), out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void walkPostmanItems(JsonArray items, List<ParsedEndpoint> out) {
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            JsonObject node = items.getJsonObject(i);
            if (node == null) {
                continue;
            }
            if (node.containsKey("item")) {
                walkPostmanItems(node.getJsonArray("item"), out);
            } else if (node.containsKey("request")) {
                ParsedEndpoint ep = new ParsedEndpoint();
                ep.raw = node.getAllKeyValue() != null ? new LinkedHashMap<>(node.getAllKeyValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))) : null;
                ep.summary = node.getString("name");
                ep.description = node.getString("description");

                Object req = node.get("request");
                if (req instanceof String) {
                    ep.method = "GET";
                    ep.path = (String) req;
                } else if (req instanceof JsonObject) {
                    JsonObject r = (JsonObject) req;
                    ep.method = r.getString("method") == null ? "GET" : r.getString("method").toUpperCase();
                    Object url = r.get("url");
                    if (url instanceof String) {
                        ep.path = stripHost((String) url);
                    } else if (url instanceof JsonObject) {
                        String raw = ((JsonObject) url).getString("raw");
                        ep.path = stripHost(raw);
                    }
                    // headers
                    JsonArray headers = r.getJsonArray("header");
                    if (headers != null) {
                        for (int j = 0; j < headers.size(); j++) {
                            JsonObject h = headers.getJsonObject(j);
                            if (h != null) {
                                Map<String, String> m = new LinkedHashMap<>();
                                m.put("name", h.getString("key"));
                                m.put("in", "header");
                                m.put("required", "false");
                                ep.parameters.add(m);
                            }
                        }
                    }
                    // body
                    JsonObject body = r.getJsonObject("body");
                    if (body != null) {
                        String mode = body.getString("mode");
                        ep.contentType = body.getString("mode") == null ? "application/json" : "application/" + mode;
                        if ("raw".equals(mode)) {
                            ep.requestBody = body.getString("raw");
                            if (body.containsKey("options")) {
                                JsonObject opts = body.getJsonObject("options");
                                if (opts != null && opts.getJsonObject("raw") != null) {
                                    String lang = opts.getJsonObject("raw").getString("language");
                                    if ("json".equals(lang)) {
                                        ep.contentType = "application/json";
                                    }
                                }
                            }
                        }
                    }
                }
                ep.statusCode = 200;
                if (ep.method != null && ep.path != null) {
                    out.add(ep);
                }

            }
        }
    }

    private static String stripHost(String url) {
        if (url == null) {
            return "/";
        }

        int idx = url.indexOf("://");
        if (idx >= 0) {
            int pathStart = url.indexOf('/', idx + 3);
            if (pathStart < 0) {
                return "/";
            }

            return url.substring(pathStart);
        }
        return url;
    }

    public enum Format {
        OPENAPI_3, SWAGGER_2, POSTMAN_21, UNKNOWN
    }

    /**
     * 单个端点解析结果（统一模型）
     */
    public static class ParsedEndpoint {
        public String method;        // GET/POST/PUT/DELETE/PATCH
        public String path;          // /users/{id}
        public String operationId;   // getUserById
        public String summary;       // 简短描述
        public String description;   // 详细描述
        public List<String> tags = new ArrayList<>();
        public List<Map<String, String>> parameters = new ArrayList<>();   // name/in/required/type
        public String requestBody;   // 请求体（尽力而为，OpenAPI 拿 schema 示例）
        public String contentType;   // application/json
        public Integer statusCode;   // 期望 200
        public String responseExample; // 响应示例（尽力而为）
        public Map<String, Object> raw; // 原始 op 对象（备用）

        public String label() {
            return method + " " + path + (summary == null || summary.isEmpty() ? "" : "  — " + summary);
        }
    }
}
