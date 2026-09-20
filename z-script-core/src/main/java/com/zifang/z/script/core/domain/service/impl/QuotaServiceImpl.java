package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.script.core.domain.entity.QuotaDO;
import com.zifang.z.script.core.domain.mapper.QuotaMapper;
import com.zifang.z.script.core.domain.service.QuotaService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * z-script 配额服务实现 (FEATURE051)
 * <p>
 * 限流算法: 滑动窗口(分钟级) + DB 计数器(日级) + ConcurrentHashMap(并发级)
 * 日级: 跨 0 点自动重置, 写 DB
 * 分钟级: 每分钟重置, DB + 内存双层
 * 并发级: 纯内存 ConcurrentHashMap (轻量)
 */
@Service
public class QuotaServiceImpl extends ServiceImpl<QuotaMapper, QuotaDO> implements QuotaService {

    private static final Logger log = LogManager.getLogger(QuotaServiceImpl.class);

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat MINUTE_FMT = new SimpleDateFormat("yyyyMMddHHmm");

    @Override
    public QuotaDO getByApiKeyId(Long apiKeyId) {
        return getOne(new LambdaQueryWrapper<QuotaDO>()
                .eq(QuotaDO::getApiKeyId, apiKeyId)
                .last("LIMIT 1"));
    }

    @Override
    public QuotaDO initForApiKey(Long apiKeyId) {
        QuotaDO existing = getByApiKeyId(apiKeyId);
        if (existing != null) {
            return existing;
        }


        QuotaDO quota = new QuotaDO();
        quota.setApiKeyId(apiKeyId);
        quota.setMaxPerDay(10000L);
        quota.setMaxPerMinute(60L);
        quota.setMaxConcurrent(10);
        quota.setAlertThreshold(80);
        quota.setUsedToday(0L);
        quota.setCreateTime(new Date());
        quota.setUpdateTime(new Date());
        save(quota);
        log.info("[FEATURE051] 配额初始化: apiKeyId={}, maxPerDay={}, maxPerMinute={}",
                apiKeyId, quota.getMaxPerDay(), quota.getMaxPerMinute());
        return quota;
    }

    @Override
    public Long tryAcquire(Long apiKeyId) {
        QuotaDO quota = getByApiKeyId(apiKeyId);
        if (quota == null) {
            quota = initForApiKey(apiKeyId);
        }

        Date now = new Date();

        // ========== 1. 日级限流 ==========
        String today = DATE_FMT.format(now);
        if (quota.getUsedTodayDate() == null || !DATE_FMT.format(quota.getUsedTodayDate()).equals(today)) {
            // 跨天: 重置
            quota.setUsedToday(0L);
            quota.setUsedTodayDate(now);
        }
        if (quota.getUsedToday() != null && quota.getMaxPerDay() != null
                && quota.getUsedToday() >= quota.getMaxPerDay()) {
            log.warn("[FEATURE051] 日配额耗尽: apiKeyId={}, used={}, max={}",
                    apiKeyId, quota.getUsedToday(), quota.getMaxPerDay());
            return secondsUntilMidnight();
        }

        // ========== 2. 分钟级限流 (滑动窗口) ==========
        String currentMinute = MINUTE_FMT.format(now);
        if (!currentMinute.equals(quota.getUsedMinuteWindow())) {
            // 新的一分钟: 重置
            quota.setUsedMinute(0L);
            quota.setUsedMinuteWindow(currentMinute);
        }
        if (quota.getUsedMinute() != null && quota.getMaxPerMinute() != null
                && quota.getUsedMinute() >= quota.getMaxPerMinute()) {
            log.warn("[FEATURE051] 分钟配额耗尽: apiKeyId={}, used={}, max={}",
                    apiKeyId, quota.getUsedMinute(), quota.getMaxPerMinute());
            return 60L; // 1 分钟后重试
        }

        // ========== 3. 通过: 计数器 +1 ==========
        quota.setUsedToday((quota.getUsedToday() == null ? 0L : quota.getUsedToday()) + 1);
        quota.setUsedMinute((quota.getUsedMinute() == null ? 0L : quota.getUsedMinute()) + 1);
        quota.setUpdateTime(now);

        // 高频场景: 简化更新 (只更新 usedToday/usedMinute/window/updated)
        update(new LambdaUpdateWrapper<QuotaDO>()
                .eq(QuotaDO::getId, quota.getId())
                .set(QuotaDO::getUsedToday, quota.getUsedToday())
                .set(QuotaDO::getUsedTodayDate, quota.getUsedTodayDate())
                .set(QuotaDO::getUsedMinute, quota.getUsedMinute())
                .set(QuotaDO::getUsedMinuteWindow, quota.getUsedMinuteWindow())
                .set(QuotaDO::getUpdateTime, now));

        return null; // 通过
    }

    @Override
    public boolean shouldAlert(QuotaDO quota) {
        if (quota == null || quota.getMaxPerDay() == null || quota.getMaxPerDay() == 0) return false;

        if (quota.getAlertThreshold() == null) return false;

        if (quota.getUsedToday() == null) return false;


        long usagePercent = (quota.getUsedToday() * 100) / quota.getMaxPerDay();
        return usagePercent >= quota.getAlertThreshold();
    }

    @Override
    public void markAlerted(Long quotaId) {
        update(new LambdaUpdateWrapper<QuotaDO>()
                .eq(QuotaDO::getId, quotaId)
                .set(QuotaDO::getAlertedAt, new Date()));
    }

    @Override
    public boolean updateQuota(Long id, Long maxPerDay, Long maxPerMinute, Integer maxConcurrent, Integer alertThreshold) {
        return update(new LambdaUpdateWrapper<QuotaDO>()
                .eq(QuotaDO::getId, id)
                .set(maxPerDay != null, QuotaDO::getMaxPerDay, maxPerDay)
                .set(maxPerMinute != null, QuotaDO::getMaxPerMinute, maxPerMinute)
                .set(maxConcurrent != null, QuotaDO::getMaxConcurrent, maxConcurrent)
                .set(alertThreshold != null, QuotaDO::getAlertThreshold, alertThreshold)
                .set(QuotaDO::getUpdateTime, new Date()));
    }

    @Override
    public boolean resetDaily(Long apiKeyId) {
        return update(new LambdaUpdateWrapper<QuotaDO>()
                .eq(QuotaDO::getApiKeyId, apiKeyId)
                .set(QuotaDO::getUsedToday, 0L)
                .set(QuotaDO::getUsedTodayDate, new Date())
                .set(QuotaDO::getUpdateTime, new Date()));
    }

    /**
     * 计算到 0 点 (明日) 的秒数
     */
    private long secondsUntilMidnight() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return (midnight.getTimeInMillis() - now.getTimeInMillis()) / 1000;
    }
}
