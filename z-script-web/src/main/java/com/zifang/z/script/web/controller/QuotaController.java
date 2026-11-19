package com.zifang.z.script.web.controller;

import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import com.zifang.z.script.core.domain.entity.QuotaDO;
import com.zifang.z.script.core.domain.service.impl.QuotaServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * z-script 配额管理 Controller (FEATURE051)
 */
@RestController
@RequestMapping("/api/script/quota")
public class QuotaController {

    private static final Logger log = LogManager.getLogger(QuotaController.class);

    @Autowired
    private QuotaServiceImpl quotaService;

    /**
     * 查询某 API Key 的配额
     */
    @GetMapping("/by-api-key/{apiKeyId}")
    @BizApi(summary = "查询某 API Key 的配额", tags = { "z-script" })
    public Map<String, Object> getByApiKey(@PathVariable @BizParam(name = "apiKeyId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long apiKeyId) {
        Map<String, Object> result = new HashMap<>();
        QuotaDO quota = quotaService.getByApiKeyId(apiKeyId);
        if (quota == null) {
            quota = quotaService.initForApiKey(apiKeyId);
        }
        result.put("success", true);
        result.put("data", quota);
        return result;
    }

    /**
     * 更新配额配置
     */
    @PutMapping("/{id}")
    @BizApi(summary = "更新配额配置", tags = { "z-script" })
    public Map<String, Object> update(@PathVariable @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long id,
                                      @RequestParam(required = false)
                                      @BizParam(name = "maxPerDay", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long maxPerDay,
                                      @RequestParam(required = false)
                                      @BizParam(name = "maxPerMinute", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long maxPerMinute,
                                      @RequestParam(required = false)
                                      @BizParam(name = "maxConcurrent", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Integer maxConcurrent,
                                      @RequestParam(required = false)
                                      @BizParam(name = "alertThreshold", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Integer alertThreshold) {
        boolean ok = quotaService.updateQuota(id, maxPerDay, maxPerMinute, maxConcurrent, alertThreshold);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "配额已更新" : "配额更新失败");
        return result;
    }

    /**
     * 重置日配额
     */
    @PostMapping("/reset-daily/{apiKeyId}")
    @BizApi(summary = "重置日配额", tags = { "z-script" })
    public Map<String, Object> resetDaily(@PathVariable @BizParam(name = "apiKeyId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long apiKeyId) {
        boolean ok = quotaService.resetDaily(apiKeyId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "日配额已重置" : "重置失败");
        return result;
    }
}

