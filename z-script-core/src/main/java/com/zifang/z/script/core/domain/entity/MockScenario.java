package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Mock 场景定义实体 (FEATURE051)
 * <p>
 * 对应表: z_mock_scenario
 * 用途: 定义 Mock 场景的基本信息和状态机配置
 *
 * @author zifang
 * @since 1.0.0
 */
@TableName("z_mock_scenario")
public class MockScenario {

    @TableId(type = IdType.AUTO)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 场景编码 (全局唯一)
     */
    private String scenarioCode;
    /**
     * 场景名称
     */
    private String scenarioName;
    /**
     * 初始状态名称
     */
    private String initialState;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 租户编码
     */
    private String tenantCode;
    /**
     * 创建人 ID
     */
    private String creatorId;
    @TableLogic
    /**
     * 逻辑删除标识
     */
    private Integer deleted;
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

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String v) {
        this.scenarioName = v;
    }

    public String getInitialState() {
        return initialState;
    }

    public void setInitialState(String v) {
        this.initialState = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String v) {
        this.tenantCode = v;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String v) {
        this.creatorId = v;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer v) {
        this.deleted = v;
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
