package com.zifang.z.script.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.util.core.json.JsonMapperFactory;
import com.zifang.z.script.core.domain.entity.ApiKeyDO;
import com.zifang.z.script.core.domain.entity.AppDO;
import com.zifang.z.script.core.domain.entity.InvokeLogDO;
import com.zifang.z.script.core.domain.entity.QuotaDO;
import com.zifang.z.script.core.domain.service.ApiKeyService;
import com.zifang.z.script.core.domain.service.AppService;
import com.zifang.z.script.core.domain.service.InvokeLogService;
import com.zifang.z.script.core.domain.service.QuotaService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * z-script API Key 鉴权拦截器 (FEATURE051)
 * <p>
 * 拦截路径: 见 ZScriptWebMvcConfig（/api/** + 遗留运行时路由 /run/**）
 * 完整校验链: API Key 存在 → 启用 → 未过期 → IP 白名单 → 应用状态+scope（app 中心：
 *            Key 只认证，能调哪些脚本由归属应用的 allowed_scripts 决定）→ 配额 → 签名 → 上下文注入 → 异步日志
 *
 * <b>HMAC readBody 支持</b>:
 * 通过 {@link ContentCachingRequestWrapperFilter} 提前缓存 POST body，使得本拦截器能正确读取
 * 请求体用于签名验证。若 filter 未生效，readBody() 会 fallback 到直接读 input stream（仅一次）。
 */
