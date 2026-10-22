package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.meta.Result;
import com.zifang.z.script.core.domain.entity.ApiKeyDO;
import com.zifang.z.script.core.domain.service.ApiKeyService;
import com.zifang.z.script.core.domain.service.impl.ApiKeyServiceImpl;
import com.zifang.z.script.web.dto.ApiKeyCreateResponse;
import com.zifang.z.script.web.dto.ApiKeyManageVO;
import com.zifang.z.script.web.dto.ApiKeyResetSecretResponse;
import com.zifang.z.script.web.support.ApiKeyAccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * z-script API Key 管理 Controller (FEATURE051)
 * <p>
 * 路径: /api/script/api-key
 * <p>
 * <b>TASK-20260925-031 整改</b>：读端点原本直接返回 {@link ApiKeyDO}，
 * 把 {@code api_key} 明文与 {@code api_secret_hash} 一起吐了出去 ——
 * 任何持有任意一把有效 Key 的调用方都能把整张密钥表拿走（再拿别人的 Key 调别人的脚本）。
 * 现在读端点一律：
 * <ol>
 *   <li><b>只出脱敏 VO</b>（{@link ApiKeyManageVO}）：api_key 只给掩码，api_secret_hash 不出现在响应体里；</li>
 *   <li><b>只出自己应用的 Key</b>（{@link ApiKeyAccessGuard} 按 appName 判归属，兼容 app_id 为空的存量数据）；</li>
 *   <li><b>按 scope 判角色</b>：ALL 读写、READ_ONLY 只读、SPECIFIC 不进管理面。</li>
 * </ol>
 * 写端点（重置 Secret / 启用 / 禁用 / 删除）同样补了归属校验 ——
 * 不补的话，「重置别人的 Key 的 Secret」是一条等价旁路：改完就把别人的明文 Secret 拿走了。
 */
