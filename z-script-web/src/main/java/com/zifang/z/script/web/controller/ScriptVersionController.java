package com.zifang.z.script.web.controller;

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
    public Map<String, Object> publish(@RequestParam Long scriptId,
                                       @RequestParam String scriptCode,
                                       @RequestParam String versionNo,
                                       @RequestParam String dslType,
                                       @RequestParam String dslContent,
                                       @RequestParam(required = false) String outputMapping,
                                       @RequestParam(required = false) String changeLog) {
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
    public Map<String, Object> list(@PathVariable Long scriptId) {
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
    public Map<String, Object> setCanary(@PathVariable Long versionId,
                                         @RequestParam Integer canaryWeight) {
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
    public Map<String, Object> promote(@PathVariable Long versionId) {
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
    public Map<String, Object> offline(@PathVariable Long versionId) {
        boolean ok = versionService.offline(versionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "已下线" : "下线失败");
        return result;
    }
}