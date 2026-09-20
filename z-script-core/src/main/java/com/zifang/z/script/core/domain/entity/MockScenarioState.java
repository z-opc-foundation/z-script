package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Mock 场景状态实体 (FEATURE051)
 * <p>
 * 对应表: z_mock_scenario_state
 * 用途: 记录 Mock 场景的当前状态和状态转换日志
 *
 * @author zifang
 * @since 1.0.0
 */
@TableName("z_mock_scenario_state")
public class MockScenarioState {

    @TableId(type = IdType.AUTO)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 场景编码
     */
    private String scenarioCode;
    /**
     * 场景实例唯一键 (用于区分同一场景的不同实例)
     */
    private String instanceKey;
    /**
     * 当前状态名称
     */
    private String currentState;
    /**
     * 状态转换日志 (JSON 格式记录转换过程)
     */
    private String transitionLog;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String v) {
        this.scenarioCode = v;
    }

    public String getInstanceKey() {
        return instanceKey;
    }

    public void setInstanceKey(String v) {
        this.instanceKey = v;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String v) {
        this.currentState = v;
    }

    public String getTransitionLog() {
        return transitionLog;
    }

    public void setTransitionLog(String v) {
        this.transitionLog = v;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date v) {
        this.createTime = v;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date v) {
        this.updateTime = v;
    }
}
