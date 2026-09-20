package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.entity.MockEnvironment;
import com.zifang.z.script.core.domain.entity.MockScenario;
import com.zifang.z.script.core.domain.entity.MockTestCase;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.core.domain.mapper.MockEnvironmentMapper;
import com.zifang.z.script.core.domain.mapper.MockScenarioMapper;
import com.zifang.z.script.core.domain.mapper.MockTestCaseMapper;
import com.zifang.z.script.engine.ScenarioStateMachine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 平台综合 API。
 * <p>
 * API 基础路径: /api/mock-platform
 * 所属模块: z-script-web
 * 鉴权: 暂未接入统一鉴权
 * <p>
 * 提供 Mock 全功能操作的统一入口:
 * <ul>
 *   <li>/endpoints     — Mock 端点 CRUD</li>
 *   <li>/environments  — 环境管理</li>
 *   <li>/scenarios     — 场景 CRUD + 状态重置</li>
 *   <li>/cases         — 测试用例 CRUD</li>
 *   <li>/stats         — 统计数据</li>
 * </ul>
 */
@Tag(name = "Mock平台综合API")
@RestController
@RequestMapping("/api/mock-platform")
public class MockPlatformController {

    @Autowired
    private MockEndpointMapper endpointMapper;
    @Autowired
    private MockEnvironmentMapper environmentMapper;
    @Autowired
    private MockScenarioMapper scenarioMapper;
    @Autowired
    private MockTestCaseMapper testCaseMapper;
    @Autowired
    private ScenarioStateMachine scenarioStateMachine;

    // ==================== Endpoints ====================

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
     * 查询 Mock 端点列表，可按环境、场景、状态过滤。
     *
     * @param envCode      环境编码，可选
     * @param scenarioCode 场景编码，可选
     * @param status       状态值，可选
     * @return 符合条件的 Mock 端点列表
     */
    @Operation(summary = "查询列表")
    @GetMapping("/endpoints/list")
    public Object listEndpoints(@RequestParam(required = false) String envCode,
                                @RequestParam(required = false) String scenarioCode,
                                @RequestParam(required = false) String status) {
        QueryWrapper<MockEndpoint> qw = new QueryWrapper<>();
        if (envCode != null && !envCode.isEmpty()) {
            qw.eq("env_code", envCode);
        }

        if (scenarioCode != null && !scenarioCode.isEmpty()) {
            qw.eq("scenario_code", scenarioCode);
        }

        if (status != null && !status.isEmpty()) {
            qw.eq("status", status);
        }

        qw.orderByAsc("priority").orderByDesc("update_time");
        return endpointMapper.selectList(qw);
    }

    /**
     * 按 mockCode 查询 Mock 端点详情。
     *
     * @param mockCode 端点编码
     * @return 命中的 Mock 端点实体；不存在时返回 null
     */
    @Operation(summary = "查询endpoints")
    @GetMapping("/endpoints/byCode")
    public Object getEndpoint(@RequestParam String mockCode) {
        return endpointMapper.selectOne(new QueryWrapper<MockEndpoint>().eq("mock_code", mockCode));
    }

    /**
     * 创建 Mock 端点。对 status、priority、method 缺省值进行兜底。
     *
     * @param endpoint 待创建的 Mock 端点实体
     * @return 包含 success 与新生成 id 的结果 Map
     */
    @Operation(summary = "创建endpoints")
    @PostMapping("/endpoints")
    public Object createEndpoint(@RequestBody MockEndpoint endpoint) {
        endpoint.setId(null);
        if (endpoint.getStatus() == null) {
            endpoint.setStatus(1);
        }

        if (endpoint.getPriority() == null) {
            endpoint.setPriority(5);
        }

        if (endpoint.getMethod() == null) {
            endpoint.setMethod("GET");
        }

        endpointMapper.insert(endpoint);
        return mapOf("success", true, "id", endpoint.getId());
    }

