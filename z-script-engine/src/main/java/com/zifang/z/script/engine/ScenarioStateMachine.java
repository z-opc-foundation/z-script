package com.zifang.z.script.engine;

import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.entity.MockScenario;
import com.zifang.z.script.core.domain.entity.MockScenarioState;
import com.zifang.z.script.core.domain.mapper.MockScenarioMapper;
import com.zifang.z.script.core.domain.mapper.MockScenarioStateMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock 场景状态机引擎
 * <p>
 * 场景 (Scenario) = 状态机
 * - 初始状态: scenario.initialState
 * - 状态转换: 当请求匹配到一个 mock endpoint 时:
 * - 如果 endpoint.requiredState 为空，任何状态都可以匹配
 * - 如果 endpoint.requiredState == 当前状态，才能匹配
 * - 如果 endpoint.newState 不为空，匹配后切换到 newState
 * <p>
 * 实例 (Instance) = 一次会话/用户的当前状态
 * - 通过 instanceKey (通常是 sessionId 或 traceId) 区分
 * - 状态保存在 z_mock_scenario_state 表
 */
@Component
public class ScenarioStateMachine {

    private static final Logger log = LogManager.getLogger(ScenarioStateMachine.class);

    @Autowired
    private MockScenarioMapper scenarioMapper;

    @Autowired
    private MockScenarioStateMapper stateMapper;

    /**
     * 检查 endpoint 的场景状态约束
     *
     * @return true 表示当前状态允许匹配
     */
    public boolean checkStateConstraint(MockEndpoint endpoint, String instanceKey) {
        if (endpoint.getScenarioCode() == null || endpoint.getScenarioCode().isEmpty()) {
            return true; // 无场景约束
        }
        if (endpoint.getRequiredState() == null || endpoint.getRequiredState().isEmpty()) {
            return true; // 无状态约束
        }
        String currentState = getCurrentState(endpoint.getScenarioCode(), instanceKey);
        return endpoint.getRequiredState().equals(currentState);
    }

    /**
     * 匹配后切换状态
     */
    public void transitionState(MockEndpoint endpoint, String instanceKey) {
        if (endpoint.getScenarioCode() == null || endpoint.getScenarioCode().isEmpty()) {
            return;
        }
        if (endpoint.getNewState() == null || endpoint.getNewState().isEmpty()) {
            return;
        }
        String newState = endpoint.getNewState();
        MockScenarioState state = new MockScenarioState();
        state.setScenarioCode(endpoint.getScenarioCode());
        state.setInstanceKey(instanceKey);
        state.setCurrentState(newState);
        state.setUpdateTime(new Date());

        MockScenarioState existing = stateMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockScenarioState>()
                        .eq("scenario_code", endpoint.getScenarioCode())
                        .eq("instance_key", instanceKey));

        if (existing == null) {
            state.setCreateTime(new Date());
            // Look up initial state for the scenario
            MockScenario scenario = scenarioMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockScenario>()
                            .eq("scenario_code", endpoint.getScenarioCode()));
            String prevState = scenario != null ? scenario.getInitialState() : "started";
            state.setTransitionLog("[" + new Date() + "] init -> " + newState + " (from " + prevState + ")");
            stateMapper.insert(state);
        } else {
            String prevState = existing.getCurrentState();
            String log = existing.getTransitionLog() == null ? "" : existing.getTransitionLog() + "\n";
            log += "[" + new Date() + "] " + prevState + " -> " + newState;
            existing.setCurrentState(newState);
            existing.setTransitionLog(log);
            existing.setUpdateTime(new Date());
            stateMapper.updateById(existing);
        }
    }

    /**
     * 获取当前状态 (无记录时返回 initialState)
     */
    public String getCurrentState(String scenarioCode, String instanceKey) {
        MockScenarioState state = stateMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockScenarioState>()
                        .eq("scenario_code", scenarioCode)
                        .eq("instance_key", instanceKey));
        if (state != null) {
            return state.getCurrentState();
        }


        MockScenario scenario = scenarioMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockScenario>()
                        .eq("scenario_code", scenarioCode));
        return scenario != null ? scenario.getInitialState() : "started";
    }

    /**
     * 重置场景状态
     */
    public void resetState(String scenarioCode, String instanceKey) {
        stateMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockScenarioState>()
                .eq("scenario_code", scenarioCode)
                .eq("instance_key", instanceKey));
        log.info("Reset scenario state: {}/{}", scenarioCode, instanceKey);
    }

    /**
     * 获取场景所有实例状态 (用于调试)
     */
    public Map<String, String> getAllInstances(String scenarioCode) {
        Map<String, String> result = new HashMap<>();
        for (MockScenarioState s : stateMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MockScenarioState>()
                        .eq("scenario_code", scenarioCode))) {
            result.put(s.getInstanceKey(), s.getCurrentState());
        }
        return result;
    }
}
