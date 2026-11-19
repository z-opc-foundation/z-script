package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * Mock 环境配置实体 (FEATURE051)
 * <p>
 * 对应表: z_mock_environment
 * 用途: 定义 Mock 环境的基本配置和默认设置
 *
 * @author zifang
 * @since 1.0.0
 */
@BizModel(name = "Mock 环境配置实体", module = "z-script", description = "Mock 环境配置实体 (FEATURE051) 对应表: z_mock_environment 用途: 定义 Mock 环境的基本配置和默认设置")
@TableName("z_mock_environment")
public class MockEnvironment {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 环境编码
     */
    @BizField(name = "环境编码", type = BizField.FieldType.STRING)
    private String envCode;
    /**
     * 环境名称
     */
    @BizField(name = "环境名称", type = BizField.FieldType.STRING)
    private String envName;
    /**
     * 环境类型 (DEV/TEST/STAGING/PROD)
     */
    @BizField(name = "环境类型 (DEV/TEST/STAGING/PROD)", type = BizField.FieldType.STRING)
    private String envType;
    /**
     * 基础 URL
     */
    @BizField(name = "基础 URL", type = BizField.FieldType.STRING)
    private String baseUrl;
    /**
     * Mock 优先级 (用于多环境匹配)
     */
    @BizField(name = "Mock 优先级 (用于多环境匹配)", type = BizField.FieldType.INTEGER)
    private Integer mockPriority;
    /**
     * 描述信息
     */
    @BizField(name = "描述信息", type = BizField.FieldType.STRING)
    private String description;
    /**
     * 是否为默认环境 (1=是 0=否)
     */
    @BizField(name = "是否默认环境", type = BizField.FieldType.INTEGER)
    private Integer isDefault;
    /**
     * 租户编码
     */
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;
    @TableLogic @BizField(name = "逻辑删除: 0=正常, 1=已删", type = BizField.FieldType.INTEGER)
    /**
     * 逻辑删除标识
     */
    private Integer deleted;
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

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String v) {
        this.envCode = v;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String v) {
        this.envName = v;
    }

    public String getEnvType() {
        return envType;
    }

    public void setEnvType(String v) {
        this.envType = v;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String v) {
        this.baseUrl = v;
    }

    public Integer getMockPriority() {
        return mockPriority;
    }

    public void setMockPriority(Integer v) {
        this.mockPriority = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer v) {
        this.isDefault = v;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String v) {
        this.tenantCode = v;
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



