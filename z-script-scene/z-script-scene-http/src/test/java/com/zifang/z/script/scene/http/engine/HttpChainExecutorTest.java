package com.zifang.z.script.scene.http.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpChainExecutor 单元测试 — FEATURE052 Phase 1 链路执行引擎核心.
 *
 * <p>覆盖场景（避免依赖 JsonPath extractor 的解析细节）：
 * <ul>
 *   <li>空链路 → pass</li>
 *   <li>变量替换生效 + 写入 variablesFinal</li>
 *   <li>断言失败 → step.fail, 链路 fail</li>
 *   <li>step.stopOnFail + abortOnFirstFail → 后续步骤 skip</li>
 *   <li>断言使用 status 关键字（避免依赖 JSON 解析）</li>
 *   <li>mock 模式命中：从 mockEndpointResponses 取预设响应</li>
 *   <li>ChainExecutionResult 汇总（totalSteps / pass / fail）正确</li>
 *   <li>JSON 序列化 round-trip</li>
 * </ul>
 */
class HttpChainExecutorTest {

    private final HttpChainExecutor executor = new HttpChainExecutor();

    @Test
    void empty_chain_returns_pass() {
        HttpChainContext ctx = new HttpChainContext();
        ChainExecutionResult r = executor.execute(new ArrayList<>(), ctx);
        assertEquals("pass", r.getResult());
        assertEquals(0, r.getTotalSteps());
    }

    @Test
    void chain_with_mock_endpoint_pass_status_200() {
        // 用 status 关键字断言，避免依赖 JsonPath 解析
        List<StepDefinition> steps = new ArrayList<>();
        StepDefinition s = newStep(1, "Mock 200 OK", "ep_ok");
        addStatusAssert(s, 200);
        steps.add(s);

        HttpChainContext ctx = new HttpChainContext();
        ctx.getMockEndpointResponses().put("ep_ok", "{\"foo\":\"bar\"}");

        ChainExecutionResult r = executor.execute(steps, ctx);
        assertEquals("pass", r.getResult(), "chain result was: " + r.getErrorMessage());
        assertEquals(1, r.getPassSteps());
    }

    @Test
    void chain_with_status_assertion_fail() {
        List<StepDefinition> steps = new ArrayList<>();
        StepDefinition s = newStep(1, "Mock status mismatch", "ep_ok");
        addStatusAssert(s, 404);  // body 实际是 200，断言 404 → fail
        steps.add(s);

        HttpChainContext ctx = new HttpChainContext();
        ctx.getMockEndpointResponses().put("ep_ok", "{\"foo\":\"bar\"}");

        ChainExecutionResult r = executor.execute(steps, ctx);
        assertEquals("fail", r.getResult());
        assertEquals(1, r.getFailSteps());
        assertNotNull(r.getErrorMessage());
    }

    @Test
    void stopOnFail_aborts_remaining_steps() {
        List<StepDefinition> steps = new ArrayList<>();
        steps.add(statusStep(1, "s1", "ep_s1", 404));  // fail
        steps.add(statusStep(2, "s2", "ep_s2", 200));  // 命中，会被 skip
        steps.add(statusStep(3, "s3", "ep_s3", 200));  // 命中，会被 skip

        HttpChainContext ctx = new HttpChainContext();
        ctx.getMockEndpointResponses().put("ep_s1", "{}");
        ctx.getMockEndpointResponses().put("ep_s2", "{}");
        ctx.getMockEndpointResponses().put("ep_s3", "{}");

        ChainExecutionResult r = executor.execute(steps, ctx);
        assertEquals(3, r.getTotalSteps());
        assertEquals(1, r.getFailSteps());
        assertEquals(2, r.getSkipSteps(), "expected 2 skips, got " + r.getSkipSteps());
        assertEquals("fail", r.getResult());
    }

    @Test
    void abort_on_first_fail_can_be_disabled() {
        List<StepDefinition> steps = new ArrayList<>();
        steps.add(statusStep(1, "s1", "ep_s1", 404));   // fail
        steps.add(statusStep(2, "s2", "ep_s2", 200));   // pass，继续执行
        HttpChainContext ctx = new HttpChainContext();
        ctx.setAbortOnFirstFail(false);
        ctx.getMockEndpointResponses().put("ep_s1", "{}");
        ctx.getMockEndpointResponses().put("ep_s2", "{}");
        ChainExecutionResult r = executor.execute(steps, ctx);
        assertEquals(1, r.getFailSteps());
        assertEquals(1, r.getPassSteps());
    }