@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    public static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("z.script.api-key.enabled", "true"));
    private static final Logger log = LogManager.getLogger(ApiKeyAuthInterceptor.class);
    private final ObjectMapper mapper = JsonMapperFactory.getDefault();
    @Autowired
    private ApiKeyService apiKeyService;
    @Autowired
    private QuotaService quotaService;
    @Autowired
    private InvokeLogService invokeLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!ENABLED) {
            return true;
        }

        long startMs = System.currentTimeMillis();

        // ========== 1. 提取 API Key ==========
        String apiKey = extractHeader(request, "X-Api-Key");
        if (apiKey == null || apiKey.isEmpty()) {
            recordLog(request, null, null, null, null, false, 401, startMs, "MISSING_API_KEY");
            writeError(response, HttpStatus.UNAUTHORIZED, "MISSING_API_KEY", "缺少 X-Api-Key 请求头");
            return false;
        }

        // ========== 2. 查询 Key ==========
        ApiKeyDO entity = apiKeyService.getByApiKey(apiKey);
        if (entity == null) {
            recordLog(request, null, null, null, null, false, 401, startMs, "INVALID_API_KEY");
            writeError(response, HttpStatus.UNAUTHORIZED, "INVALID_API_KEY", "API Key 不存在");
            return false;
        }

        // ========== 3. 启用状态 ==========
        if (entity.getStatus() == null || entity.getStatus() != 1) {
            recordLog(request, entity, null, null, null, false, 401, startMs, "API_KEY_DISABLED");
            writeError(response, HttpStatus.UNAUTHORIZED, "API_KEY_DISABLED", "API Key 已禁用");
            return false;
        }

        // ========== 4. 过期校验 ==========
        if (entity.getExpireAt() != null && entity.getExpireAt().getTime() < System.currentTimeMillis()) {
            recordLog(request, entity, null, null, null, false, 401, startMs, "API_KEY_EXPIRED");
            writeError(response, HttpStatus.UNAUTHORIZED, "API_KEY_EXPIRED", "API Key 已过期");
            return false;
        }

        // ========== 5. IP 白名单校验 ==========
        String remoteIp = getClientIp(request);
        if (!apiKeyService.verifyIpWhitelist(entity, remoteIp)) {
            recordLog(request, entity, null, null, remoteIp, false, 403, startMs, "IP_NOT_ALLOWED");
            writeError(response, HttpStatus.FORBIDDEN, "IP_NOT_ALLOWED", "IP " + remoteIp + " 不在白名单中");
            return false;
        }

        // ========== 6. Scope 校验（app 中心：Key 只认证，能调什么由归属应用决定） ==========
        String scriptCode = extractScriptCode(request);
        AppDO app = resolveApp(entity);
        if (app != null) {
            if (app.getStatus() == null || app.getStatus() != 1) {
                recordLog(request, entity, scriptCode, null, remoteIp, false, 403, startMs, "APP_DISABLED");
                writeError(response, HttpStatus.FORBIDDEN, "APP_DISABLED", "应用已禁用: " + app.getAppCode());
                return false;
            }
            if (!appService.verifyScope(app, scriptCode)) {
                recordLog(request, entity, scriptCode, null, remoteIp, false, 403, startMs, "SCOPE_NOT_ALLOWED");
                writeError(response, HttpStatus.FORBIDDEN, "SCOPE_NOT_ALLOWED",
                        "应用 " + app.getAppCode() + " 无权访问该脚本");
                return false;
            }
        } else if (!apiKeyService.verifyScope(entity, scriptCode)) {
            // 兜底：app_id 与按 appName 反查都找不到应用（init.sql 回填前的存量库），退回 Key 自己的 scope
            recordLog(request, entity, scriptCode, null, remoteIp, false, 403, startMs, "SCOPE_NOT_ALLOWED");
            writeError(response, HttpStatus.FORBIDDEN, "SCOPE_NOT_ALLOWED", "API Key 无权访问该脚本");
            return false;
        }

        // ========== 7. 配额限流 ==========
        Long retryAfter = quotaService.tryAcquire(entity.getId());
        if (retryAfter != null) {
            recordLog(request, entity, scriptCode, null, remoteIp, false, 429, startMs, "QUOTA_EXCEEDED");
            writeErrorWithRetry(response, retryAfter, "QUOTA_EXCEEDED", "配额已耗尽, 请稍后重试");
            return false;
        }

        // ========== 8. 签名校验 ==========
        String timestamp = extractHeader(request, "X-Timestamp");
        String signature = extractHeader(request, "X-Signature");
        if (timestamp != null && signature != null) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String body = readBody(request);
            if (!apiKeyService.verifySignature(entity, method, path, timestamp, body, signature)) {
                recordLog(request, entity, scriptCode, null, remoteIp, false, 401, startMs, "INVALID_SIGNATURE");
                writeError(response, HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "签名校验失败");
                return false;
            }
        }

        // ========== 9. 配额预警检查 ==========
        QuotaDO quota = quotaService.getByApiKeyId(entity.getId());
        if (quota != null && quotaService.shouldAlert(quota)) {
            log.warn("[FEATURE051] 配额预警: apiKeyId={}, appName={}, used={}/{}, threshold={}%",
                    entity.getId(), entity.getAppName(), quota.getUsedToday(),
                    quota.getMaxPerDay(), quota.getAlertThreshold());
            quotaService.markAlerted(quota.getId());
        }

        // ========== 10. 上下文注入 ==========
        request.setAttribute("z.apiKeyId", entity.getId());
        request.setAttribute("z.appName", entity.getAppName());
        request.setAttribute("z.apiKeyEntity", entity);
        request.setAttribute("z.startMs", startMs);
        request.setAttribute("z.scriptCode", scriptCode);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startMs = (Long) request.getAttribute("z.startMs");
        if (startMs == null) {
            return;
        }
        ApiKeyDO entity = (ApiKeyDO) request.getAttribute("z.apiKeyEntity");
        if (entity == null) {
            return;
        }

        // 记录成功调用日志
        int status = response.getStatus();
        recordLog(request, entity,
                (String) request.getAttribute("z.scriptCode"),
                null,
                getClientIp(request),
                status >= 200 && status < 400,
                status,
                startMs,
                ex == null ? null : ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }

    private void recordLog(HttpServletRequest request, ApiKeyDO entity, String scriptCode,
                           String dslType, String clientIp, boolean success,
                           int statusCode, long startMs, String errorMessage) {
        try {
            InvokeLogDO log2 = new InvokeLogDO();
            log2.setApiKeyId(entity == null ? null : entity.getId());
            log2.setAppName(entity == null ? null : entity.getAppName());
            log2.setScriptCode(scriptCode != null ? scriptCode : extractScriptCode(request));
            log2.setDslType(dslType);
            log2.setClientIp(clientIp != null ? clientIp : getClientIp(request));
            log2.setHttpMethod(request.getMethod());
            log2.setPath(request.getRequestURI());
            log2.setRequestSize(request.getContentLength() > 0 ? (int) request.getContentLength() : null);
            log2.setSuccess(success ? 1 : 0);
            log2.setStatusCode(statusCode);
            log2.setCostMs(System.currentTimeMillis() - startMs);
            log2.setErrorMessage(errorMessage);
            invokeLogService.recordAsync(log2);
        } catch (Exception ignored) {
            // 日志失败不影响主流程
        }
    }

    /** 动态路由里脚本标识紧跟在这几个前缀之后；取到下一段为止。 */
    private static final String[] SCRIPT_CODE_MARKERS = {"/script-run/", "/run/", "/mock/"};

    @Autowired
    private AppService appService;

    /** Key 归属的应用：先按 app_id，再按 appName（= appCode）反查；都没有才返回 null（走 Key 自身 scope）。 */
    private AppDO resolveApp(ApiKeyDO entity) {
        if (entity.getAppId() != null) {
            AppDO byId = appService.getById(entity.getAppId());
            if (byId != null) {
                return byId;
            }
        }
        return appService.getByAppCode(entity.getAppName());
    }

    private String extractScriptCode(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String marker : SCRIPT_CODE_MARKERS) {
            int idx = uri.indexOf(marker);
            if (idx < 0) {
                continue;
            }
            int start = idx + marker.length();
            int end = uri.indexOf('/', start);
            String code = uri.substring(start, end < 0 ? uri.length() : end);
            if (!code.isEmpty()) {
                return code;
            }
        }
        return null;
    }

    private String extractHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v != null) {
            return v;
        }

        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String h = headers.nextElement();
            if (h.equalsIgnoreCase(name.replace("-", "_"))) {
                return request.getHeader(h);
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }

    private String readBody(HttpServletRequest request) {
        // 读 input stream。POST body 只能读一次，若后续 Controller 需要读取，应由 Filter 包装为
        // ContentCachingRequestWrapper（需 Spring Boot 自动配置，此处暂不实现）。
        // 当前实现：仅在 HMAC 签名验证时读取 body（可选功能），且读取后 Controller 将无法再次读取。
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (Exception ignored) {
            // ignore
        }
        try (java.io.BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws Exception {
        response.setStatus(status.value());
        writeJsonBody(response, code, message, null);
    }

    private void writeErrorWithRetry(HttpServletResponse response, Long retryAfter, String code, String message) throws Exception {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        writeJsonBody(response, code, message, retryAfter);
    }

    private void writeJsonBody(HttpServletResponse response, String code, String message, Long retryAfter) throws Exception {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("errorCode", code);
        body.put("message", code + ": " + message);
        if (retryAfter != null) {
            body.put("retryAfter", retryAfter);
        }
        try (PrintWriter w = response.getWriter()) {
            w.write(mapper.writeValueAsString(body));
        }
    }
}