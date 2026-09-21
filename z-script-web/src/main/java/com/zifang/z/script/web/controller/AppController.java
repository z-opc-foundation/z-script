package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.script.core.domain.entity.ApiKeyDO;
import com.zifang.z.script.core.domain.entity.AppDO;
import com.zifang.z.script.core.domain.service.ApiKeyService;
import com.zifang.z.script.core.domain.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * z-script 应用管理 Controller。
 * <p>
 * 路径: /api/script/app。应用是权限模型的中心：AK 挂应用、脚本列表挂应用；
 * 签 Key 时应用不存在会自动引导创建（见 AppService.findOrCreate），这里的 CRUD 负责日常治理。
 */
@Tag(name = "应用管理")
@RestController
@RequestMapping("/api/script/app")
public class AppController {

    @Resource
    private AppService appService;
    @Resource
    private ApiKeyService apiKeyService;

    @Operation(summary = "应用列表")
    @GetMapping("/list")
    public Result<List<AppDO>> list() {
        return Result.success(appService.list());
    }

    @Operation(summary = "创建应用")
    @PostMapping
    public Result<AppDO> create(@RequestBody Map<String, String> body) {
        String appCode = body.get("appCode");
        if (appCode == null || appCode.trim().isEmpty()) {
            return Result.<AppDO>fail("appCode 必填").code(400);
        }
        if (appService.getByAppCode(appCode.trim()) != null) {
            return Result.<AppDO>fail("appCode 已存在: " + appCode).code(400);
        }
        AppDO app = appService.findOrCreate(appCode.trim(), body.getOrDefault("ownerId", "anonymous"),
                body.getOrDefault("scope", "SPECIFIC"));
        if (body.get("appName") != null && !body.get("appName").isEmpty()) {
            app.setAppName(body.get("appName"));
            appService.updateById(app);
        }
        return Result.success(app);
    }

    @Operation(summary = "更新应用基础信息 / 启用禁用")
    @PutMapping
    public Result<AppDO> update(@RequestParam String appCode, @RequestBody AppDO patch) {
        AppDO app = appService.getByAppCode(appCode);
        if (app == null) {
            return Result.<AppDO>fail("应用不存在: " + appCode).code(404);
        }
        if (patch.getAppName() != null) {
            app.setAppName(patch.getAppName());
        }
        if (patch.getOwnerId() != null) {
            app.setOwnerId(patch.getOwnerId());
        }
        if (patch.getDescription() != null) {
            app.setDescription(patch.getDescription());
        }
        if (patch.getStatus() != null) {
            app.setStatus(patch.getStatus());
        }
        app.setUpdateTime(new Date());
        appService.updateById(app);
        return Result.success(app);
    }

    @Operation(summary = "配置应用可访问的脚本列表 (scope=ALL 时忽略列表)")
    @PostMapping("/scripts")
    public Result<AppDO> saveScripts(@RequestParam String appCode, @RequestBody Map<String, Object> body) {
        String scope = (String) body.getOrDefault("scope", "SPECIFIC");
        @SuppressWarnings("unchecked")
        List<String> scripts = (List<String>) body.get("scripts");
        if (!appService.saveScripts(appCode, scope, scripts)) {
            return Result.<AppDO>fail("应用不存在: " + appCode).code(404);
        }
        return Result.success(appService.getByAppCode(appCode));
    }

    @Operation(summary = "启用应用（名下所有 Key 恢复可用）")
    @PostMapping("/{appCode}/enable")
    public Result<Boolean> enable(@PathVariable String appCode) {
        return Result.success(setStatus(appCode, 1));
    }

    @Operation(summary = "禁用应用（名下所有 Key 一并 403）")
    @PostMapping("/{appCode}/disable")
    public Result<Boolean> disable(@PathVariable String appCode) {
        return Result.success(setStatus(appCode, 0));
    }

    @Operation(summary = "删除应用（名下还有 Key 时拒绝）")
    @DeleteMapping
    public Result<Boolean> delete(@RequestParam String appCode) {
        AppDO app = appService.getByAppCode(appCode);
        if (app == null) {
            return Result.<Boolean>fail("应用不存在: " + appCode).code(404);
        }
        Long keyCount = apiKeyService.count(new LambdaQueryWrapper<ApiKeyDO>().eq(ApiKeyDO::getAppId, app.getId()));
        if (keyCount != null && keyCount > 0) {
            return Result.<Boolean>fail("该应用名下还有 " + keyCount + " 把 Key，先删除/转移再删应用").code(400);
        }
        return Result.success(appService.removeById(app.getId()));
    }

    private boolean setStatus(String appCode, int status) {
        AppDO app = appService.getByAppCode(appCode);
        if (app == null) {
            return false;
        }
        app.setStatus(status);
        app.setUpdateTime(new Date());
        return appService.updateById(app);
    }
}
