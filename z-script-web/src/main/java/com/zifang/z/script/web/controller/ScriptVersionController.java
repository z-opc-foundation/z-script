package com.zifang.z.script.web.controller;

import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import com.zifang.z.script.core.domain.entity.ScriptVersionDO;
import com.zifang.z.script.core.domain.service.impl.ScriptVersionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z-script 版本管理 Controller (FEATURE051)
 */
@RestController
@RequestMapping("/api/script/version")
public class ScriptVersionController {

    @Autowired
    private ScriptVersionServiceImpl versionService;

    /**
     * 发布新版本
     */
    @PostMapping("/publish")
    @BizApi(summary = "发布新版本", tags = { "z-script" })
    public Map<String, Object> publish(@RequestParam @BizParam(name = "scriptId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long scriptId,
                                       @RequestParam @BizParam(name = "scriptCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String scriptCode,
                                       @RequestParam @BizParam(name = "versionNo", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String versionNo,
                                       @RequestParam @BizParam(name = "dslType", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dslType,
                                       @RequestParam @BizParam(name = "dslContent", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dslContent,
                                       @RequestParam(required = false)
                                       @BizParam(name = "outputMapping", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String outputMapping,
                                       @RequestParam(required = false)
                                       @BizParam(name = "changeLog", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String changeLog) {
        ScriptVersionDO v = versionService.publishVersion(scriptId, scriptCode, versionNo,
                dslType, dslContent, outputMapping, changeLog);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", v);
        return result;
    }

    /**
     * 列出某脚本的所有版本
     */
    @GetMapping("/list/{scriptId}")
    @BizApi(summary = "列出某脚本的所有版本", tags = { "z-script" })
    public Map<String, Object> list(@PathVariable @BizParam(name = "scriptId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long scriptId) {
        List<ScriptVersionDO> list = versionService.listByScriptId(scriptId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", list);
        return result;
    }

    /**
     * 设置灰度
     */
    @PostMapping("/canary/{versionId}")
    @BizApi(summary = "设置灰度", tags = { "z-script" })
    public Map<String, Object> setCanary(@PathVariable @BizParam(name = "versionId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long versionId,
                                         @RequestParam @BizParam(name = "canaryWeight", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Integer canaryWeight) {
        boolean ok = versionService.setCanary(versionId, canaryWeight);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "灰度已设置" : "设置失败");
        return result;
    }

    /**
     * 灰度晋升为生产
     */
    @PostMapping("/promote/{versionId}")
    @BizApi(summary = "灰度晋升为生产", tags = { "z-script" })
    public Map<String, Object> promote(@PathVariable @BizParam(name = "versionId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long versionId) {
        boolean ok = versionService.promoteToProd(versionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "已晋升为生产" : "晋升失败");
        return result;
    }

    /**
     * 下线版本
     */
    @PostMapping("/offline/{versionId}")
    @BizApi(summary = "下线版本", tags = { "z-script" })
    public Map<String, Object> offline(@PathVariable @BizParam(name = "versionId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long versionId) {
        boolean ok = versionService.offline(versionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "已下线" : "下线失败");
        return result;
    }
}

