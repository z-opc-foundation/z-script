package com.zifang.z.script.engine;

import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.json.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API_BRIDGE DSL 的定义 POJO。
 * <p>
 * 一个 API_BRIDGE 脚本就是把"任意 HTTP 接口"包装成可被 MCP / HTTP 调用的工具。
 * 它由三部分组成:
 * 1) request:  真正要发出去的 HTTP 请求定义 (z-util-http 的 HttpRequestDefinition)
 * 2) inputParams: 用户传入的参数如何映射到 request 的 path / query / header / body
 * 3) outputMapping: response body 上的 JSONPath → 工具输出字段
 * <p>
 * 三者以 JSON 形式存在 Script.sourceCode 中 (因为 z_script 表里没有结构化列存这些)。
 * <p>
 * 一个完整的 sourceCode 例子:
 * <pre>
 * {
 *   "request": {
 *     "httpRequestLine": { "method": "GET", "url": "https://api.github.com/users/${username}" },
 *     "httpRequestHeader": { "headers": { "Accept": "application/json" } },
 *     "httpRequestBody": null
 *   },
 *   "inputParams": [
 *     { "name": "username", "in": "path", "required": true, "description": "GitHub 用户名" }
 *   ],
 *   "outputMapping": [
 *     { "name": "login",     "type": "string",  "jsonPath": "$.login" },
 *     { "name": "id",        "type": "integer", "jsonPath": "$.id" },
 *     { "name": "avatarUrl", "type": "string",  "jsonPath": "$.avatar_url" }
 *   ]
 * }
 * </pre>
 */
public class ApiBridgeDefinition {

    /**
     * HTTP 请求定义 (z-util-http POJO)。
     */
    private HttpRequestDefinition request;

    /**
     * 输入参数定义列表 (用于生成 MCP inputSchema / OpenAPI)。
     */
    private List<InputParam> inputParams = new ArrayList<>();

    /**
     * 输出字段映射 (JSONPath → named output)。
     */
    private List<FieldMapping> outputMapping = new ArrayList<>();

    public static ApiBridgeDefinition fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ApiBridgeDefinition();
        }
        return JsonUtil.fromJson(json, ApiBridgeDefinition.class);
    }

    public HttpRequestDefinition getRequest() {
        return request;
    }

    public void setRequest(HttpRequestDefinition request) {
        this.request = request;
    }

    public List<InputParam> getInputParams() {
        return inputParams;
    }

    public void setInputParams(List<InputParam> inputParams) {
        this.inputParams = inputParams;
    }

    public List<FieldMapping> getOutputMapping() {
        return outputMapping;
    }

    // -------- 子结构 --------

    public void setOutputMapping(List<FieldMapping> outputMapping) {
        this.outputMapping = outputMapping;
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    // -------- 序列化辅助 --------

    /**
     * 将用户传入的 params 合并进 request，产出真正要发的 HttpRequestDefinition。
     * 规则:
     * - in=path 的参数会替换 URL 里的 ${name}
     * - in=query 的参数追加到 URL 的 ?key=value (重复 key 用 &)
     * - in=header 的参数写入 headers
     * - in=body 的参数合并进 JSON body 的顶层 key
     */
    @SuppressWarnings("unchecked")
    public HttpRequestDefinition materialize(Map<String, Object> params) {
        if (request == null) {
            return null;
        }

        // 深拷贝避免污染原对象
        HttpRequestDefinition req = JsonUtil.fromJson(JsonUtil.toJson(request), HttpRequestDefinition.class);

        if (req.getHttpRequestLine() == null || req.getHttpRequestLine().getUrl() == null) {
            return req;
        }
        String url = req.getHttpRequestLine().getUrl();

        // 1) 处理 path 替换 + 把所有 ${name} 都试一遍
        if (params != null) {
            Pattern p = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher m = p.matcher(url);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String key = m.group(1);
                Object val = params.get(key);
                String replacement = val == null ? "" : val.toString();
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            url = sb.toString();
            req.getHttpRequestLine().setUrl(url);
        }

        if (params == null) {
            return req;
        }


        // 2) 把 inputParams 拆分类目处理
        if (inputParams != null) {
            for (InputParam ip : inputParams) {
                Object val = params.get(ip.getName());
                if (val == null && ip.getDefaultValue() != null) val = ip.getDefaultValue();

                if (val == null) {
                    continue;
                }

                switch (ip.getIn() == null ? "query" : ip.getIn()) {
                    case "path":
                        // 已在 ${} 替换里处理
                        break;
                    case "header":
                        if (req.getHttpRequestHeader() == null) continue;

                        req.getHttpRequestHeader().put(ip.getName(), val.toString());
                        break;
                    case "body":
                        if (req.getHttpRequestBody() == null) continue;

                        // 获取已有 body (byte[]) 尝试解析为 JSON 后合并
                        byte[] existingBytes = req.getHttpRequestBody().getBody();
                        Map<String, Object> bodyObj;
                        if (existingBytes == null || existingBytes.length == 0) {
                            bodyObj = new LinkedHashMap<>();
                        } else {
                            String bodyStr = new String(existingBytes, java.nio.charset.StandardCharsets.UTF_8);
                            try {
                                Object parsed = JsonUtil.parseObject(bodyStr);
                                if (parsed instanceof Map) {
                                    bodyObj = (Map<String, Object>) parsed;
                                } else {
                                    bodyObj = new LinkedHashMap<>();
                                }
                            } catch (Exception e) {
                                bodyObj = new LinkedHashMap<>();
                            }
                        }
                        bodyObj.put(ip.getName(), val);
                        req.getHttpRequestBody().setBody(JsonUtil.toJson(bodyObj).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        break;
                    case "query":
                    default:
                        url = req.getHttpRequestLine().getUrl();
                        String sep = url.contains("?") ? "&" : "?";
                        req.getHttpRequestLine().setUrl(url + sep + ip.getName() + "=" + val);
                        break;
                }
            }
        }
        return req;
    }

    /**
     * 根据 outputMapping + 真实 response body 计算工具输出。
     * 实际执行是 JsonPathExtractor.extract，单独抽方法便于测试。
     */
    public Map<String, Object> extractOutput(Object responseBody) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (responseBody == null || outputMapping == null) {
            return out;
        }

        for (FieldMapping fm : outputMapping) {
            if (fm.getName() == null || fm.getJsonPath() == null) {
                continue;
            }

            Object v = JsonPathExtractor.extract(responseBody, fm.getJsonPath());
            out.put(fm.getName(), v);
        }
        return out;
    }

    public static class InputParam {
        private String name;
        private String in = "query";   // path | query | header | body
        private boolean required = false;
        private String description = "";
        private String defaultValue;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIn() {
            return in;
        }

        public void setIn(String in) {
            this.in = in;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    public static class FieldMapping {
        private String name;
        private String type = "string"; // string | integer | number | boolean | object | array
        private String jsonPath;
        private String description = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getJsonPath() {
            return jsonPath;
        }

        public void setJsonPath(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
