package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.script.core.domain.entity.MockTestCase;
import com.zifang.z.script.core.domain.mapper.MockTestCaseMapper;
import com.zifang.z.script.engine.ApiExecutionResult;
import com.zifang.z.script.engine.AssertionEngine;
import com.zifang.z.script.engine.DynamicApiDefinition;
import com.zifang.z.script.engine.DynamicApiExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Mock 测试用例执行器。
 * <p>
 * API 基础路径: /api/mock-platform/cases
 * 所属模块: z-script-web
 * 鉴权: 暂未接入统一鉴权
 * <p>
 * 重构后：HTTP 发送统一委托 {@link DynamicApiExecutor}（OkHttp + SSE + 异步），
 * 享受连接池、灵活超时、Bearer/Basic Auth。
 * <p>
 * 主要端点:
 * <ul>
 *   <li>POST /run?caseCode={code}  — 执行单个测试用例</li>
 *   <li>POST /run-batch             — 批量执行测试用例</li>
 * </ul>
 */
@Tag(name = "Mock-测试用例执行")
@RestController
@RequestMapping("/api/mock-platform/cases")
public class MockTestCaseRunnerController {

    @Autowired
    private MockTestCaseMapper testCaseMapper;
    @Autowired
    private AssertionEngine assertionEngine;
    @Autowired
    private DynamicApiExecutor executor;

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

    /**
     * 执行单个 MockTestCase。
     *
     * @param caseCode 测试用例编码
     * @return 包含 status、actualStatus、responseBody、responseHeaders、durationMs、error、errorType、assertionResults 等字段的结果 Map；用例不存在时返回 success=false 与提示信息
     */
    @Operation(summary = "执行单个测试用例")
    @PostMapping("/run")
    public Object runCase(@RequestParam String caseCode) {
        MockTestCase testCase = testCaseMapper.selectOne(
                new QueryWrapper<MockTestCase>().eq("case_code", caseCode));
        if (testCase == null) {
            return mapOf("success", false, "message", "Case not found");
        }
        return runSingleCase(testCase);
    }

    /**
     * 批量执行多个测试用例。请求体需包含 {@code caseCodes} 列表。
     *
     * @param body 请求体，包含 key {@code caseCodes}
     * @return 包含 total、pass、fail 与每个用例结果列表；caseCodes 为空时返回 success=false 与提示信息
     */
    @PostMapping("/run-batch")
    @Operation(summary = "创建run-batch")
    public Object runBatch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> caseCodes = (List<String>) body.get("caseCodes");
        if (caseCodes == null || caseCodes.isEmpty()) {
            return mapOf("success", false, "message", "caseCodes required");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        int pass = 0, fail = 0;
        for (String code : caseCodes) {
            MockTestCase tc = testCaseMapper.selectOne(
                    new QueryWrapper<MockTestCase>().eq("case_code", code));
            if (tc == null) {
                Map<String, Object> r = new HashMap<>();
                r.put("caseCode", code);
                r.put("status", "SKIP");
                r.put("message", "not found");
                results.add(r);
                fail++;
                continue;
            }
            Map<String, Object> result = runSingleCase(tc);
            results.add(result);
            if ("PASS".equals(result.get("status"))) {
                pass++;
            } else fail++;
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("total", caseCodes.size());
        resp.put("pass", pass);
        resp.put("fail", fail);
        resp.put("results", results);
        return resp;
    }

    /**
     * 执行单个测试用例：构造请求、调用 DynamicApiExecutor、跑断言、更新统计。
     *
     * @param tc 测试用例实体
     * @return 包含 status、actualStatus、responseBody、responseHeaders、durationMs、error、errorType、assertionResults 的结果 Map
     */
    private Map<String, Object> runSingleCase(MockTestCase tc) {
        Map<String, Object> result = new HashMap<>();
        result.put("caseCode", tc.getCaseCode());
        result.put("caseName", tc.getCaseName());
        try {
            // 构造 DynamicApiDefinition
            String url = tc.getRequestUrl();
            if (tc.getRequestQuery() != null && !tc.getRequestQuery().isEmpty()) {
                url += (url.contains("?") ? "&" : "?") + tc.getRequestQuery();
            }
            String fullUrl = "http://localhost:" + System.getProperty("server.port", "8080") + url;

            DynamicApiDefinition def = DynamicApiDefinition.of(
                    tc.getRequestMethod() == null ? "GET" : tc.getRequestMethod(),
                    fullUrl);
            if (tc.getRequestHeaders() != null) {
                Map<String, String> headers = JsonUtil.fromJson(tc.getRequestHeaders(), Map.class);
                if (headers != null) {
                    def.getHeaders().putAll(headers);
                }

            }
            if (tc.getRequestBody() != null && !tc.getRequestBody().isEmpty()) {
                def.setBody(tc.getRequestBody());
            }
            def.setSource("TESTCASE");
            def.setCode(tc.getCaseCode());

            // 委托执行
            ApiExecutionResult r = executor.execute(def);
            long duration = r.getDurationMs();

            // Run assertions
            List<Map<String, Object>> assertions = parseAssertions(tc.getAssertions());
            List<AssertionEngine.AssertionResult> assertionResults = assertionEngine.assertAll(
                    assertions, r.getStatus(), r.getHeaders(), r.getBody(), duration);

            boolean allPassed = assertionResults.stream().allMatch(AssertionEngine.AssertionResult::isPassed);
            if (assertionResults.isEmpty() && tc.getExpectedStatus() != null) {
                allPassed = (r.getStatus() == tc.getExpectedStatus());
            }

            result.put("status", allPassed ? "PASS" : "FAIL");
            result.put("actualStatus", r.getStatus());
            result.put("responseBody", r.getBody());
            result.put("responseHeaders", r.getHeaders());
            result.put("durationMs", duration);
            result.put("error", r.getError());
            result.put("errorType", r.getErrorType());
            result.put("assertionResults", assertionResults);

            // Update case stats
            updateCaseStats(tc, allPassed ? "PASS" : "FAIL");
        } catch (Exception e) {
            result.put("status", "FAIL");
            result.put("error", e.getMessage());
            updateCaseStats(tc, "FAIL");
        }
        return result;
    }

    /**
     * 将 JSON 字符串解析为断言规则列表。
     *
     * @param json 断言 JSON
     * @return 断言规则列表；解析失败或入参为空时返回空列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAssertions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return JsonUtil.fromJson(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 更新测试用例的运行统计：runCount、passCount/failCount、lastRunTime、lastRunResult。
     *
     * @param tc     测试用例实体
     * @param status 执行结果状态，{@code PASS} 或其他
     */
    private void updateCaseStats(MockTestCase tc, String status) {
        try {
            MockTestCase update = new MockTestCase();
            update.setId(tc.getId());
            update.setRunCount((tc.getRunCount() == null ? 0 : tc.getRunCount()) + 1);
            if ("PASS".equals(status)) {
                update.setPassCount((tc.getPassCount() == null ? 0 : tc.getPassCount()) + 1);
            } else {
                update.setFailCount((tc.getFailCount() == null ? 0 : tc.getFailCount()) + 1);
            }
            update.setLastRunTime(new Date());
            update.setLastRunResult(status);
            testCaseMapper.updateById(update);
        } catch (Exception ignored) {
        }
    }
}