    /**
     * 按 mockCode 更新 Mock 端点。
     *
     * @param mockCode 目标 Mock 端点编码
     * @param endpoint 新的 Mock 端点内容
     * @return success=true 表示更新成功；success=false 表示端点不存在
     */
    @Operation(summary = "更新endpoints")
    @PutMapping("/endpoints")
    public Object updateEndpoint(@RequestParam String mockCode, @RequestBody MockEndpoint endpoint) {
        MockEndpoint existing = endpointMapper.selectOne(new QueryWrapper<MockEndpoint>().eq("mock_code", mockCode));
        if (existing == null) {
            return mapOf("success", false, "message", "not found");
        }

        endpoint.setId(existing.getId());
        endpointMapper.updateById(endpoint);
        return mapOf("success", true);
    }

    /**
     * 按 mockCode 删除 Mock 端点。
     *
     * @param mockCode 目标 Mock 端点编码
     * @return success=true 表示删除成功；success=false 表示端点不存在
     */
    @Operation(summary = "删除endpoints")
    @DeleteMapping("/endpoints")
    public Object deleteEndpoint(@RequestParam String mockCode) {
        MockEndpoint existing = endpointMapper.selectOne(new QueryWrapper<MockEndpoint>().eq("mock_code", mockCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        endpointMapper.deleteById(existing.getId());
        return mapOf("success", true);
    }

    // ==================== Environments ====================

    /**
     * 按 mockCode 切换 Mock 端点的启用状态。
     *
     * @param mockCode 目标 Mock 端点编码
     * @param status   目标状态值
     * @return success=true 表示切换成功；success=false 表示端点不存在
     */
    @Operation(summary = "切换启用状态")
    @PostMapping("/endpoints/toggle")
    public Object toggleEndpoint(@RequestParam String mockCode, @RequestParam Integer status) {
        MockEndpoint existing = endpointMapper.selectOne(new QueryWrapper<MockEndpoint>().eq("mock_code", mockCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        existing.setStatus(status);
        endpointMapper.updateById(existing);
        return mapOf("success", true);
    }

    /**
     * 查询全部 Mock 环境列表，按 id 升序。
     *
     * @return Mock 环境列表
     */
    @Operation(summary = "查询列表")
    @GetMapping("/environments/list")
    public Object listEnvironments() {
        return environmentMapper.selectList(new QueryWrapper<MockEnvironment>().orderByAsc("id"));
    }

    /**
     * 创建 Mock 环境。
     *
     * @param env 待创建的环境实体
     * @return 包含 success 与新生成 id 的结果 Map
     */
    @Operation(summary = "创建environments")
    @PostMapping("/environments")
    public Object createEnvironment(@RequestBody MockEnvironment env) {
        env.setId(null);
        environmentMapper.insert(env);
        return mapOf("success", true, "id", env.getId());
    }

    /**
     * 按 envCode 更新 Mock 环境。
     *
     * @param envCode 目标环境编码
     * @param env     新的环境实体
     * @return success=true 表示更新成功；success=false 表示环境不存在
     */
    @Operation(summary = "更新environments")
    @PutMapping("/environments")
    public Object updateEnvironment(@RequestParam String envCode, @RequestBody MockEnvironment env) {
        MockEnvironment existing = environmentMapper.selectOne(new QueryWrapper<MockEnvironment>().eq("env_code", envCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        env.setId(existing.getId());
        environmentMapper.updateById(env);
        return mapOf("success", true);
    }

    // ==================== Scenarios ====================

    /**
     * 按 envCode 删除 Mock 环境。默认环境不可删除。
     *
     * @param envCode 目标环境编码
     * @return success=true 表示删除成功；success=false 表示不存在或不允许删除（默认环境）
     */
    @Operation(summary = "删除environments")
    @DeleteMapping("/environments")
    public Object deleteEnvironment(@RequestParam String envCode) {
        MockEnvironment existing = environmentMapper.selectOne(new QueryWrapper<MockEnvironment>().eq("env_code", envCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        if (existing.getIsDefault() != null && existing.getIsDefault() == 1) {
            return mapOf("success", false, "message", "cannot delete default env");
        }
        environmentMapper.deleteById(existing.getId());
        return mapOf("success", true);
    }

    /**
     * 查询全部 Mock 场景列表，按 id 降序。
     *
     * @return Mock 场景列表
     */
    @Operation(summary = "查询列表")
    @GetMapping("/scenarios/list")
    public Object listScenarios() {
        return scenarioMapper.selectList(new QueryWrapper<MockScenario>().orderByDesc("id"));
    }

    /**
     * 按 scenarioCode 查询 Mock 场景详情。
     *
     * @param scenarioCode 场景编码
     * @return 命中的 Mock 场景实体；不存在时返回 null
     */
    @Operation(summary = "查询scenarios")
    @GetMapping("/scenarios/byCode")
    public Object getScenario(@RequestParam String scenarioCode) {
        return scenarioMapper.selectOne(new QueryWrapper<MockScenario>().eq("scenario_code", scenarioCode));
    }

    /**
     * 创建 Mock 场景，initialState 缺省时为 {@code started}。
     *
     * @param scenario 待创建的 Mock 场景实体
     * @return 包含 success 与新生成 id 的结果 Map
     */
    @Operation(summary = "创建scenarios")
    @PostMapping("/scenarios")
    public Object createScenario(@RequestBody MockScenario scenario) {
        scenario.setId(null);
        if (scenario.getInitialState() == null) {
            scenario.setInitialState("started");
        }

        scenarioMapper.insert(scenario);
        return mapOf("success", true, "id", scenario.getId());
    }

    /**
     * 按 scenarioCode 更新 Mock 场景。
     *
     * @param scenarioCode 目标场景编码
     * @param scenario     新的场景实体
     * @return success=true 表示更新成功；success=false 表示场景不存在
     */
    @Operation(summary = "更新scenarios")
    @PutMapping("/scenarios")
    public Object updateScenario(@RequestParam String scenarioCode, @RequestBody MockScenario scenario) {
        MockScenario existing = scenarioMapper.selectOne(new QueryWrapper<MockScenario>().eq("scenario_code", scenarioCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        scenario.setId(existing.getId());
        scenarioMapper.updateById(scenario);
        return mapOf("success", true);
    }

    /**
     * 按 scenarioCode 删除 Mock 场景。
     *
     * @param scenarioCode 目标场景编码
     * @return success=true 表示删除成功；success=false 表示场景不存在
     */
    @Operation(summary = "删除scenarios")
    @DeleteMapping("/scenarios")
    public Object deleteScenario(@RequestParam String scenarioCode) {
        MockScenario existing = scenarioMapper.selectOne(new QueryWrapper<MockScenario>().eq("scenario_code", scenarioCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        scenarioMapper.deleteById(existing.getId());
        return mapOf("success", true);
    }

    /**
     * 重置场景状态机。{@code instanceKey} 为空时按默认实例重置，否则按指定实例重置。
     *
     * @param scenarioCode 目标场景编码
     * @param instanceKey  实例 key，可选；为空时使用 {@code default}
     * @return success=true 表示重置成功
     */
    @Operation(summary = "重置状态")
    @PostMapping("/scenarios/reset")
    public Object resetScenarioState(@RequestParam String scenarioCode, @RequestParam(required = false) String instanceKey) {
        if (instanceKey == null || instanceKey.isEmpty()) {
            // Reset all instances
            scenarioStateMachine.resetState(scenarioCode, "default");
        } else {
            scenarioStateMachine.resetState(scenarioCode, instanceKey);
        }
        return mapOf("success", true);
    }

    // ==================== Test Cases ====================

    /**
     * 查询场景的所有运行实例（来自状态机）。
     *
     * @param scenarioCode 场景编码
     * @return 实例集合
     */
    @Operation(summary = "查询实例列表")
    @GetMapping("/scenarios/instances")
    public Object getScenarioInstances(@RequestParam String scenarioCode) {
        return scenarioStateMachine.getAllInstances(scenarioCode);
    }

    /**
     * 查询 Mock 测试用例列表，可按 caseGroup、envCode 过滤，按 update_time 降序。
     *
     * @param caseGroup 用例分组，可选
     * @param envCode   环境编码，可选
     * @return Mock 测试用例列表
     */
    @Operation(summary = "查询列表")
    @GetMapping("/cases/list")
    public Object listTestCases(@RequestParam(required = false) String caseGroup,
                                @RequestParam(required = false) String envCode) {
        QueryWrapper<MockTestCase> qw = new QueryWrapper<>();
        if (caseGroup != null && !caseGroup.isEmpty()) {
            qw.eq("case_group", caseGroup);
        }

        if (envCode != null && !envCode.isEmpty()) {
            qw.eq("env_code", envCode);
        }

        qw.orderByDesc("update_time");
        return testCaseMapper.selectList(qw);
    }

    /**
     * 按 caseCode 查询 Mock 测试用例详情。
     *
     * @param caseCode 用例编码
     * @return 命中的 Mock 测试用例实体；不存在时返回 null
     */
    @Operation(summary = "查询cases")
    @GetMapping("/cases/byCode")
    public Object getTestCase(@RequestParam String caseCode) {
        return testCaseMapper.selectOne(new QueryWrapper<MockTestCase>().eq("case_code", caseCode));
    }

    /**
     * 创建 Mock 测试用例，对 status、priority、expectedBodyMatchType 提供缺省值。
     *
     * @param testCase 待创建的测试用例实体
     * @return 包含 success 与新生成 id 的结果 Map
     */
    @Operation(summary = "创建cases")
    @PostMapping("/cases")
    public Object createTestCase(@RequestBody MockTestCase testCase) {
        testCase.setId(null);
        if (testCase.getStatus() == null) {
            testCase.setStatus(1);
        }

        if (testCase.getPriority() == null) {
            testCase.setPriority("P1");
        }

        if (testCase.getExpectedBodyMatchType() == null) {
            testCase.setExpectedBodyMatchType("EXACT");
        }

        testCaseMapper.insert(testCase);
        return mapOf("success", true, "id", testCase.getId());
    }

    /**
     * 按 caseCode 更新 Mock 测试用例。
     *
     * @param caseCode 目标用例编码
     * @param testCase 新的测试用例实体
     * @return success=true 表示更新成功；success=false 表示用例不存在
     */
    @Operation(summary = "更新cases")
    @PutMapping("/cases")
    public Object updateTestCase(@RequestParam String caseCode, @RequestBody MockTestCase testCase) {
        MockTestCase existing = testCaseMapper.selectOne(new QueryWrapper<MockTestCase>().eq("case_code", caseCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        testCase.setId(existing.getId());
        testCaseMapper.updateById(testCase);
        return mapOf("success", true);
    }

    // ==================== Statistics ====================

    /**
     * 按 caseCode 删除 Mock 测试用例。
     *
     * @param caseCode 目标用例编码
     * @return success=true 表示删除成功；success=false 表示用例不存在
     */
    @Operation(summary = "删除cases")
    @DeleteMapping("/cases")
    public Object deleteTestCase(@RequestParam String caseCode) {
        MockTestCase existing = testCaseMapper.selectOne(new QueryWrapper<MockTestCase>().eq("case_code", caseCode));
        if (existing == null) {
            return mapOf("success", false);
        }

        testCaseMapper.deleteById(existing.getId());
        return mapOf("success", true);
    }

    /**
     * 汇总 Mock 平台统计信息：端点/场景/环境/用例总数以及 Top10 命中端点。
     *
     * @return 包含 totalEndpoints、activeEndpoints、totalScenarios、totalEnvironments、totalCases、topHitEndpoints 的 Map
     */
    @Operation(summary = "查询统计数据")
    @GetMapping("/stats")
    public Object stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEndpoints", endpointMapper.selectCount(null));
        stats.put("activeEndpoints", endpointMapper.selectCount(new QueryWrapper<MockEndpoint>().eq("status", 1)));
        stats.put("totalScenarios", scenarioMapper.selectCount(null));
        stats.put("totalEnvironments", environmentMapper.selectCount(null));
        stats.put("totalCases", testCaseMapper.selectCount(null));
        // Top 10 hit endpoints
        List<MockEndpoint> topHits = endpointMapper.selectList(
                new QueryWrapper<MockEndpoint>().orderByDesc("hit_count").last("LIMIT 10"));
        stats.put("topHitEndpoints", topHits);
        return stats;
    }
}
