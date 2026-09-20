package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * z-script API Key 实体 (FEATURE051)
 * <p>
 * 对应表: z_script_api_key
 * 用途: 调用方鉴权凭证, secret 仅存 SHA256 哈希不存明文
 */
@TableName("z_script_api_key")
public class ApiKeyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * API Key 标识 (zsk_live_xxx)
     */
    private String apiKey;

    /**
     * Secret 的 SHA256 哈希 (不存明文)
     */
    private String apiSecretHash;

    /**
     * 调用方应用名
     */
    private String appName;

    /**
     * 创建人 ID
     */
    private String ownerId;

    /**
     * 权限范围: ALL / READ_ONLY / SPECIFIC
     */
    @TableField("scope")
    private String scope;

    /**
     * FEATURE051 - t17 重试配置
     */
    @TableField("max_retry")
    private Integer maxRetry;

    @TableField("retry_backoff_ms")
    private Integer retryBackoffMs;

    @TableField("retry_on")
    private String retryOn;

    @TableField("circuit_breaker_threshold")
    private Integer circuitBreakerThreshold;

    /**
     * SPECIFIC 模式下的 script_code 列表 (JSON)
     */
    private String allowedScripts;

    /**
     * IP 白名单 (JSON 数组, 支持 CIDR)
     */
    private String ipWhitelist;

    /**
     * 1=启用 0=禁用
     */
    private Integer status;

    /**
     * 过期时间 (NULL=永不过期)
     */
    private Date expireAt;

    /**
     * 最后一次调用时间
     */
    private Date lastUsedAt;

    /**
     * 累计调用次数
     */
    private Long totalCalls;

    /**
     * 备注
     */
    private String description;

    private Date createTime;

    private Date updateTime;

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
