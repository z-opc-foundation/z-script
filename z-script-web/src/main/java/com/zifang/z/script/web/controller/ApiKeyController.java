package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.meta.Result;
import com.zifang.z.script.core.domain.entity.ApiKeyDO;
import com.zifang.z.script.core.domain.service.ApiKeyService;
import com.zifang.z.script.core.domain.service.impl.ApiKeyServiceImpl;
import com.zifang.z.script.web.dto.ApiKeyCreateResponse;
import com.zifang.z.script.web.dto.ApiKeyResetSecretResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * z-script API Key 管理 Controller (FEATURE051)
 * <p>
 * 路径: /api/script/api-key
 */
@Tag(name = "API Key 管理")
@RestController
@RequestMapping("/api/script/api-key")
public class ApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @Operation(summary = "分页查询 API Key")
    @GetMapping("/page")
    public Result<IPage<ApiKeyDO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<ApiKeyDO> qw = new LambdaQueryWrapper<>();
        if (appName != null && !appName.isEmpty()) {
            qw.like(ApiKeyDO::getAppName, appName);
        }

        if (status != null) {
            qw.eq(ApiKeyDO::getStatus, status);
        }

        qw.orderByDesc(ApiKeyDO::getCreateTime);

        IPage<ApiKeyDO> page = apiKeyService.page(new Page<>(current, size), qw);
        return Result.success(page);
    }

    @Operation(summary = "查询所有启用的 Key")
    @GetMapping("/list-enabled")
    public Result<List<ApiKeyDO>> listEnabled() {
        return Result.success(apiKeyService.listEnabled());
    }

    @Operation(summary = "创建 API Key (返回明文 secret 仅 1 次, 必须立即保存)")
    @PostMapping
    public Result<ApiKeyCreateResponse> create(@RequestBody java.util.Map<String, String> body) {
        String appName = body.get("appName");
        String ownerId = body.getOrDefault("ownerId", "anonymous");
        String scope = body.getOrDefault("scope", "ALL");
        String description = body.getOrDefault("description", "");

        if (appName == null || appName.trim().isEmpty()) {
            return Result.<ApiKeyCreateResponse>fail("appName 必填").code(400);
        }

        ApiKeyDO entity = apiKeyService.createApiKey(appName, ownerId, scope, description);

        // 读取明文 secret (仅此一次)
        String plainSecret = ApiKeyServiceImpl.PLAIN_SECRET_HOLDER.get();
        ApiKeyServiceImpl.PLAIN_SECRET_HOLDER.remove(); // 立即清除

        ApiKeyCreateResponse out = new ApiKeyCreateResponse(
                entity.getApiKey(), plainSecret, entity.getAppName(),
                entity.getStatus(), entity.getScope(), entity.getCreateTime());

        return Result.success(out);
    }

    @Operation(summary = "重置 Secret")
    @PostMapping("/{id}/reset-secret")
    public Result<ApiKeyResetSecretResponse> resetSecret(@PathVariable Long id) {
        String plainSecret = apiKeyService.resetSecret(id);
        if (plainSecret == null) {
            return Result.<ApiKeyResetSecretResponse>fail("API Key 不存在").code(404);
        }
        ApiKeyResetSecretResponse out = new ApiKeyResetSecretResponse(id, plainSecret);
        return Result.success(out);
    }

    @Operation(summary = "启用")
    @PostMapping("/{id}/enable")
    public Result<Boolean> enable(@PathVariable Long id) {
        return Result.success(apiKeyService.enable(id));
    }

    @Operation(summary = "禁用")
    @PostMapping("/{id}/disable")
    public Result<Boolean> disable(@PathVariable Long id) {
        return Result.success(apiKeyService.disable(id));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(apiKeyService.removeById(id));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<ApiKeyDO> get(@PathVariable Long id) {
        return Result.success(apiKeyService.getById(id));
    }
}
