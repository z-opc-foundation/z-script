package com.zifang.z.script.web.controller;

import com.zifang.util.core.lang.RandomUtil;
import com.zifang.util.http.base.pojo.HttpRequestBody;
import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.http.base.pojo.HttpRequestHeader;
import com.zifang.util.http.base.pojo.HttpRequestLine;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.http.parser.curl.CurlBuilder;
import com.zifang.util.http.parser.curl.CurlParser;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.entity.MockTestCase;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.core.domain.mapper.MockTestCaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * curl 一键导入 / 一键运行 / 一键导出
 * <p>
 * 端点:
 * POST /api/mock-platform/curl/parse              - 只解析（不执行、不入库）
 * POST /api/mock-platform/curl/run                - 解析后直接执行（用于 ad-hoc 测试）
 * POST /api/mock-platform/curl/import-as-case     - 解析后入库为 MockTestCase
 * POST /api/mock-platform/curl/import-as-endpoint - 解析后入库为 MockEndpoint
 * POST /api/mock-platform/curl/export             - MockTestCase → curl 命令
 * <p>
 * 全部基于 z-util-http 的 CurlParser / CurlBuilder / HttpExecutor，业务零实现。
 */
@Tag(name = "Mock-curl导入导出")
@RestController
@RequestMapping("/api/mock-platform/curl")
public class MockCurlController {

    private final HttpExecutor httpExecutor = HttpExecutor.getDefault();
    @Autowired
    private MockTestCaseMapper testCaseMapper;
    @Autowired
    private MockEndpointMapper endpointMapper;

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
    // 1. 解析（不执行、不入库）
    // =================================================================

    /**
     * 仅解析 curl 字符串，不执行、不入库。
     *
     * @param body 请求体，包含 key {@code curl}
     * @return 包含 success、data(method/url/path/query/headers/body) 的结果 Map；解析失败时返回 success=false 与错误信息
     */
    @Operation(summary = "解析curl命令")
    @PostMapping("/parse")
    public Object parse(@RequestBody Map<String, String> body) {
        String curl = body.get("curl");
        if (curl == null || curl.trim().isEmpty()) {
            return mapOf("success", false, "message", "curl is required");
        }
        try {
            HttpRequestDefinition def = CurlParser.parse(curl);
            return mapOf("success", true, "data", toParseResult(def));
        } catch (Exception e) {
            return mapOf("success", false, "message", "Parse failed: " + e.getMessage());
        }
    }

    // =================================================================
    // 2. 直接执行 curl
    // =================================================================

    /**
     * 解析 curl 后立即执行，适用于 ad-hoc 测试场景。
     *
     * @param body 请求体，包含 key {@code curl}
     * @return 包含 success、status、headers、body、durationMs、bodySize、error 的执行结果 Map
     */
    @Operation(summary = "解析并执行curl")
    @PostMapping("/run")
    public Object run(@RequestBody Map<String, String> body) {
        String curl = body.get("curl");
        if (curl == null || curl.trim().isEmpty()) {
            return mapOf("success", false, "message", "curl is required");
        }
        HttpExecutionResult r = httpExecutor.executeByCurl(curl);
        return mapOf(
                "success", r.isSuccess(),
                "status", r.getStatus(),
                "headers", r.getHeaders(),
                "body", r.getBody(),
                "durationMs", r.getDurationMs(),
                "bodySize", r.getBodySize(),
                "error", r.getError() == null ? "" : r.getError()
        );
    }

    // =================================================================
    // 3. 导入为测试用例
    // =================================================================

