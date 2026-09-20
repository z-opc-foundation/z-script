package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.script.core.domain.entity.InvokeLogDO;
import com.zifang.z.script.core.domain.mapper.InvokeLogMapper;
import com.zifang.z.script.core.domain.service.InvokeLogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * z-script 调用日志服务实现 (FEATURE051)
 */
@Service
public class InvokeLogServiceImpl extends ServiceImpl<InvokeLogMapper, InvokeLogDO> implements InvokeLogService {

    private static final Logger log = LogManager.getLogger(InvokeLogServiceImpl.class);

    @Override
    public void recordAsync(InvokeLogDO invokeLog) {
        // 单线程池异步落库, 不阻塞主调用
        Thread t = new Thread(() -> {
            try {
                if (invokeLog.getCreateTime() == null) {
                    invokeLog.setCreateTime(new Date());
                }
                save(invokeLog);
            } catch (Exception e) {
                log.warn("[FEATURE051] 调用日志落库失败: {}", e.getMessage());
            }
        }, "FEATURE051-invoke-log");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void recordSync(InvokeLogDO invokeLog) {
        if (invokeLog.getCreateTime() == null) {
            invokeLog.setCreateTime(new Date());
        }
        try {
            save(invokeLog);
        } catch (Exception e) {
            log.warn("[FEATURE051] 调用日志落库失败: {}", e.getMessage());
        }
    }

    @Override
    public IPage<InvokeLogDO> queryByPage(Long apiKeyId, String scriptCode, Integer success,
                                          int pageNo, int pageSize) {
        LambdaQueryWrapper<InvokeLogDO> wrapper = new LambdaQueryWrapper<>();
        if (apiKeyId != null) {
            wrapper.eq(InvokeLogDO::getApiKeyId, apiKeyId);
        }
        if (scriptCode != null && !scriptCode.isEmpty()) {
            wrapper.eq(InvokeLogDO::getScriptCode, scriptCode);
        }
        if (success != null) {
            wrapper.eq(InvokeLogDO::getInvokeStatus, success);
        }
        wrapper.orderByDesc(InvokeLogDO::getInvokedAt);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public Long sumRecentCalls(Long apiKeyId, int days) {
        Date since = daysAgo(days);
        QueryWrapper<InvokeLogDO> wrapper = new QueryWrapper<>();
        wrapper.eq("api_key_id", apiKeyId)
                .ge("invoked_at", since);
        return baseMapper.selectCount(wrapper);
    }

    @Override
    public Double failureRate(Long apiKeyId, int days) {
        Date since = daysAgo(days);
        Long total = baseMapper.selectCount(new QueryWrapper<InvokeLogDO>()
                .eq("api_key_id", apiKeyId)
                .ge("invoked_at", since));
        if (total == null || total == 0) {
            return 0.0;
        }

        Long failed = baseMapper.selectCount(new QueryWrapper<InvokeLogDO>()
                .eq("api_key_id", apiKeyId)
                .eq("invoke_status", 0)
                .ge("invoked_at", since));
        return (failed * 1.0) / total;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> dailyTrend(Long apiKeyId, int days) {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();

        // 1. 生成 N 天的日期序列 (从最早 -> 今天)
        String[] dates = new String[days];
        for (int i = days - 1; i >= 0; i--) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.add(java.util.Calendar.DAY_OF_MONTH, -i);
            dates[days - 1 - i] = sdf.format(c.getTime());
        }

        // 2. 查 N 天的所有日志
        Date since = daysAgo(days);
        QueryWrapper<InvokeLogDO> qw = new QueryWrapper<InvokeLogDO>()
                .ge("invoked_at", since)
                .select("DATE(invoked_at) AS day, invoke_status, COUNT(*) AS cnt");
        if (apiKeyId != null) {
            qw.eq("api_key_id", apiKeyId);
        }
        qw.groupBy("day, invoke_status");
        // 用 mapper 的 selectMaps 返回 List<Map>
        java.util.List<java.util.Map<String, Object>> rows = baseMapper.selectMaps(qw);

        // 3. 聚合 (date -> {total, success, failed})
        java.util.Map<String, java.util.Map<String, Object>> bucket = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : rows) {
            String day = String.valueOf(row.get("day"));
            Integer status = row.get("invoke_status") == null ? 0 : ((Number) row.get("invoke_status")).intValue();
            Long cnt = row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue();
            java.util.Map<String, Object> slot = bucket.computeIfAbsent(day, k -> new java.util.HashMap<>());
            slot.put("total", ((Number) slot.getOrDefault("total", 0L)).longValue() + cnt);
            if (status == 1) {
                slot.put("success", ((Number) slot.getOrDefault("success", 0L)).longValue() + cnt);
            } else {
                slot.put("failed", ((Number) slot.getOrDefault("failed", 0L)).longValue() + cnt);
            }
        }

        // 4. 填充完整日期序列
        for (String date : dates) {
            java.util.Map<String, Object> slot = bucket.getOrDefault(date, new java.util.HashMap<>());
            long total = ((Number) slot.getOrDefault("total", 0L)).longValue();
            long success = ((Number) slot.getOrDefault("success", 0L)).longValue();
            long failed = ((Number) slot.getOrDefault("failed", 0L)).longValue();
            java.util.Map<String, Object> point = new java.util.HashMap<>();
            point.put("date", date);
            point.put("total", total);
            point.put("success", success);
            point.put("failed", failed);
            result.add(point);
        }
        return result;
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }
}