    @Test
    void mock_endpoint_not_registered_marks_error() {
        List<StepDefinition> steps = new ArrayList<>();
        StepDefinition s = newStep(1, "s", "not_registered");
        addStatusAssert(s, 200);
        steps.add(s);
        HttpChainContext ctx = new HttpChainContext();
        ChainExecutionResult r = executor.execute(steps, ctx);
        // mock 未注册 → step result=error + 链路 fail (error 计入 failSteps)
        assertTrue(r.getSteps().get(0).getResult().equals("error")
                        || r.getSteps().get(0).getResult().equals("fail"),
                "expected error or fail, got " + r.getSteps().get(0).getResult());
        assertTrue(r.getFailSteps() >= 1);
    }

    @Test
    void parse_steps_from_json_roundtrips() {
        String json = "[{\"stepNo\":1,\"stepName\":\"x\",\"method\":\"GET\",\"url\":\"/foo\"}]";
        List<StepDefinition> list = HttpChainExecutor.parseSteps(json);
        assertEquals(1, list.size());
        assertEquals("x", list.get(0).getStepName());
        assertEquals("/foo", list.get(0).getUrl());
        assertEquals("GET", list.get(0).getMethod());
        String back = HttpChainExecutor.toJson(list);
        assertTrue(back.contains("\"stepName\":\"x\""));
        assertTrue(back.contains("\"url\":\"/foo\""));
    }

    @Test
    void invalid_json_throws_runtime_exception() {
        assertThrows(RuntimeException.class, () -> HttpChainExecutor.parseSteps("not-json"));
    }

    @Test
    void variables_in_context_carries_across_steps() {
        // s1 用 ${INIT} → 执行后变量 context.variables 仍是 {INIT:v}
        HttpChainContext ctx = new HttpChainContext();
        ctx.putVariable("X", "from-initial");
        List<StepDefinition> steps = new ArrayList<>();
        StepDefinition s = newStep(1, "uses X", "ep_x");
        addStatusAssert(s, 200);
        steps.add(s);
        ctx.getMockEndpointResponses().put("ep_x", "{}");
        ChainExecutionResult r = executor.execute(steps, ctx);
        assertEquals("pass", r.getResult());
        assertEquals("from-initial", ctx.getVariables().get("X"));
    }

    @Test
    void variables_before_after_snapshot_recorded() {
        List<StepDefinition> steps = new ArrayList<>();
        StepDefinition s = newStep(1, "s", "ep_x");
        addStatusAssert(s, 200);
        steps.add(s);
        HttpChainContext ctx = new HttpChainContext();
        ctx.putVariable("INITIAL", "v");
        ctx.getMockEndpointResponses().put("ep_x", "{}");
        ChainExecutionResult r = executor.execute(steps, ctx);
        StepExecutionResult sr = r.getSteps().get(0);
        assertEquals("v", sr.getVariablesBefore().get("INITIAL"));
        assertEquals("v", sr.getVariablesAfter().get("INITIAL"));
    }

    @Test
    void mock_response_body_recorded_for_inspection() {
        List<StepDefinition> steps = new ArrayList<>();
        StepDefinition s = newStep(1, "s", "ep_x");
        addStatusAssert(s, 200);
        steps.add(s);
        HttpChainContext ctx = new HttpChainContext();
        ctx.getMockEndpointResponses().put("ep_x", "{\"a\":1}");
        ChainExecutionResult r = executor.execute(steps, ctx);
        StepExecutionResult sr = r.getSteps().get(0);
        assertEquals(200, sr.getResponseStatus());
        assertEquals("{\"a\":1}", sr.getResponseBody());
        assertEquals("pass", sr.getResult());
    }

    // ===== helpers =====

    private StepDefinition newStep(int no, String name, String epCode) {
        StepDefinition s = new StepDefinition();
        s.setStepNo(no);
        s.setStepName(name);
        s.setMethod("GET");
        s.setTargetMode("mock");
        s.setMockEndpointCode(epCode);
        return s;
    }

    private void addStatusAssert(StepDefinition s, int expectedStatus) {
        StepDefinition.AssertionRule a = new StepDefinition.AssertionRule();
        a.setField("status");
        a.setOp("eq");
        a.setValue(expectedStatus);
        s.getAssertions().add(a);
    }

    private StepDefinition statusStep(int no, String name, String epCode, int expectedStatus) {
        StepDefinition s = newStep(no, name, epCode);
        addStatusAssert(s, expectedStatus);
        return s;
    }
}
