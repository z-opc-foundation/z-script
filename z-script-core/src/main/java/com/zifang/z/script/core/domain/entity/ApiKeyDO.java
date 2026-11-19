package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * z-script API Key 实体 (FEATURE051)
 * <p>
 * 对应表: z_script_api_key
 * 用途: 调用方鉴权凭证, secret 仅存 SHA256 哈希不存明文
 */
@BizModel(name = "z-script API Key 实体", module = "z-script", description = "z-script API Key 实体 (FEATURE051) 对应表: z_script_api_key 用途: 调用方鉴权凭证, secret 仅存 SHA256 哈希不存明文")
@TableName("z_script_api_key")
public class ApiKeyDO {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * API Key 标识 (zsk_live_xxx)
     */
    @BizField(name = "API Key 标识 (zsk_live_xxx)", type = BizField.FieldType.STRING)
    private String apiKey;

    /**
     * 归属应用 id（权限模型的中心：能调什么由应用决定，Key 只负责认证）
     */
    @TableField("app_id")
    @BizField(name = "归属应用 id（权限模型的中心：能调什么由应用决定，Key 只负责认证）", type = BizField.FieldType.INTEGER)
    private Long appId;

    /**
     * Secret 的 SHA256 哈希 (不存明文)
     */
    @BizField(name = "Secret 的 SHA256 哈希 (不存明文)", type = BizField.FieldType.STRING)
    private String apiSecretHash;

    /**
     * 调用方应用名
     */
    @BizField(name = "调用方应用名", type = BizField.FieldType.STRING)
    private String appName;

    /**
     * 创建人 ID
     */
    @BizField(name = "创建人 ID", type = BizField.FieldType.STRING)
    private String ownerId;

    /**
     * 权限范围: ALL / READ_ONLY / SPECIFIC
     */
    @TableField("scope")
    @BizField(name = "权限范围: ALL|READ_ONLY|SPECIFIC", type = BizField.FieldType.STRING)
    private String scope;

    /**
     * FEATURE051 - t17 重试配置
     */
    @TableField("max_retry")
    @BizField(name = "FEATURE051 - t17 重试配置", type = BizField.FieldType.INTEGER)
    private Integer maxRetry;

    @TableField("retry_backoff_ms")
    @BizField(name = "重试基础退避毫秒 (指数退避)", type = BizField.FieldType.INTEGER)
    private Integer retryBackoffMs;

    @TableField("retry_on")
    @BizField(name = "重试条件: ALL|TIMEOUT|HTTP_5XX|NONE", type = BizField.FieldType.STRING)
    private String retryOn;

    @TableField("circuit_breaker_threshold")
    @BizField(name = "熔断连续失败阈值 (0=不熔断)", type = BizField.FieldType.INTEGER)
    private Integer circuitBreakerThreshold;

    /**
     * SPECIFIC 模式下的 script_code 列表 (JSON)
     */
    @BizField(name = "SPECIFIC 模式下的 script_code 列表 (JSON)", type = BizField.FieldType.STRING)
    private String allowedScripts;

    /**
     * IP 白名单 (JSON 数组, 支持 CIDR)
     */
    @BizField(name = "IP 白名单 (JSON 数组, 支持 CIDR)", type = BizField.FieldType.STRING)
    private String ipWhitelist;

    /**
     * 1=启用 0=禁用
     */
    @BizField(name = "1=启用 0=禁用", type = BizField.FieldType.INTEGER)
    private Integer status;

    /**
     * 过期时间 (NULL=永不过期)
     */
    @BizField(name = "过期时间 (NULL=永不过期)", type = BizField.FieldType.DATETIME)
    private Date expireAt;

    /**
     * 最后一次调用时间
     */
    @BizField(name = "最后一次调用时间", type = BizField.FieldType.DATETIME)
    private Date lastUsedAt;

    /**
     * 累计调用次数
     */
    @BizField(name = "累计调用次数", type = BizField.FieldType.INTEGER)
    private Long totalCalls;

    /**
     * 备注
     */
    @BizField(name = "备注", type = BizField.FieldType.STRING)
    private String description;

    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;

    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private Date updateTime;

    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    // ========== Getter / Setter ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getApiSecretHash() {
        return apiSecretHash;
    }

    public void setApiSecretHash(String apiSecretHash) {
        this.apiSecretHash = apiSecretHash;
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

    public Integer getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(Integer maxRetry) {
        this.maxRetry = maxRetry;
    }

    public Integer getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(Integer retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public String getRetryOn() {
        return retryOn;
    }

    public void setRetryOn(String retryOn) {
        this.retryOn = retryOn;
    }

    public Integer getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }

    public void setCircuitBreakerThreshold(Integer circuitBreakerThreshold) {
        this.circuitBreakerThreshold = circuitBreakerThreshold;
    }

    public String getAllowedScripts() {
        return allowedScripts;
    }

    public void setAllowedScripts(String allowedScripts) {
        this.allowedScripts = allowedScripts;
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Date expireAt) {
        this.expireAt = expireAt;
    }

    public Date getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Date lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(Long totalCalls) {
        this.totalCalls = totalCalls;
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