    /**
     * 解析 curl 后将其入库为 MockTestCase。
     *
     * @param body 请求体，包含 {@code curl}，可选 {@code caseName}、{@code caseGroup}、{@code envCode}
     * @return 成功时返回 success=true、caseCode 与 id；失败时返回 success=false 与错误信息
     */
    @Operation(summary = "导入为测试用例")
    @PostMapping("/import-as-case")
    public Object importAsCase(@RequestBody Map<String, Object> body) {
        String curl = (String) body.get("curl");
        if (curl == null || curl.trim().isEmpty()) {
            return mapOf("success", false, "message", "curl is required");
        }
        try {
            HttpRequestDefinition def = CurlParser.parse(curl);
            String caseName = (String) body.getOrDefault("caseName", "from-curl-" + System.currentTimeMillis());
            String caseGroup = (String) body.getOrDefault("caseGroup", "imported");
            String envCode = (String) body.getOrDefault("envCode", "default");

            MockTestCase tc = new MockTestCase();
            tc.setCaseCode("case-" + RandomUtil.uuidShort(8));
            tc.setCaseName(caseName);
            tc.setCaseGroup(caseGroup);
            tc.setEnvCode(envCode);
            tc.setRequestMethod(def.getHttpRequestLine() == null || def.getHttpRequestLine().getRequestMethod() == null
                    ? "GET" : def.getHttpRequestLine().getRequestMethod().name());
            tc.setRequestUrl(extractPathAndQuery(def.getHttpRequestLine() == null ? null : def.getHttpRequestLine().getUrl()));
            tc.setRequestHeaders(headersToJson(def.getHttpRequestHeader()));
            tc.setRequestBody(bodyToString(def.getHttpRequestBody()));
            tc.setExpectedStatus(200);
            tc.setStatus(1);
            tc.setPriority("P2");
            tc.setTags("curl-import");
            testCaseMapper.insert(tc);
            return mapOf("success", true, "caseCode", tc.getCaseCode(), "id", tc.getId());
        } catch (Exception e) {
            return mapOf("success", false, "message", "Import failed: " + e.getMessage());
        }
    }

    // =================================================================
    // 4. 导入为 Mock 端点
    // =================================================================

    /**
     * 解析 curl 后将其入库为 MockEndpoint。
     *
     * @param body 请求体，包含 {@code curl}，可选 {@code mockName}、{@code envCode}、{@code projectCode}、{@code responseTemplate}
     * @return 成功时返回 success=true、mockCode 与 id；失败时返回 success=false 与错误信息
     */
    @Operation(summary = "导入为Mock端点")
    @PostMapping("/import-as-endpoint")
    public Object importAsEndpoint(@RequestBody Map<String, Object> body) {
        String curl = (String) body.get("curl");
        if (curl == null || curl.trim().isEmpty()) {
            return mapOf("success", false, "message", "curl is required");
        }
        try {
            HttpRequestDefinition def = CurlParser.parse(curl);
            String mockName = (String) body.getOrDefault("mockName", "from-curl-" + System.currentTimeMillis());
            String envCode = (String) body.getOrDefault("envCode", "default");
            String projectCode = (String) body.getOrDefault("projectCode", "default");
            String responseTemplate = (String) body.getOrDefault("responseTemplate", "{\"code\":0,\"data\":{}}");

            String path = def.getHttpRequestLine() == null ? "/" : extractPathAndQuery(def.getHttpRequestLine().getUrl());

            MockEndpoint ep = new MockEndpoint();
            ep.setMockCode("ep-" + RandomUtil.uuidShort(8));
            ep.setMockName(mockName);
            ep.setEnvCode(envCode);
            ep.setProjectCode(projectCode);
            ep.setPath(path);
            ep.setMethod(def.getHttpRequestLine() == null || def.getHttpRequestLine().getRequestMethod() == null
                    ? "GET" : def.getHttpRequestLine().getRequestMethod().name());
            ep.setMatchUrlPattern(path);
            ep.setResponseTemplate(responseTemplate);
            ep.setResponseHeaders(headersToJson(def.getHttpRequestHeader()));
            ep.setStatusCode(200);
            ep.setPriority(5);
            ep.setStatus(1);
            ep.setTags("curl-import");
            endpointMapper.insert(ep);
            return mapOf("success", true, "mockCode", ep.getMockCode(), "id", ep.getId());
        } catch (Exception e) {
            return mapOf("success", false, "message", "Import failed: " + e.getMessage());
        }
    }

    // =================================================================
    // 5. MockTestCase → curl 命令（导出）
    // =================================================================

