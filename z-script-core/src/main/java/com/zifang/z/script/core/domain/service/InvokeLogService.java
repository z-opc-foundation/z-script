package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.InvokeLogDO;

/**
 * z-script 调用日志服务接口 (FEATURE051)
 */
public interface InvokeLogService extends IService<InvokeLogDO> {

    /**
     * 异步记录一次调用 (推荐用于调用路径)
     */
    void recordAsync(InvokeLogDO log);

    /**
     * 同步记录一次调用 (用于错误路径)
     */
    void recordSync(InvokeLogDO log);

    /**
     * 分页查询日志
     */
    IPage<InvokeLogDO> queryByPage(Long apiKeyId, String scriptCode, Integer success,
                                   int pageNo, int pageSize);

    /**
     * 统计某 API Key 的最近 N 天调用量
     */
    Long sumRecentCalls(Long apiKeyId, int days);

    /**
     * 统计某 API Key 的失败率
     */
    Double failureRate(Long apiKeyId, int days);

    /**
     * FEATURE051 t19 - 按天调用趋势 (含成功/失败分布)
     *
     * @param apiKeyId 可选; null 表示全量
     * @param days     最近 N 天
     * @return [{date: '2026-07-14', total: 100, success: 90, failed: 10}, ...]
     */
    java.util.List<java.util.Map<String, Object>> dailyTrend(Long apiKeyId, int days);
}