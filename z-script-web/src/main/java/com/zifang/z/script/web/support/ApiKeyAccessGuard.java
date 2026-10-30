package com.zifang.z.script.web.support;

import com.zifang.z.script.core.domain.entity.ApiKeyDO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API Key 管理面的「角色 / 归属」判定 (TASK-20260925-031)。
 * <p>
 * <b>为什么没有平台角色</b>：z-script 不挂 z-ctc 4A / SSO（见 ZScriptWebMvcConfig 的类注释），
 * 唯一的认证方式是 app + API Key ⇒ 这里的「角色」只能是 Key 自带的 scope，
 * 这里的「归属」只能是 Key 所属的应用。
 * <p>
 * <b>归属主键取 appName，不取 ownerId</b>：ownerId 是建 Key 时请求体里带的自由文本
 * （{@code ApiKeyController#create} 默认给 {@code "anonymous"}），调用方自己就能填 ⇒
 * 它不是可信身份，拿它当授权依据等于没授权。app_id 在存量数据里整列为 NULL
 * （{@code z_script_app} 表 0 行），但保留为兼容位：将来补了应用绑定后，
 * 同一 app_id 的 Key 自然同属。
 * <p>
 * <b>scope 怎么当角色用</b>：
 * <ul>
 *   <li>{@code ALL}：管理面读写都放行；</li>
 *   <li>{@code READ_ONLY}：管理面只读（列自己的 Key 可以，改别人的不行）；</li>
 *   <li>{@code SPECIFIC}：脚本级 Key，不进管理面（它本来只该调被点名的脚本）。</li>
 * </ul>
 * 注意拦截器的 {@code verifyScope(entity, null)} 对管理面端点（scriptCode 为空）是放行的，
 * 所以 scope 这道判定必须由本类在 Controller 侧补上，否则 SPECIFIC 的 Key 也能进管理面。
 */
public final class ApiKeyAccessGuard {

    /**
     * 拦截器把通过鉴权的 Key 塞进请求上下文的位置（见 {@code ApiKeyAuthInterceptor} 第 10 步）。
     */
    public static final String CALLER_ATTR = "z.apiKeyEntity";

    private ApiKeyAccessGuard() {
    }

    /**
     * 取当前请求的调用方 Key。
     *
     * @param request 当前请求（可为 null）
     * @return 调用方 Key；没有有效 API Key 身份时返回 null
     */
    public static ApiKeyDO callerOf(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(CALLER_ATTR);
        return attr instanceof ApiKeyDO ? (ApiKeyDO) attr : null;
    }

    /**
     * 调用方是否拥有目标 Key（自己那把、同一 app_id、同一 appName 都算拥有）。
     *
     * @param caller 调用方 Key
     * @param target 目标 Key
     * @return true 表示同属
     */
    public static boolean owns(ApiKeyDO caller, ApiKeyDO target) {
        if (caller == null || target == null) {
            return false;
        }
        if (caller.getId() != null && caller.getId().equals(target.getId())) {
            return true;
        }
        if (caller.getAppId() != null && caller.getAppId().equals(target.getAppId())) {
            return true;
        }
        String callerApp = trim(caller.getAppName());
        String targetApp = trim(target.getAppName());
        return !callerApp.isEmpty() && !targetApp.isEmpty() && callerApp.equalsIgnoreCase(targetApp);
    }

    /**
     * 读权限：ALL / READ_ONLY 可读管理面。
     *
     * @param caller 调用方 Key
     * @return true 表示可读
     */
    public static boolean canRead(ApiKeyDO caller) {
        if (caller == null) {
            return false;
        }
        String scope = scopeOf(caller);
        return "ALL".equals(scope) || "READ_ONLY".equals(scope);
    }

    /**
     * 写权限（改 Key / 重置 Secret）：只有 ALL。
     *
     * @param caller 调用方 Key
     * @return true 表示可写
     */
    public static boolean canWrite(ApiKeyDO caller) {
        if (caller == null) {
            return false;
        }
        return "ALL".equals(scopeOf(caller));
    }

    /**
     * 归属过滤用的 appName（DB 层收窄查询用）。
     *
     * @param caller 调用方 Key
     * @return appName；没有可用归属键时返回 null（此时只能在内存里逐条判归属）
     */
    public static String ownAppName(ApiKeyDO caller) {
        if (caller == null) {
            return null;
        }
        String app = trim(caller.getAppName());
        return app.isEmpty() ? null : app;
    }

    private static String scopeOf(ApiKeyDO caller) {
        return trim(caller.getScope()).toUpperCase();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
