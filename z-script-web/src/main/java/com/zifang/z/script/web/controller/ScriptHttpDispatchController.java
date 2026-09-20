package com.zifang.z.script.web.controller;

import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.core.domain.service.ScriptService;
import com.zifang.z.script.engine.ExecutionResult;
import com.zifang.z.script.engine.ScriptEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 把已发布为 HTTP 的脚本暴露成可调用端点。
 * <p>
 * API 基础路径: /api/script-run
 * 所属模块: z-script-web
 * 鉴权: app + AK（{@code X-Api-Key}），由 ApiKeyAuthInterceptor 在 /api/** 上统一校验；
 *       本控制器自身不做权限判断，scope=SPECIFIC 的 Key 只能调 allowedScripts 里的 scriptCode
 * <p>
 * 路径: /api/script-run/{scriptCode}
 * <ul>
 *   <li>接受任意 HTTP 方法</li>
 *   <li>params 通过 query / path / body 三种途径传入，统一合并到调用参数</li>
 *   <li>返回 ScriptEngine.execute() 的结果 (data / errorMessage / durationMs)</li>
 * </ul>
 */
@Tag(name = "脚本 HTTP 暴露")
@RestController
@RequestMapping("/api/script-run")
public class ScriptHttpDispatchController {

    @Resource
    private ScriptService scriptService;

    @Resource
    private ScriptEngine scriptEngine;

    /**
     * 调用已发布为 HTTP 的脚本。queryParams 与 bodyParams 合并为执行参数（body 优先）。
     *
     * @param scriptCode  脚本编码
     * @param queryParams URL 查询参数
     * @param headers     请求头
     * @param bodyParams  请求体参数（与 queryParams 合并）
     * @return 包含 success、data、errorMessage、durationMs 的结果 Map
     */
    @Operation(summary = "调用已发布为 HTTP 的脚本")
    @RequestMapping("/{scriptCode}/**")
    public Map<String, Object> run(@PathVariable String scriptCode,
                                   @RequestParam(required = false) Map<String, Object> queryParams,
                                   @RequestHeader Map<String, String> headers,
                                   @RequestBody(required = false) Map<String, Object> bodyParams) {
        Script script = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (script == null) {
            result.put("success", false);
            result.put("errorMessage", "脚本不存在: " + scriptCode);
            return result;
        }
        if (!"HTTP".equals(script.getExposeAs()) && !"BOTH".equals(script.getExposeAs())) {
            result.put("success", false);
            result.put("errorMessage", "脚本未发布为 HTTP，当前协议: " + script.getExposeAs());
            return result;
        }

        // 合并三路参数: query < headers < body (body 优先)
        Map<String, Object> params = new HashMap<>();
        if (queryParams != null) {
            params.putAll(queryParams);
        }

        if (bodyParams != null) {
            params.putAll(bodyParams);
        }

        ExecutionResult exec = scriptEngine.execute(script, params);

        result.put("success", exec.isSuccess());
        result.put("data", exec.getData());
        result.put("errorMessage", exec.getErrorMessage());
        result.put("durationMs", exec.getDurationMs());
        return result;
    }

    /**
     * 简化版：直接以 POST Body 作为参数调用已发布为 HTTP 的脚本。
     *
     * @param scriptCode 脚本编码
     * @param params     POST Body 参数
     * @return 与 {@link #run} 一致的结果 Map
     */
    @Operation(summary = "简化版: 直接 POST 参数调用")
    @PostMapping("/{scriptCode}")
    public Map<String, Object> runPost(@PathVariable String scriptCode,
                                       @RequestBody(required = false) Map<String, Object> params) {
        return run(scriptCode, params, new HashMap<>(), params);
    }

    /**
     * 简化版：GET 形式调用已发布为 HTTP 的脚本，参数走 query string。
     *
     * @param scriptCode 脚本编码
     * @param params     URL 查询参数
     * @return 与 {@link #run} 一致的结果 Map
     */
    @Operation(summary = "GET 形式调用 (参数走 query string)")
    @GetMapping("/{scriptCode}")
    public Map<String, Object> runGet(@PathVariable String scriptCode,
                                      @RequestParam(required = false) Map<String, Object> params) {
        return run(scriptCode, params, new HashMap<>(), null);
    }
}
