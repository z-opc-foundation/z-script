package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

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
@BizModel(name = "Mock 场景状态实体", module = "z-script", description = "Mock 场景状态实体 (FEATURE051) 对应表: z_mock_scenario_state 用途: 记录 Mock 场景的当前状态和状态转换日志")
@TableName("z_mock_scenario_state")
public class MockScenarioState {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 场景编码
     */
    @BizField(name = "场景编码", type = BizField.FieldType.STRING)
    private String scenarioCode;
    /**
     * 场景实例唯一键 (用于区分同一场景的不同实例)
     */
    @BizField(name = "场景实例唯一键 (用于区分同一场景的不同实例)", type = BizField.FieldType.STRING)
    private String instanceKey;
    /**
     * 当前状态名称
     */
    @BizField(name = "当前状态名称", type = BizField.FieldType.STRING)
    private String currentState;
    /**
     * 状态转换日志 (JSON 格式记录转换过程)
     */
    @BizField(name = "状态转换日志 (JSON 格式记录转换过程)", type = BizField.FieldType.STRING)
    private String transitionLog;
    /**
     * 创建时间
     */
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;
    /**
     * 更新时间
     */
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
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



