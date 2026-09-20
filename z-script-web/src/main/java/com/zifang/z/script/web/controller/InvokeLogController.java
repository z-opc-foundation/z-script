package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.z.script.core.domain.entity.InvokeLogDO;
import com.zifang.z.script.core.domain.service.impl.InvokeLogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * z-script 调用日志 Controller (FEATURE051)
 */
@RestController
@RequestMapping("/api/script/invoke-log")
public class InvokeLogController {

    @Autowired
    private InvokeLogServiceImpl invokeLogService;

    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam(required = false) Long apiKeyId,
                                    @RequestParam(required = false) String scriptCode,
                                    @RequestParam(required = false) Integer success,
                                    @RequestParam(defaultValue = "1") int pageNo,
                                    @RequestParam(defaultValue = "20") int pageSize) {
        IPage<InvokeLogDO> page = invokeLogService.queryByPage(apiKeyId, scriptCode, success, pageNo, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", page);
        return result;
    }

    @GetMapping("/stats/sum-recent")
    public Map<String, Object> sumRecent(@RequestParam Long apiKeyId,
                                         @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", invokeLogService.sumRecentCalls(apiKeyId, days));
        return result;
    }

    @GetMapping("/stats/failure-rate")
    public Map<String, Object> failureRate(@RequestParam Long apiKeyId,
                                           @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", invokeLogService.failureRate(apiKeyId, days));
        return result;
    }

    /**
     * FEATURE051 t19 - 按天调用趋势 (最近 N 天, 含成功/失败分布)
     */
    @GetMapping("/stats/trend")
    public Map<String, Object> trend(@RequestParam(required = false) Long apiKeyId,
                                     @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", invokeLogService.dailyTrend(apiKeyId, days));
        return result;
    }
}