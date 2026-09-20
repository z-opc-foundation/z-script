package com.zifang.z.script.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * 创建 API Key 响应 DTO.
 * <p>
 * 包含新建的 apiKey、仅返回一次的明文 secret 及元信息.
 */
@Schema(description = "创建 API Key 响应")
public class ApiKeyCreateResponse {

    @Schema(description = "API Key 标识")
    private String apiKey;

    @Schema(description = "明文 Secret（仅返回 1 次，需立即保存）")
    private String plainSecret;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "权限范围")
    private String scope;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "Secret 保存提示")
    private String secretHint;

    public ApiKeyCreateResponse() {
    }

    public ApiKeyCreateResponse(String apiKey, String plainSecret, String appName,
                                Integer status, String scope, Date createTime) {
        this.apiKey = apiKey;
        this.plainSecret = plainSecret;
        this.appName = appName;
        this.status = status;
        this.scope = scope;
        this.createTime = createTime;
        this.secretHint = "⚠️ 明文 Secret 仅返回 1 次, 请立即保存到安全位置 (推荐 Bitwarden / 1Password / Vault)";
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPlainSecret() {
        return plainSecret;
    }

    public void setPlainSecret(String plainSecret) {
        this.plainSecret = plainSecret;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getSecretHint() {
        return secretHint;
    }

    public void setSecretHint(String secretHint) {
        this.secretHint = secretHint;
    }
}
