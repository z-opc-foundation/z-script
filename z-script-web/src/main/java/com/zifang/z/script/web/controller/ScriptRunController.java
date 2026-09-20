package com.zifang.z.script.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.core.domain.service.ScriptService;
import com.zifang.z.script.engine.ExecutionResult;
import com.zifang.z.script.engine.ScriptEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 脚本运行时控制器（历史路由）。
 * <p>
 * API 基础路径: /run/{scriptCode}
 * 所属模块: z-script-web
 * 鉴权: app + AK（{@code X-Api-Key}）。该路由不在 /api 前缀下，由 ZScriptWebMvcConfig 显式点名拦截。
 * <p>
 * 与 {@link ScriptHttpDispatchController} 的 {@code /api/script-run/{code}} 是同一件事的两扇门；
 * 新接入请用 /api 前缀那组（返回体是裸的 success/data/errorMessage/durationMs，且支持任意 HTTP 方法）。
 * 这里保留 GET/POST + Result 包装，是给早期接入方的兼容入口。
 * <p>
 * 曾经还有第三条 {@code /mock/**}：它用 {@code getRequestURI().substring(5)} 剥前缀，
 * 在 admin 挂了 context-path=/script 之后永远算错 mockPath（{@code /mock/x} → {@code /mock/x}），
 * 与 {@link MockDispatchController} 的 {@code /api/mock/**} 功能重复且从未生效，已删除。
 */
@Tag(name = "脚本运行时")
@RestController
public class ScriptRunController {

    @Resource
    private ScriptService scriptService;
    @Resource
    private ScriptEngine scriptEngine;

    /**
     * 执行已发布为 HTTP 的脚本。GET / POST 共用入口，将请求体与 query 参数合并作为执行参数。
     *
     * @param scriptCode 脚本编码
     * @param body       请求体参数（可选）
     * @param request    原始 HttpServletRequest，用于读取 query 参数
     * @return Result 包装的成功结果（data 为脚本执行结果），脚本不存在或未发布时返回 code=404
     */
    @Operation(summary = "执行已发布脚本")
    @RequestMapping(value = "/run/{scriptCode}", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<Object> runScript(@PathVariable String scriptCode,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        Script script = scriptService.getByScriptCode(scriptCode);
        if (script == null || !"HTTP".equals(script.getExposeAs()) && !"BOTH".equals(script.getExposeAs())) {
            return Result.<Object>success(errorMap("脚本不存在或未发布HTTP: " + scriptCode)).code(404);
        }

        Map<String, Object> params = body != null ? body : new HashMap<>();
        // Add query params
        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0) {
                params.put(k, v[0]);
            }

        });

        ExecutionResult result = scriptEngine.execute(script, params);
        if (result.isSuccess()) {
            return Result.<Object>success(result.getData());
        } else {
            return Result.<Object>success(errorMap(result.getErrorMessage())).code(500);
        }
    }

    /**
     * 构造统一格式的错误响应。
     *
     * @param message 错误提示信息
     * @return 包含 success=false 与 message 的 Map
     */
    private Map<String, Object> errorMap(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }
}
