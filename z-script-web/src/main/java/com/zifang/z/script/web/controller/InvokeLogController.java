package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.z.script.core.domain.entity.InvokeLogDO;
import com.zifang.z.script.core.domain.service.impl.InvokeLogServiceImpl;
import com.zifang.z.script.web.dto.PageResult;
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

    /**
     * 分页查询调用日志（TASK-20260925-035）。
     *
     * <p><b>参数名</b>：{@code current} / {@code size} 是本仓分页接口的**统一口径**
     * （与 {@code /api/script/api-key/page} 一致）；{@code pageNo} / {@code pageSize} 作为
     * **历史别名**保留 —— 仓内 e2e 脚本（{@code _doc/002_testing/FEATURE051/e2e.sh}）
     * 与旧版前端用的就是这两个名字，直接删会打断它们。
     * 优先级：{@code current/size} 优先，缺省时才回落到别名。
     *
     * <p><b>真分页</b>：靠 {@code ScriptMyBatisConfig} 里注册的
     * {@code PaginationInnerInterceptor}。没有它时本接口恒返回全表（实测 131 行），
     * {@code total} 恒为字符串 {@code "0"}。
     */
    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam(required = false) Long apiKeyId,
                                    @RequestParam(required = false) String scriptCode,
                                    @RequestParam(required = false) Integer success,
                                    @RequestParam(required = false) Long current,
                                    @RequestParam(required = false) Long size,
                                    @RequestParam(required = false) Integer pageNo,
                                    @RequestParam(required = false) Integer pageSize) {
        long rawCurrent = current != null ? current : (pageNo != null ? pageNo : 1L);
        long rawSize = size != null ? size : (pageSize != null ? pageSize : 20L);
        int effCurrent = PageResult.normalizeCurrent(rawCurrent);
        int effSize = PageResult.normalizeSize(rawSize);

        IPage<InvokeLogDO> page = invokeLogService.queryByPage(apiKeyId, scriptCode, success, effCurrent, effSize);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        // total 是服务端 count 出来的真计数（JSON 数字），不是字符串 "0"
        result.put("data", PageResult.of(page, page.getRecords()));
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