package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.QuotaDO;

/**
 * z-script 配额服务接口 (FEATURE051)
 */
public interface QuotaService extends IService<QuotaDO> {

    /**
     * 通过 apiKeyId 查询配额
     */
    QuotaDO getByApiKeyId(Long apiKeyId);

    /**
     * 为新创建的 API Key 自动初始化配额
     */
    QuotaDO initForApiKey(Long apiKeyId);

    /**
     * 检查并扣减配额 (滑动窗口)
     *
     * @return null 表示通过; 否则返回 Retry-After 秒数
     */
    Long tryAcquire(Long apiKeyId);

    /**
     * 检查配额是否触发预警
     */
    boolean shouldAlert(QuotaDO quota);

    /**
     * 标记已告警 (防重复告警)
     */
    void markAlerted(Long quotaId);

    /**
     * 更新配额配置
     */
    boolean updateQuota(Long id, Long maxPerDay, Long maxPerMinute, Integer maxConcurrent, Integer alertThreshold);

    /**
     * 重置日配额 (管理后台用)
     */
    boolean resetDaily(Long apiKeyId);
}
