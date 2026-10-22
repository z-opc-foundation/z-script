package com.zifang.z.script.web.dto;

import com.zifang.z.script.core.domain.entity.ApiKeyDO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * API Key 管理面出参（脱敏 VO）。
 * <p>
 * <b>为什么不让 Controller 直接吐 ApiKeyDO</b>：DO 上带着 {@code apiKey}（明文）与
 * {@code apiSecretHash}（secret 的 SHA256）。管理面的读端点（{@code /api/script/api-key/page}、
 * {@code /list-enabled}、{@code /{id}}）一旦原样返回 DO，任何能打到这三个端点的调用方就能
 * 把整张密钥表的明文 Key 拿走 —— 拿着别人的 Key 就能以别人的身份调别人的脚本。
 * 哈希虽然不是明文，但落进响应体等于把「拿候选 secret 逐个比对」这个 oracle 交到外网。
 * <p>
 * <b>字段口径</b>：字段名与 {@link ApiKeyDO} 保持一致（前端按 appName / id 取值，
 * e2e 脚本也按这些名字取），差别只在两处：
 * <ul>
 *   <li>{@code apiKey} 只给掩码（{@code zsk_live_abcd…ef01}），不再是可用的凭据；</li>
 *   <li>{@code apiSecretHash} 整个字段不出现在本类里（不是置空，是没有这个字段）。</li>
 * </ul>
 * 掩码保留前缀与首尾各 4 位，是为了让人还能认出「这是哪一把 Key」，而不是让它变成一个 ID。
 *
 * @see com.zifang.z.script.web.support.ApiKeyAccessGuard
 */
@Schema(description = "API Key 管理面出参（脱敏：apiKey 只给掩码，无 api_secret_hash）")
public class ApiKeyManageVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "API Key 标识（掩码，非可用凭据）")
    private String apiKey;

    @Schema(description = "归属应用 id（存量数据可能为 NULL）")
    private Long appId;

    @Schema(description = "归属应用名（归属判定主键）")
    private String appName;

    @Schema(description = "创建人 ID（建 Key 时由请求方自填，非可信身份，仅展示用）")
    private String ownerId;

    @Schema(description = "权限范围 ALL / READ_ONLY / SPECIFIC")
    private String scope;

    @Schema(description = "状态 1=启用 0=禁用")
    private Integer status;

    @Schema(description = "过期时间（NULL=永不过期）")
    private Date expireAt;

    @Schema(description = "最后一次调用时间")
    private Date lastUsedAt;

    @Schema(description = "累计调用次数")
    private Long totalCalls;

    @Schema(description = "备注")
    private String description;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "租户编码")
    private String tenantCode;

    public ApiKeyManageVO() {
    }

    /**
     * 由实体转成脱敏出参。
     *
     * @param src 实体（可为空）
     * @return 脱敏 VO（src 为 null 时返回 null）
     */
    public static ApiKeyManageVO from(ApiKeyDO src) {
        if (src == null) {
            return null;
        }
        ApiKeyManageVO vo = new ApiKeyManageVO();
        vo.setId(src.getId());
        vo.setApiKey(mask(src.getApiKey()));
        vo.setAppId(src.getAppId());
        vo.setAppName(src.getAppName());
        vo.setOwnerId(src.getOwnerId());
        vo.setScope(src.getScope());
        vo.setStatus(src.getStatus());
        vo.setExpireAt(src.getExpireAt());
        vo.setLastUsedAt(src.getLastUsedAt());
        vo.setTotalCalls(src.getTotalCalls());
        vo.setDescription(src.getDescription());
        vo.setCreateTime(src.getCreateTime());
        vo.setTenantCode(src.getTenantCode());
        return vo;
    }

    /**
     * Key 掩码：保留前缀（如 {@code zsk_live_}）与随机段首尾各 4 位，中间用 {@code …} 代替。
     *
     * <p>例：{@code zsk_live_abcdef0123456789abcdef01} → {@code zsk_live_abcd…ef01}。
     * 掩码结果不可用于鉴权（少掉的是随机段中间 16 位）。
     *
     * @param apiKey 明文 Key
     * @return 掩码串（入参为空时返回 null）
     */
    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        int idx = apiKey.lastIndexOf('_');
        String prefix = idx > 0 ? apiKey.substring(0, idx + 1) : "";
        String body = idx > 0 ? apiKey.substring(idx + 1) : apiKey;
        if (body.length() <= 8) {
            // 短 Key：只留首位，避免把整串都泄出去
            return prefix + body.charAt(0) + "…";
        }
        return prefix + body.substring(0, 4) + "…" + body.substring(body.length() - 4);
    }

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

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
