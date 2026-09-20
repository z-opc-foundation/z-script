package com.zifang.z.script.scene.http.web;

import com.zifang.util.core.lang.RandomUtil;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.script.scene.http.engine.ChainExecutionResult;
import com.zifang.z.script.scene.http.engine.HttpChainContext;
import com.zifang.z.script.scene.http.engine.HttpChainExecutor;
import com.zifang.z.script.scene.http.engine.StepDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 链路执行入口（REST 控制器）。
 *
 * <p>FEATURE052 §6 中描述的链路执行能力在底层 z-script-scene-http 模块以 REST 形式暴露：
 * <ul>
 *   <li>POST /api/scene-http/chain/execute — 执行链路（带 baseUrl + 变量 + 可选 mock 响应）</li>
 *   <li>POST /api/scene-http/chain/preview — 干跑模式（不真正发请求，只看模板替换结果）</li>
 *   <li>GET  /api/scene-http/health — 健康检查</li>
 * </ul>
 *
 * <p>典型调用方：z-qa 后端（RunController 触发执行时调用此处，作为通用 HTTP 链路引擎）。
 */
@RestController
@RequestMapping("/api/scene-http")
public class HttpChainController {

    @Autowired
    private HttpChainExecutor executor;

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    /**
     * 同步执行链路。
     * 请求体：{ chainId?, baseUrl?, mockEndpointResponses?:{ code: jsonString }, variables?:{...}, steps:[StepDefinition...] }
     * 响应：ChainExecutionResult (含 steps 全量溯源)
     */
    @PostMapping("/chain/execute")
    public Map<String, Object> execute(@RequestBody Map<String, Object> body) {
        long start = System.currentTimeMillis();
        Map<String, Object> resp = new HashMap<>();

        HttpChainContext ctx = new HttpChainContext();
        ctx.setChainId(body.get("chainId") == null ? "chain-" + RandomUtil.uuidShort(8) : String.valueOf(body.get("chainId")));
        ctx.setBaseUrl(asString(body.get("baseUrl")));

        // mock 端点响应 (targetMode=mock 时查表)
        Object mocks = body.get("mockEndpointResponses");
        if (mocks instanceof Map) {
            Map<String, Object> raw = (Map<String, Object>) mocks;
            Map<String, String> converted = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                converted.put(e.getKey(), e.getValue() == null ? null : e.getValue().toString());
            }
            ctx.setMockEndpointResponses(converted);
        }

        // 初始变量
        Object vars = body.get("variables");
        if (vars instanceof Map) {
            Map<String, Object> raw = (Map<String, Object>) vars;
            Map<String, Object> converted = new LinkedHashMap<>(raw);
            ctx.setVariables(converted);
        }

        Object abort = body.get("abortOnFirstFail");
        if (abort instanceof Boolean) {
            ctx.setAbortOnFirstFail((Boolean) abort);
        }


        // 解析步骤
        Object stepsRaw = body.get("steps");
        List<StepDefinition> steps = HttpChainExecutor.parseSteps(JsonUtil.toJson(stepsRaw));

        ChainExecutionResult chain;
        try {
            chain = executor.execute(steps, ctx);
        } catch (Exception ex) {
            resp.put("code", 500);
            resp.put("ok", false);
            resp.put("error", ex.getMessage());
            resp.put("durationMs", System.currentTimeMillis() - start);
            return resp;
        }

        resp.put("code", 200);
        resp.put("ok", true);
        resp.put("durationMs", System.currentTimeMillis() - start);
        resp.put("result", JsonUtil.toJson(chain));
        return resp;
    }

    /**
     * 干跑模式：只解析/校验步骤，不真正发请求。
     * 方便前端调试。
     */
    @PostMapping("/chain/preview")
    public Map<String, Object> preview(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new HashMap<>();
        Object stepsRaw = body.get("steps");
        try {
            List<StepDefinition> steps = HttpChainExecutor.parseSteps(JsonUtil.toJson(stepsRaw));
            resp.put("code", 200);
            resp.put("ok", true);
            resp.put("stepCount", steps.size());
            resp.put("steps", JsonUtil.toJson(steps));
        } catch (Exception ex) {
            resp.put("code", 400);
            resp.put("ok", false);
            resp.put("error", ex.getMessage());
        }
        return resp;
    }

    @org.springframework.web.bind.annotation.GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("ok", true);
        resp.put("module", "z-script-scene-http");
        resp.put("timestamp", System.currentTimeMillis());
        return resp;
    }
}