@Tag(name = "API Key 管理")
@RestController
@RequestMapping("/api/script/api-key")
public class ApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @Operation(summary = "分页查询 API Key（脱敏 + 只出本应用的 Key）")
    @GetMapping("/page")
    public Result<IPage<ApiKeyManageVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        ApiKeyDO caller = ApiKeyAccessGuard.callerOf(request);
        if (caller == null) {
            return Result.<IPage<ApiKeyManageVO>>fail("NO_API_KEY_IDENTITY: 缺少 API Key 身份").code(401);
        }
        if (!ApiKeyAccessGuard.canRead(caller)) {
            return Result.<IPage<ApiKeyManageVO>>fail("SCOPE_NOT_ALLOWED: 当前 Key 的 scope 无权读取 API Key 清单").code(403);
        }

        LambdaQueryWrapper<ApiKeyDO> qw = new LambdaQueryWrapper<>();
        // 归属收窄：只查本应用的 Key（app_id 存量整列 NULL，appName 是归属主键）
        String ownApp = ApiKeyAccessGuard.ownAppName(caller);
        if (ownApp != null) {
            qw.eq(ApiKeyDO::getAppName, ownApp);
        }
        if (appName != null && !appName.isEmpty()) {
            qw.like(ApiKeyDO::getAppName, appName);
        }

        if (status != null) {
            qw.eq(ApiKeyDO::getStatus, status);
        }

        qw.orderByDesc(ApiKeyDO::getCreateTime);

        IPage<ApiKeyDO> page = apiKeyService.page(new Page<>(current, size), qw);
        // 二次归属过滤：即便 service/DB 侧漏了过滤，也不把别人的 Key 带出响应体
        List<ApiKeyManageVO> records = new ArrayList<>();
        for (ApiKeyDO row : page.getRecords()) {
            if (ApiKeyAccessGuard.owns(caller, row)) {
                records.add(ApiKeyManageVO.from(row));
            }
        }
        Page<ApiKeyManageVO> out = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        out.setRecords(records);
        return Result.success(out);
    }

    @Operation(summary = "查询所有启用的 Key（脱敏 + 只出本应用的 Key）")
    @GetMapping("/list-enabled")
    public Result<List<ApiKeyManageVO>> listEnabled(HttpServletRequest request) {
        ApiKeyDO caller = ApiKeyAccessGuard.callerOf(request);
        if (caller == null) {
            return Result.<List<ApiKeyManageVO>>fail("NO_API_KEY_IDENTITY: 缺少 API Key 身份").code(401);
        }
        if (!ApiKeyAccessGuard.canRead(caller)) {
            return Result.<List<ApiKeyManageVO>>fail("SCOPE_NOT_ALLOWED: 当前 Key 的 scope 无权读取 API Key 清单").code(403);
        }
        List<ApiKeyManageVO> out = new ArrayList<>();
        for (ApiKeyDO row : apiKeyService.listEnabled()) {
            if (ApiKeyAccessGuard.owns(caller, row)) {
                out.add(ApiKeyManageVO.from(row));
            }
        }
        return Result.success(out);
    }

    @Operation(summary = "创建 API Key (返回明文 secret 仅 1 次, 必须立即保存)")
    @PostMapping
    public Result<ApiKeyCreateResponse> create(@RequestBody java.util.Map<String, String> body) {
        // 这条是**引导端点**：ZScriptWebMvcConfig 把精确路径 /api/script/api-key 排除在 AK 鉴权之外
        // （建第一把 Key 时调用方手上还没有 Key）。读端点与写端点不在排除范围内，都走归属校验。
        // 残留面（引导端点免鉴权 ⇒ 谁都能建一把 Key）：见 TASK-20260925-031 的提案，未在本卡改动。
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

    @Operation(summary = "重置 Secret（只能重置本应用的 Key）")
    @PostMapping("/{id}/reset-secret")
    public Result<ApiKeyResetSecretResponse> resetSecret(@PathVariable Long id, HttpServletRequest request) {
        ApiKeyDO caller = ApiKeyAccessGuard.callerOf(request);
        if (caller == null) {
            return Result.<ApiKeyResetSecretResponse>fail("NO_API_KEY_IDENTITY: 缺少 API Key 身份").code(401);
        }
        if (!ApiKeyAccessGuard.canWrite(caller)) {
            return Result.<ApiKeyResetSecretResponse>fail("SCOPE_NOT_ALLOWED: 当前 Key 的 scope 无权重置 Secret").code(403);
        }
        ApiKeyDO target = apiKeyService.getById(id);
        if (target == null || !ApiKeyAccessGuard.owns(caller, target)) {
            return Result.<ApiKeyResetSecretResponse>fail("API Key 不存在或无权操作").code(404);
        }
        String plainSecret = apiKeyService.resetSecret(id);
        if (plainSecret == null) {
            return Result.<ApiKeyResetSecretResponse>fail("API Key 不存在").code(404);
        }
        ApiKeyResetSecretResponse out = new ApiKeyResetSecretResponse(id, plainSecret);
        return Result.success(out);
    }

    @Operation(summary = "启用（只能改本应用的 Key）")
    @PostMapping("/{id}/enable")
    public Result<Boolean> enable(@PathVariable Long id, HttpServletRequest request) {
        Result<Boolean> guard = guardWrite(id, request);
        if (guard != null) {
            return guard;
        }
        return Result.success(apiKeyService.enable(id));
    }

    @Operation(summary = "禁用（只能改本应用的 Key）")
    @PostMapping("/{id}/disable")
    public Result<Boolean> disable(@PathVariable Long id, HttpServletRequest request) {
        Result<Boolean> guard = guardWrite(id, request);
        if (guard != null) {
            return guard;
        }
        return Result.success(apiKeyService.disable(id));
    }

    @Operation(summary = "删除（只能删本应用的 Key）")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        Result<Boolean> guard = guardWrite(id, request);
        if (guard != null) {
            return guard;
        }
        return Result.success(apiKeyService.removeById(id));
    }

    @Operation(summary = "详情（脱敏 + 只出本应用的 Key）")
    @GetMapping("/{id}")
    public Result<ApiKeyManageVO> get(@PathVariable Long id, HttpServletRequest request) {
        ApiKeyDO caller = ApiKeyAccessGuard.callerOf(request);
        if (caller == null) {
            return Result.<ApiKeyManageVO>fail("NO_API_KEY_IDENTITY: 缺少 API Key 身份").code(401);
        }
        if (!ApiKeyAccessGuard.canRead(caller)) {
            return Result.<ApiKeyManageVO>fail("SCOPE_NOT_ALLOWED: 当前 Key 的 scope 无权读取 API Key 详情").code(403);
        }
        ApiKeyDO entity = apiKeyService.getById(id);
        if (entity == null || !ApiKeyAccessGuard.owns(caller, entity)) {
            return Result.<ApiKeyManageVO>fail("API Key 不存在或无权访问").code(404);
        }
        return Result.success(ApiKeyManageVO.from(entity));
    }

    /**
     * 写端点的前置闸门：调用方身份 → scope 角色 → 归属。
     *
     * @param id      目标 Key 主键
     * @param request 当前请求
     * @return null 表示放行；非 null 表示应当直接返回的失败结果
     */
    private Result<Boolean> guardWrite(Long id, HttpServletRequest request) {
        ApiKeyDO caller = ApiKeyAccessGuard.callerOf(request);
        if (caller == null) {
            return Result.<Boolean>fail("NO_API_KEY_IDENTITY: 缺少 API Key 身份").code(401);
        }
        if (!ApiKeyAccessGuard.canWrite(caller)) {
            return Result.<Boolean>fail("SCOPE_NOT_ALLOWED: 当前 Key 的 scope 无权改 API Key").code(403);
        }
        ApiKeyDO target = apiKeyService.getById(id);
        if (target == null || !ApiKeyAccessGuard.owns(caller, target)) {
            return Result.<Boolean>fail("API Key 不存在或无权操作").code(404);
        }
        return null;
    }
}
