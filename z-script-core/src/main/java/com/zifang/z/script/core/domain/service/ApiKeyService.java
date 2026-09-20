package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.ApiKeyDO;

import java.util.List;

/**
 * z-script API Key 服务接口 (FEATURE051)
 */
public interface ApiKeyService extends IService<ApiKeyDO> {

    /**
     * 通过 api_key 查询
     */
    ApiKeyDO getByApiKey(String apiKey);

    /**
     * 创建 API Key (自动生成 apiKey + apiSecret, 返回明文 secret 仅 1 次)
     *
     * @return [ApiKeyDO, plainSecret]
     */
    ApiKeyDO createApiKey(String appName, String ownerId, String scope, String description);

    /**
     * 重置 Secret
     *
     * @return 新的明文 Secret
     */
    String resetSecret(Long id);

    /**
     * 校验请求签名 (HMAC-SHA256)
     *
     * @return true 通过; false 失败
     */
    boolean verifySignature(ApiKeyDO apiKey, String method, String path, String timestamp, String body, String signature);

    /**
     * 校验 IP 白名单
     */
    boolean verifyIpWhitelist(ApiKeyDO apiKey, String remoteIp);

    /**
     * 校验 scope (ALL / READ_ONLY / SPECIFIC)
     */
    boolean verifyScope(ApiKeyDO apiKey, String scriptCode);

    /**
     * 记录一次调用 (异步更新 last_used_at + total_calls)
     */
    void recordCall(Long id);

    /**
     * 列出所有启用的 Key
     */
    List<ApiKeyDO> listEnabled();

    /**
     * 禁用 Key
     */
    boolean disable(Long id);

    /**
     * 启用 Key
     */
    boolean enable(Long id);
}