    /**
     * 将已存在的 MockTestCase 反向生成为 curl 命令字符串。
     *
     * @param body 请求体，包含 {@code caseCode}
     * @return 成功时返回 success=true 与生成的 curl 命令；case 不存在或生成失败时返回 success=false 与错误信息
     */
    @Operation(summary = "导出测试用例为curl")
    @PostMapping("/export")
    public Object export(@RequestBody Map<String, Object> body) {
        String caseCode = (String) body.get("caseCode");
        if (caseCode == null) {
            return mapOf("success", false, "message", "caseCode required");
        }
        MockTestCase tc = testCaseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockTestCase>().eq("case_code", caseCode));
        if (tc == null) {
            return mapOf("success", false, "message", "case not found");
        }
        try {
            String curl = buildCurlFromCase(tc);
            return mapOf("success", true, "curl", curl);
        } catch (Exception e) {
            return mapOf("success", false, "message", "Export failed: " + e.getMessage());
        }
    }

    // =================================================================
    // 内部工具
    // =================================================================

    /**
     * 将 MockTestCase 转换为 HttpRequestDefinition 后调用 CurlBuilder 生成 curl 命令。
     * 若 URL 不是以 http 开头，会自动补上 {@code http://localhost:8888} 前缀。
     *
     * @param tc 测试用例实体
     * @return 生成的 curl 命令字符串
     */
    private String buildCurlFromCase(MockTestCase tc) {
        // 直接用 z-util-http 的 CurlBuilder（标准实现）
        HttpRequestDefinition def = new HttpRequestDefinition();

        HttpRequestLine rl = new HttpRequestLine();
        try {
            rl.setRequestMethod(com.zifang.util.http.base.define.RequestMethod.valueOf(
                    tc.getRequestMethod() == null ? "GET" : tc.getRequestMethod().toUpperCase()));
        } catch (Exception e) {
            rl.setRequestMethod(com.zifang.util.http.base.define.RequestMethod.GET);
        }
        String url = tc.getRequestUrl();
        if (url != null && !url.startsWith("http")) {
            url = "http://localhost:8888" + (url.startsWith("/") ? "" : "/") + url;
        }
        rl.setUrl(url);
        def.setHttpRequestLine(rl);

        if (tc.getRequestHeaders() != null) {
            HttpRequestHeader hh = new HttpRequestHeader();
            try {
                Map<String, String> map = JsonUtil.fromJson(tc.getRequestHeaders(), Map.class);
                if (map != null) {
                    hh.putAll(map);
                }

            } catch (Exception ignored) {
            }
            def.setHttpRequestHeader(hh);
        }
        if (tc.getRequestBody() != null) {
            HttpRequestBody b = new HttpRequestBody();
            b.setBody(tc.getRequestBody().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            def.setHttpRequestBody(b);
        }
        return CurlBuilder.build(def);
    }

    /**
     * 将 HttpRequestDefinition 转换为用于接口展示的 Map 结构。
     * 输出包含 method、url、path、query、headers、body。
     *
     * @param def 解析后的请求定义
     * @return 展示用的 Map 结果
     */
    private Map<String, Object> toParseResult(HttpRequestDefinition def) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (def.getHttpRequestLine() != null) {
            r.put("method", def.getHttpRequestLine().getRequestMethod() == null
                    ? "GET" : def.getHttpRequestLine().getRequestMethod().name());
            r.put("url", def.getHttpRequestLine().getUrl());
            if (def.getHttpRequestLine().getUrl() != null) {
                String u = def.getHttpRequestLine().getUrl();
                int q = u.indexOf('?');
                if (q >= 0) {
                    r.put("path", u.substring(0, q));
                    r.put("query", u.substring(q + 1));
                } else {
                    r.put("path", u);
                    r.put("query", "");
                }
            }
        }
        Map<String, String> headers = new LinkedHashMap<>();
        if (def.getHttpRequestHeader() != null) {
            for (Map.Entry<String, String> e : def.getHttpRequestHeader().entrySet()) {
                headers.put(e.getKey(), e.getValue());
            }
        }
        r.put("headers", headers);
        r.put("body", bodyToString(def.getHttpRequestBody()));
        return r;
    }

    /**
     * 将请求体字节数组按 UTF-8 转换为字符串。
     *
     * @param body HTTP 请求体
     * @return 字符串形式的内容；body 或其字节为空时返回空串
     */
    private String bodyToString(HttpRequestBody body) {
        if (body == null || body.getBody() == null) {
            return "";
        }

        return new String(body.getBody(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 将请求头序列化为 JSON 字符串，便于入库。
     *
     * @param h HTTP 请求头
     * @return JSON 字符串；请求头为 null 或为空时返回 null
     */
    private String headersToJson(HttpRequestHeader h) {
        if (h == null || h.isEmpty()) {
            return null;
        }
        return JsonUtil.toJson(h);
    }

    /**
     * 拆出 path 和 query（从完整 URL）
     */
    private String extractPathAndQuery(String url) {
        if (url == null) {
            return "/";
        }

        // 去掉 host
        int idx = url.indexOf("://");
        if (idx >= 0) {
            int pathStart = url.indexOf('/', idx + 3);
            if (pathStart < 0) {
                return "/";
            }

            return url.substring(pathStart);
        }
        return url.startsWith("/") ? url : "/" + url;
    }
}
