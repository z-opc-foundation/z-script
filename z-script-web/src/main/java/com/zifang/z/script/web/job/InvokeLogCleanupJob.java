package com.zifang.z.script.web.job;

import com.zifang.z.script.core.domain.mapper.InvokeLogMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * FEATURE051 - t18 日志定时清理
 * <p>
 * 每天凌晨 03:00 清理 N 天前的调用日志, 默认 90 天
 * 配置项 (application.yml):
 * zscript.invoke-log.retention-days: 90
 * zscript.invoke-log.clean-enabled: true
 * zscript.invoke-log.clean-cron: 0 0 3 * * ?
 * <p>
 * 注意: 必须在启动类加 @EnableScheduling
 */
@Component
public class InvokeLogCleanupJob {

    private static final Logger log = LogManager.getLogger(InvokeLogCleanupJob.class);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private InvokeLogMapper invokeLogMapper;
    @Value("${zscript.invoke-log.retention-days:90}")
    private int retentionDays;
    @Value("${zscript.invoke-log.clean-enabled:true}")
    private boolean cleanEnabled;

    /**
     * 默认每天凌晨 3:00 执行
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "${zscript.invoke-log.clean-cron:0 0 3 * * ?}")
    public void cleanOldLogs() {
        if (!cleanEnabled) {
            log.debug("[FEATURE051] 日志清理已禁用");
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -retentionDays);
        Date cutoff = cal.getTime();

        long start = System.currentTimeMillis();
        try {
            int deleted = invokeLogMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.zifang.z.script.core.domain.entity.InvokeLogDO>()
                            .lt("invoked_at", cutoff)
            );
            long cost = System.currentTimeMillis() - start;
            log.info("[FEATURE051] 日志清理完成: 保留 {} 天, 删除 {} 条记录 (< {}), 耗时 {} ms",
                    retentionDays, deleted, SDF.format(cutoff), cost);
        } catch (Exception e) {
            log.error("[FEATURE051] 日志清理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发清理 (供测试 / 管理接口)
     *
     * @param days 保留天数 (传 0 用默认配置)
     */
    public int manualClean(int days) {
        if (days <= 0) {
            days = retentionDays;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        Date cutoff = cal.getTime();
        int deleted = invokeLogMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.zifang.z.script.core.domain.entity.InvokeLogDO>()
                        .lt("invoked_at", cutoff)
        );
        log.info("[FEATURE051] 手动清理: 删除 {} 天前的 {} 条日志", days, deleted);
        return deleted;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public boolean isCleanEnabled() {
        return cleanEnabled;
    }
}
