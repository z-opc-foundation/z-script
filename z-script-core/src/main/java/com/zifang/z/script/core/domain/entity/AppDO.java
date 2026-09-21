package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * z-script 应用实体（权限模型的中心）。
 * <p>
 * 对应表: z_script_app
 * 模型: 应用是管理的基本单位 —— API Key 只是应用的凭证（一个应用可签发多把、可随时重置吊销），
 * 应用挂脚本列表决定这批 Key 能调什么。鉴权链: X-Api-Key → ApiKeyDO.appId → AppDO.scope/allowedScripts。
 */
@TableName("z_script_app")
public class AppDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 应用编码（唯一，ApiKeyName 即 appCode）
     */
    private String appCode;

    /**
     * 应用显示名
     */
    private String appName;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 权限范围: ALL=全部脚本 / SPECIFIC=仅 allowed_scripts 列表
     */
    private String scope;

    /**
     * SPECIFIC 模式下的 script_code 列表 (JSON 数组)
     */
    private String allowedScripts;

    /**
     * 1=启用 0=禁用（禁用后该应用名下所有 Key 一并 403）
     */
    private Integer status;

    private String description;

    private Date createTime;

    private Date updateTime;

    private String tenantCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAllowedScripts() {
        return allowedScripts;
    }

    public void setAllowedScripts(String allowedScripts) {
        this.allowedScripts = allowedScripts;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
