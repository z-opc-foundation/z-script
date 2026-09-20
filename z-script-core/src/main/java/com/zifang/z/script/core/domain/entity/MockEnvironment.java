package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

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
@TableName("z_mock_environment")
public class MockEnvironment {

    @TableId(type = IdType.AUTO)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 环境编码
     */
    private String envCode;
    /**
     * 环境名称
     */
    private String envName;
    /**
     * 环境类型 (DEV/TEST/STAGING/PROD)
     */
    private String envType;
    /**
     * 基础 URL
     */
    private String baseUrl;
    /**
     * Mock 优先级 (用于多环境匹配)
     */
    private Integer mockPriority;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 是否为默认环境 (1=是 0=否)
     */
    private Integer isDefault;
    /**
     * 租户编码
     */
    private String tenantCode;
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
