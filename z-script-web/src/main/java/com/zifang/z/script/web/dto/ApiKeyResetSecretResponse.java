package com.zifang.z.script.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 重置 Secret 响应 DTO.
 * <p>
 * 包含重置后的明文 secret 及提示信息.
 */
@Schema(description = "重置 API Key Secret 响应")
public class ApiKeyResetSecretResponse {

    @Schema(description = "API Key ID")
    private Long id;

    @Schema(description = "新明文 Secret（旧 Secret 立即失效）")
    private String plainSecret;

    @Schema(description = "Secret 重置提示")
    private String secretHint;

    public ApiKeyResetSecretResponse() {
    }

    public ApiKeyResetSecretResponse(Long id, String plainSecret) {
        this.id = id;
        this.plainSecret = plainSecret;
        this.secretHint = "⚠️ 新 Secret 已生效, 旧 Secret 立即失效";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlainSecret() {
        return plainSecret;
    }

    public void setPlainSecret(String plainSecret) {
        this.plainSecret = plainSecret;
    }

    public String getSecretHint() {
        return secretHint;
    }

    public void setSecretHint(String secretHint) {
        this.secretHint = secretHint;
    }
}
