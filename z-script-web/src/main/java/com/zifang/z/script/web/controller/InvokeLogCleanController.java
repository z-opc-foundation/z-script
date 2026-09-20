package com.zifang.z.script.web.controller;

import com.zifang.z.script.web.job.InvokeLogCleanupJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * FEATURE051 - t18 日志清理 Controller (手动触发 / 查看配置)
 */
@RestController
@RequestMapping("/api/script/invoke-log/clean")
public class InvokeLogCleanController {

    @Autowired
    private InvokeLogCleanupJob cleanupJob;

    /**
     * 手动触发清理, days=0 用默认配置
     */
    @PostMapping("/run")
    public Map<String, Object> runClean(@RequestParam(defaultValue = "0") int days) {
        long start = System.currentTimeMillis();
        int deleted = cleanupJob.manualClean(days);
        long cost = System.currentTimeMillis() - start;
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("deleted", deleted);
        result.put("days", days > 0 ? days : cleanupJob.getRetentionDays());
        result.put("costMs", cost);
        result.put("message", "清理完成, 删除 " + deleted + " 条记录");
        return result;
    }

    /**
     * 查看清理配置
     */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> result = new HashMap<>();
        result.put("retentionDays", cleanupJob.getRetentionDays());
        result.put("enabled", cleanupJob.isCleanEnabled());
        result.put("cron", "0 0 3 * * ?  (每天凌晨3点)");
        return result;
    }
}
