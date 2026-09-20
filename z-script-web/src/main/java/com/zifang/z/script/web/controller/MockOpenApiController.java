package com.zifang.z.script.web.controller;

import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.engine.ApiSpecParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * OpenAPI / Swagger / Postman 批量导入 Mock 端点。
 * <p>
 * API 基础路径: /api/mock-platform/openapi
 * 所属模块: z-script-web
 * 鉴权: 暂未接入统一鉴权
 * <p>
 * 主要端点:
 * <ul>
 *   <li>POST /api/mock-platform/openapi/parse        — 解析（不落库）</li>
 *   <li>POST /api/mock-platform/openapi/import-batch — 解析后批量入库为 MockEndpoint</li>
 *   <li>GET  /api/mock-platform/openapi/sample       — 返回 OpenAPI 3.x / Swagger 2 / Postman 示例文本</li>
 * </ul>
 */
@Tag(name = "Mock-开放API")
@RestController
@RequestMapping("/api/mock-platform/openapi")
public class MockOpenApiController {

    @Autowired
    private MockEndpointMapper endpointMapper;

    // =================================================================
    // 1. 解析（不落库）
    // =================================================================

    /**
     * 构造有序 Map 的辅助方法，按入参顺序保留键值对。
     *
     * @param kvs 变长参数，按 key1, value1, key2, value2 ... 排列
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 按插入顺序排列的 LinkedHashMap
     */
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> mapOf(Object... kvs) {
        Map<K, V> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            m.put((K) kvs[i], (V) kvs[i + 1]);
        }
        return m;
    }

    // =================================================================
    // 2. 批量入库
    // =================================================================

    /**
     * 仅解析 OpenAPI / Swagger / Postman 内容，不入库。
     *
     * @param body 请求体，包含 key {@code content}
     * @return 成功时返回 success=true、format、count 与 endpoints 列表；失败时返回 success=false 与错误信息
     */
    @Operation(summary = "解析OpenAPI/Swagger/Postman内容")
    @PostMapping("/parse")
    public Object parse(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return mapOf("success", false, "message", "content is required");
        }
        try {
            ApiSpecParser.Format fmt = ApiSpecParser.detectFormat(content);
            List<ApiSpecParser.ParsedEndpoint> eps = ApiSpecParser.parse(content);
            return mapOf(
                    "success", true,
                    "format", fmt.name(),
                    "count", eps.size(),
                    "endpoints", eps
            );
        } catch (Exception e) {
            return mapOf("success", false, "message", "Parse failed: " + e.getMessage());
        }
    }

    // =================================================================
    // 3. 示例
    // =================================================================

    /**
     * 解析内容后批量入库为 MockEndpoint。可通过 selected 指定需要导入的端点子集。
     *
     * @param body 请求体，包含 {@code content}；可选 {@code selected}、{@code envCode}、{@code projectCode}、{@code defaultResponse}、{@code defaultStatus}、{@code priority}、{@code tagPrefix}
     * @return 成功时返回 success=true、imported、failed 与 mockCodes 列表；解析失败时返回 success=false 与错误信息
     */
    @Operation(summary = "批量导入为Mock端点")
    @PostMapping("/import-batch")
    public Object importBatch(@RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return mapOf("success", false, "message", "content is required");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selected = (List<Map<String, Object>>) body.get("selected");
        String envCode = (String) body.getOrDefault("envCode", "default");
        String projectCode = (String) body.getOrDefault("projectCode", "default");
        String defaultResponse = (String) body.getOrDefault("defaultResponse", "{\"code\":0,\"message\":\"ok\",\"data\":{}}");
        Integer defaultStatus = (Integer) body.getOrDefault("defaultStatus", 200);
        Integer priority = (Integer) body.getOrDefault("priority", 5);
        String tagPrefix = (String) body.getOrDefault("tagPrefix", "openapi-import");

        try {
            List<ApiSpecParser.ParsedEndpoint> all = ApiSpecParser.parse(content);
            List<ApiSpecParser.ParsedEndpoint> toImport = new ArrayList<>();
            if (selected == null || selected.isEmpty()) {
                toImport = all;
            } else {
                Set<String> keys = new HashSet<>();
                for (Map<String, Object> s : selected) {
                    keys.add(s.get("method") + "::" + s.get("path"));
                }
                for (ApiSpecParser.ParsedEndpoint ep : all) {
                    if (keys.contains(ep.method + "::" + ep.path)) {
                        toImport.add(ep);
                    }

                }
            }

            int ok = 0, fail = 0;
            List<String> codes = new ArrayList<>();
            for (ApiSpecParser.ParsedEndpoint ep : toImport) {
                try {
                    MockEndpoint m = new MockEndpoint();
                    m.setMockCode("ep-" + RandomUtil.uuidShort(8));
                    String name = (ep.summary != null && !ep.summary.isEmpty())
                            ? ep.summary : (ep.operationId != null ? ep.operationId : (ep.method + " " + ep.path));
                    m.setMockName(name);
                    m.setEnvCode(envCode);
                    m.setProjectCode(projectCode);
                    m.setPath(ep.path);
                    m.setMethod(ep.method);
                    m.setMatchUrlPattern(ep.path);
                    m.setResponseTemplate(ep.responseExample != null ? ep.responseExample : defaultResponse);
                    m.setStatusCode(ep.statusCode != null ? ep.statusCode : defaultStatus);
                    m.setPriority(priority);
                    m.setStatus(1);
                    String tags = tagPrefix;
                    if (ep.tags != null && !ep.tags.isEmpty()) {
                        tags += "," + String.join(",", ep.tags);
                    }
                    m.setTags(tags);
                    m.setDescription(ep.description);
                    m.setMatchBody(ep.requestBody);
                    endpointMapper.insert(m);
                    codes.add(m.getMockCode());
                    ok++;
                } catch (Exception ex) {
                    fail++;
                }
            }
            return mapOf(
                    "success", true,
                    "imported", ok,
                    "failed", fail,
                    "mockCodes", codes
            );
        } catch (Exception e) {
            return mapOf("success", false, "message", "Import failed: " + e.getMessage());
        }
    }

    /**
     * 返回内置的 OpenAPI 3 / Swagger 2 / Postman 示例文本，用于前端编辑器或自测。
     *
     * @param format 指定示例格式：{@code openapi3}（默认）、{@code swagger2} 或 {@code postman}
     * @return 包含 success、format、content 的 Map
     */
    @Operation(summary = "获取示例文本")
    @GetMapping("/sample")
    public Object sample(@RequestParam(defaultValue = "openapi3") String format) {
        String openapi3 = "{\n" +
                "  \"openapi\": \"3.0.1\",\n" +
                "  \"info\": { \"title\": \"Sample API\", \"version\": \"1.0.0\" },\n" +
                "  \"paths\": {\n" +
                "    \"/users\": {\n" +
                "      \"get\": {\n" +
                "        \"summary\": \"List users\",\n" +
                "        \"operationId\": \"listUsers\",\n" +
                "        \"tags\": [\"users\"],\n" +
                "        \"responses\": {\n" +
                "          \"200\": { \"description\": \"ok\",\n" +
                "            \"content\": { \"application/json\": { \"example\": {\"code\":0,\"data\":[]} } } }\n" +
                "        }\n" +
                "      },\n" +
                "      \"post\": {\n" +
                "        \"summary\": \"Create user\",\n" +
                "        \"operationId\": \"createUser\",\n" +
                "        \"requestBody\": {\n" +
                "          \"content\": { \"application/json\": { \"example\": {\"name\":\"alice\"} } }\n" +
                "        },\n" +
                "        \"responses\": { \"200\": { \"description\": \"ok\" } }\n" +
                "      }\n" +
                "    },\n" +
                "    \"/users/{id}\": {\n" +
                "      \"get\": {\n" +
                "        \"summary\": \"Get user by id\",\n" +
                "        \"operationId\": \"getUserById\",\n" +
                "        \"parameters\": [{ \"name\": \"id\", \"in\": \"path\", \"required\": true, \"schema\": {\"type\":\"string\"} }],\n" +
                "        \"responses\": { \"200\": { \"description\": \"ok\" } }\n" +
                "      },\n" +
                "      \"delete\": {\n" +
                "        \"summary\": \"Delete user\",\n" +
                "        \"operationId\": \"deleteUser\",\n" +
                "        \"responses\": { \"200\": { \"description\": \"ok\" } }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String swagger2 = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": { \"title\": \"Sample API\", \"version\": \"1.0.0\" },\n" +
                "  \"paths\": {\n" +
                "    \"/pets\": {\n" +
                "      \"get\": { \"summary\": \"List pets\", \"operationId\": \"listPets\", \"responses\": { \"200\": { \"description\": \"ok\" } } }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String postman = "{\n" +
                "  \"info\": { \"name\": \"Sample\", \"_postman_id\": \"abc-123\" },\n" +
                "  \"item\": [\n" +
                "    { \"name\": \"Get user\", \"request\": { \"method\": \"GET\", \"url\": { \"raw\": \"https://api.example.com/users/1\" } } },\n" +
                "    { \"name\": \"Create user\", \"request\": { \"method\": \"POST\",\n" +
                "      \"url\": { \"raw\": \"https://api.example.com/users\" },\n" +
                "      \"header\": [{\"key\":\"Content-Type\",\"value\":\"application/json\"}],\n" +
                "      \"body\": { \"mode\": \"raw\", \"raw\": \"{\\\"name\\\":\\\"bob\\\"}\" } } }\n" +
                "  ]\n" +
                "}";

        switch (format) {
            case "swagger2":
                return mapOf("success", true, "format", "SWAGGER_2", "content", swagger2);
            case "postman":
                return mapOf("success", true, "format", "POSTMAN_21", "content", postman);
            default:
                return mapOf("success", true, "format", "OPENAPI_3", "content", openapi3);
        }
    }
}
