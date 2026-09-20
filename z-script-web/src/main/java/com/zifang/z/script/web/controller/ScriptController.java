package com.zifang.z.script.web.controller;

import com.zifang.util.http.base.define.RequestMethod;
import com.zifang.util.http.base.pojo.HttpRequestBody;
import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.http.base.pojo.HttpRequestLine;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.http.parser.curl.CurlParser;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonArray;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.core.domain.service.ScriptService;
import com.zifang.z.script.engine.ApiBridgeDefinition;
import com.zifang.z.script.engine.ExecutionResult;
import com.zifang.z.script.engine.ScriptEngine;
import com.zifang.z.script.engine.ScriptMcpAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 脚本管理控制器。
 * <p>
 * API 基础路径: /api/script
 * 所属模块: z-script-web
 * 鉴权: 部分接口读取 {@code X-User-Id} Header 标识创建者
 * <p>
 * 主要端点:
 * <ul>
 *   <li>GET    /list                  — 脚本列表，可按 dslType / exposeAs 过滤</li>
 *   <li>GET    /byCode?scriptCode={code} — 脚本详情</li>
 *   <li>POST   /                      — 创建脚本</li>
 *   <li>PUT    /?scriptCode={code}     — 更新脚本</li>
 *   <li>DELETE /?scriptCode={code}     — 删除脚本</li>
 *   <li>POST   /run?scriptCode={code}  — Ad-hoc 执行脚本</li>
 *   <li>POST   /publish?scriptCode={code}  — 发布脚本（暴露为 HTTP / MCP）</li>
 *   <li>POST   /unpublish?scriptCode={code} — 取消发布</li>
 *   <li>POST   /import-curl           — 从 curl 一键创建 API_BRIDGE 脚本</li>
 *   <li>POST   /preview-mapping       — 预览 curl + outputMapping 的执行结果</li>
 *   <li>POST   /import-openapi        — 从 OpenAPI spec 批量导入脚本</li>
 * </ul>
 */
@Tag(name = "脚本管理")
@RestController
@RequestMapping("/api/script")
public class ScriptController {

    @Resource
    private ScriptService scriptService;

    @Resource
    private ScriptEngine scriptEngine;

    @Resource
    private ApplicationContext applicationContext;

    /**
     * 查询脚本列表。可选按 dslType 或 exposeAs 过滤。
     *
     * @param dslType  DSL 类型，可选
     * @param exposeAs 暴露方式，可选
     * @return 包含 success 与 data 的 Map；data 为 Script 列表
     */
    @Operation(summary = "脚本列表")
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String dslType,
            @RequestParam(required = false) String exposeAs) {
        List<Script> scripts;
        if (dslType != null) {
            scripts = scriptService.listByDslType(dslType);
        } else if (exposeAs != null) {
            scripts = scriptService.listByExposeAs(exposeAs);
        } else {
            scripts = scriptService.list();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", scripts);
        return result;
    }

    /**
     * 按 scriptCode 查询脚本详情。
     *
     * @param scriptCode 脚本编码
     * @return 存在时返回 success=true 与 data；不存在时返回 success=false 与提示信息
     */
    @Operation(summary = "脚本详情")
    @GetMapping("/byCode")
    public Map<String, Object> get(@RequestParam String scriptCode) {
        Script script = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (script != null) {
            result.put("success", true);
            result.put("data", script);
        } else {
            result.put("success", false);
            result.put("message", "脚本不存在: " + scriptCode);
        }
        return result;
    }

    /**
     * 创建脚本。根据 exposeAs 自动生成 httpPath 与 mcpToolName；creatorId 取自 X-User-Id Header。
     *
     * @param script 待创建脚本实体
     * @param userId 创建者 ID（可选，来自 Header X-User-Id）
     * @return 包含 success、data、message 的结果 Map
     */
    @Operation(summary = "创建脚本")
    @PostMapping
    public Map<String, Object> create(@RequestBody Script script,
                                      @RequestHeader(value = "X-User-Id", required = false) String userId) {
        script.setCreatorId(userId);
        // Auto-generate HTTP path if expose_as includes HTTP
        if ("HTTP".equals(script.getExposeAs()) || "BOTH".equals(script.getExposeAs())) {
            script.setHttpPath("/run/" + script.getScriptCode());
        }
        // Auto-generate MCP tool name if expose_as includes MCP
        if ("MCP".equals(script.getExposeAs()) || "BOTH".equals(script.getExposeAs())) {
            script.setMcpToolName("script_" + script.getScriptCode());
        }
        boolean ok = scriptService.saveScript(script);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("data", ok ? script : null);
        result.put("message", ok ? "创建成功" : "创建失败，scriptCode 可能已存在");
        return result;
    }

    /**
     * 按 scriptCode 更新脚本。
     *
     * @param scriptCode 目标脚本编码
     * @param script     新脚本实体
     * @return 成功时返回 success=true 与 data；脚本不存在时返回 success=false 与提示信息
     */
    @Operation(summary = "更新脚本")
    @PutMapping
    public Map<String, Object> update(@RequestParam String scriptCode, @RequestBody Script script) {
        Script existing = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (existing == null) {
            result.put("success", false);
            result.put("message", "脚本不存在: " + scriptCode);
            return result;
        }
        script.setId(existing.getId());
        boolean ok = scriptService.updateScript(script);
        result.put("success", ok);
        result.put("data", ok ? scriptService.getByScriptCode(scriptCode) : null);
        return result;
    }

    /**
     * 按 scriptCode 删除脚本。
     *
     * @param scriptCode 目标脚本编码
     * @return success 表示是否删除成功；message 提示具体原因
     */
    @Operation(summary = "删除脚本")
    @DeleteMapping
    public Map<String, Object> delete(@RequestParam String scriptCode) {
        boolean ok = scriptService.deleteByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "删除成功" : "脚本不存在");
        return result;
    }

    /**
     * Ad-hoc 执行脚本。
     *
     * @param scriptCode 脚本编码
     * @param params     执行参数，可选
     * @return 包含 success、data、errorMessage、durationMs 的结果 Map；脚本不存在时返回 success=false 与提示信息
     */
    @Operation(summary = "执行脚本(Ad-hoc)")
    @PostMapping("/run")
    public Map<String, Object> run(@RequestParam String scriptCode,
                                   @RequestBody(required = false) Map<String, Object> params) {
        Script script = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (script == null) {
            result.put("success", false);
            result.put("message", "脚本不存在: " + scriptCode);
            return result;
        }
        ExecutionResult execResult = scriptEngine.execute(script, params);
        result.put("success", execResult.isSuccess());
        result.put("data", execResult.getData());
        result.put("errorMessage", execResult.getErrorMessage());
        result.put("durationMs", execResult.getDurationMs());
        return result;
    }

    /**
     * 发布脚本，将其暴露为 HTTP、MCP 或 BOTH。{@code exposeAs} 包含 MCP 时会发布 ScriptPublishEvent 以注册 MCP tool。
     *
     * @param scriptCode 脚本编码
     * @param exposeAs   暴露协议，{@code HTTP}/{@code MCP}/{@code BOTH}，默认 {@code HTTP}
     * @return 包含 success、message、httpPath、mcpToolName 的结果 Map；脚本不存在时返回 success=false
     */
    @Operation(summary = "发布脚本(暴露为HTTP/MCP)")
    @PostMapping("/publish")
    public Map<String, Object> publish(@RequestParam String scriptCode,
                                       @RequestParam(defaultValue = "HTTP") String exposeAs) {
        Script script = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (script == null) {
            result.put("success", false);
            result.put("message", "脚本不存在: " + scriptCode);
            return result;
        }
        script.setExposeAs(exposeAs);
        script.setStatus(1);
        if ("HTTP".equals(exposeAs) || "BOTH".equals(exposeAs)) {
            script.setHttpPath("/run/" + scriptCode);
        }
        if ("MCP".equals(exposeAs) || "BOTH".equals(exposeAs)) {
            script.setMcpToolName("script_" + scriptCode);
        }
        scriptService.updateScript(script);
        // FEATURE030: notify ScriptMcpAdapter to register this script as an MCP tool
        if ("MCP".equals(exposeAs) || "BOTH".equals(exposeAs)) {
            applicationContext.publishEvent(new ScriptMcpAdapter.ScriptPublishEvent(this, scriptCode));
        }
        result.put("success", true);
        result.put("message", "发布成功，协议: " + exposeAs);
        result.put("httpPath", script.getHttpPath());
        result.put("mcpToolName", script.getMcpToolName());
        return result;
    }

    /**
     * 取消脚本发布：{@code status} 归 0（回到未上线），exposeAs 置为 NONE，清空 httpPath 与
     * mcpToolName，并发布 ScriptUnpublishEvent 以注销 MCP tool。
     * <p>
     * status 必须一起改：只有 {@link #publish} 会把它置 1，若这里不回滚，脚本就永远停在
     * 「已上线」，既没有别的机会回到草稿态，控制台也无法再次上线（上线/下线按 status 判断）。
     *
     * @param scriptCode 脚本编码
     * @return 包含 success 与 message 的结果 Map；脚本不存在时返回 success=false
     */
    @Operation(summary = "取消发布")
    @PostMapping("/unpublish")
    public Map<String, Object> unpublish(@RequestParam String scriptCode) {
        Script script = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (script == null) {
            result.put("success", false);
            result.put("message", "脚本不存在: " + scriptCode);
            return result;
        }
        script.setStatus(0);
        script.setExposeAs("NONE");
        script.setHttpPath(null);
        script.setMcpToolName(null);
        scriptService.updateScript(script);
        // FEATURE030: notify ScriptMcpAdapter to unregister this script from MCP tools
        applicationContext.publishEvent(new ScriptMcpAdapter.ScriptUnpublishEvent(this, scriptCode));
        result.put("success", true);
        result.put("message", "已取消发布");
        return result;
    }

    // ============ HTTP → MCP 一键转换 ============

    /**
     * 从 curl 命令一键创建 API_BRIDGE 脚本。
     * <p>
     * 入参:
     * curl           原始 curl 字符串 (必填)
     * scriptCode     脚本编码 (可选，未传则从 url 生成)
     * scriptName     脚本名称 (可选)
     * description    描述 (可选)
     * inputParams    自定义输入参数 (可选，默认从 curl 中的 ${var} 推断)
     * outputMapping  自定义输出映射 (可选，默认空数组 —— 用户后续在 UI 配)
     */
    @Operation(summary = "从 curl 导入并创建 API_BRIDGE 脚本")
    @PostMapping("/import-curl")
    public Map<String, Object> importCurl(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String curl = (String) body.get("curl");
        if (curl == null || curl.trim().isEmpty()) {
            return error("curl 不能为空");
        }
        String scriptCode = (String) body.get("scriptCode");
        String scriptName = (String) body.get("scriptName");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputMapping = (List<Map<String, Object>>) body.get("outputMapping");

        // 1) curl → HttpRequestDefinition (z-util-http CurlParser)
        HttpRequestDefinition req;
        try {
            req = CurlParser.parse(curl);
        } catch (Exception e) {
            return error("解析 curl 失败: " + e.getMessage());
        }
        if (req.getHttpRequestLine() == null || req.getHttpRequestLine().getUrl() == null) {
            return error("curl 中未找到 URL");
        }

        // 2) 推断 inputParams: 扫描 url/headers/body 里的 ${var}
        Set<String> paramNames = new LinkedHashSet<>();
        String url = req.getHttpRequestLine().getUrl();
        scanPlaceholders(url, paramNames);
        if (req.getHttpRequestHeader() != null) {
            for (Object v : req.getHttpRequestHeader().values()) {
                if (v != null) {
                    scanPlaceholders(v.toString(), paramNames);
                }

            }
        }
        if (req.getHttpRequestBody() != null && req.getHttpRequestBody().getBody() != null) {
            scanPlaceholders(new String(req.getHttpRequestBody().getBody(), java.nio.charset.StandardCharsets.UTF_8), paramNames);
        }

        List<ApiBridgeDefinition.InputParam> inputParams = new ArrayList<>();
        for (String name : paramNames) {
            ApiBridgeDefinition.InputParam ip = new ApiBridgeDefinition.InputParam();
            ip.setName(name);
            ip.setIn(name.equalsIgnoreCase("path") ? "path" : "query");
            ip.setRequired(true);
            inputParams.add(ip);
        }

        // 3) 构造 ApiBridgeDefinition
        ApiBridgeDefinition def = new ApiBridgeDefinition();
        def.setRequest(req);
        def.setInputParams(inputParams);
        if (outputMapping != null) {
            for (Map<String, Object> om : outputMapping) {
                ApiBridgeDefinition.FieldMapping fm = new ApiBridgeDefinition.FieldMapping();
                fm.setName((String) om.get("name"));
                fm.setType(om.get("type") == null ? "string" : (String) om.get("type"));
                fm.setJsonPath((String) om.get("jsonPath"));
                fm.setDescription((String) om.getOrDefault("description", ""));
                def.getOutputMapping().add(fm);
            }
        }

        // 4) 默认 scriptCode/scriptName
        if (scriptCode == null || scriptCode.isEmpty()) {
            scriptCode = "bridge_" + Math.abs((req.getHttpRequestLine().getUrl().hashCode()));
        }
        if (scriptName == null || scriptName.isEmpty()) {
            scriptName = "Bridge: " + req.getHttpRequestLine().getRequestMethod() + " " + req.getHttpRequestLine().getUrl();
        }

        // 5) 持久化
        Script script = new Script();
        script.setScriptCode(scriptCode);
        script.setScriptName(scriptName);
        script.setDslType("API_BRIDGE");
        script.setSourceCode(def.toJson());
        script.setExposeAs("NONE");
        script.setDescription(description);
        script.setCreatorId(userId);
        script.setStatus(0);
        boolean ok = scriptService.saveScript(script);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("data", ok ? script : null);
        result.put("scriptCode", scriptCode);
        result.put("inputParams", inputParams);
        result.put("outputMapping", def.getOutputMapping());
        result.put("request", req);
        result.put("message", ok ? "脚本创建成功，请配置 outputMapping 后发布" :
                "创建失败，可能 scriptCode 已存在");
        return result;
    }

    /**
     * 试运行一次 curl + outputMapping，立刻看到抽取出的字段。
     * 不写库，仅用于前端预览。
     */
    @Operation(summary = "预览 curl + outputMapping 的执行结果")
    @PostMapping("/preview-mapping")
    public Map<String, Object> previewMapping(@RequestBody Map<String, Object> body) {
        String curl = (String) body.get("curl");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputMapping = (List<Map<String, Object>>) body.get("outputMapping");
        @SuppressWarnings("unchecked")
        Map<String, Object> sampleParams = (Map<String, Object>) body.get("sampleParams");

        if (curl == null || curl.trim().isEmpty()) {
            return error("curl 不能为空");
        }

        HttpRequestDefinition req;
        try {
            req = CurlParser.parse(curl);
        } catch (Exception e) {
            return error("解析 curl 失败: " + e.getMessage());
        }
        ApiBridgeDefinition def = new ApiBridgeDefinition();
        def.setRequest(req);
        if (outputMapping != null) {
            for (Map<String, Object> om : outputMapping) {
                ApiBridgeDefinition.FieldMapping fm = new ApiBridgeDefinition.FieldMapping();
                fm.setName((String) om.get("name"));
                fm.setJsonPath((String) om.get("jsonPath"));
                fm.setType(om.get("type") == null ? "string" : (String) om.get("type"));
                def.getOutputMapping().add(fm);
            }
        }
        HttpRequestDefinition materialized = def.materialize(sampleParams);
        HttpExecutionResult result = HttpExecutor.getDefault().execute(materialized);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", result.getStatus());
        resp.put("success", result.isSuccess());
        resp.put("durationMs", result.getDurationMs());
        resp.put("error", result.getError());
        resp.put("bodyRaw", result.getBody());
        if (result.isSuccess()) {
            Object parsed = result.getBodyObject();
            if (parsed == null && result.getBody() != null) {
                try {
                    parsed = JsonUtil.fromJson(result.getBody(), Object.class);
                } catch (Exception ignore) {
                }
            }
            // 把 JsonObject 序列化为 String 避免 Jackson HttpMessageConversionException
            // (com.zifang.util.json.model.JsonObject 不可直接 JSON 化)
            // FEATURE008 P1 修复 2026-06-25
            if (parsed != null) {
                try {
                    resp.put("bodyObjectJson", JsonUtil.toJson(parsed));
                } catch (Exception e) {
                    resp.put("bodyObjectJson", parsed.toString());
                }
            }
            resp.put("mapped", def.extractOutput(parsed));
        }
        return resp;
    }

    /**
     * 从 OpenAPI spec (JSON) 批量生成 API_BRIDGE 脚本。
     * 入参:
     * openApiJson    OpenAPI 文档 (JSON 字符串)
     * prefix         scriptCode 前缀 (默认 "openapi_")
     */
    @Operation(summary = "从 OpenAPI spec 批量导入脚本")
    @PostMapping("/import-openapi")
    public Map<String, Object> importOpenApi(@RequestBody Map<String, Object> body,
                                             @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String openApiJson = (String) body.get("openApiJson");
        String prefix = (String) body.getOrDefault("prefix", "openapi_");
        if (openApiJson == null || openApiJson.isEmpty()) {
            return error("openApiJson 不能为空");
        }
        JsonObject spec;
        try {
            spec = JsonUtil.parseObject(openApiJson);
        } catch (Exception e) {
            return error("OpenAPI 不是合法 JSON: " + e.getMessage());
        }
        String baseUrl = "";
        if (spec.containsKey("servers")) {
            JsonArray servers = spec.getJsonArray("servers");
            if (servers != null && !servers.isEmpty()) {
                baseUrl = servers.getJsonObject(0).getString("url");
            }
        }
        if (baseUrl.isEmpty() && spec.containsKey("host")) {
            baseUrl = spec.getString("host");
        }

        JsonObject paths = spec.getJsonObject("paths");
        List<Map<String, Object>> created = new ArrayList<>();
        if (paths == null) {
            return error("OpenAPI 中没有 paths 段");
        }
        for (java.util.Map.Entry<String, Object> pathEntry : paths.getAllKeyValue()) {
            String path = pathEntry.getKey();
            JsonObject methods = paths.getJsonObject(path);
            for (String method : new String[]{"get", "post", "put", "delete", "patch"}) {
                JsonObject op = methods.getJsonObject(method);
                if (op == null) {
                    continue;
                }
                String opId = op.getString("operationId");
                String summary = op.getString("summary");
                String scriptCode = prefix + (opId != null ? opId :
                        method.toUpperCase() + "_" + path.replaceAll("[^a-zA-Z0-9]", "_"));
                String scriptName = summary != null ? summary : (method.toUpperCase() + " " + path);

                // 构造 HttpRequestDefinition
                HttpRequestDefinition req = new HttpRequestDefinition();
                HttpRequestLine line = new HttpRequestLine();
                line.setRequestMethod(RequestMethod.valueOf(method.toUpperCase()));
                String fullUrl = (baseUrl == null ? "" : baseUrl) + path;
                line.setUrl(fullUrl);
                req.setHttpRequestLine(line);

                // 输入参数 (path 参数 → path 替换; query 参数 → query)
                List<ApiBridgeDefinition.InputParam> inputParams = new ArrayList<>();
                JsonArray parameters = op.getJsonArray("parameters");
                if (parameters != null) {
                    for (int i = 0; i < parameters.size(); i++) {
                        JsonObject p = parameters.getJsonObject(i);
                        String in = p.getString("in");
                        String name = p.getString("name");
                        boolean required = Boolean.TRUE.equals(p.getBoolean("required"));
                        if ("path".equals(in) && name != null) {
                            fullUrl = fullUrl.replace("{" + name + "}", "${" + name + "}");
                        }
                        ApiBridgeDefinition.InputParam ip = new ApiBridgeDefinition.InputParam();
                        ip.setName(name);
                        ip.setIn(in);
                        ip.setRequired(required);
                        ip.setDescription(p.getString("description"));
                        inputParams.add(ip);
                    }
                }
                line.setUrl(fullUrl);

                // requestBody
                JsonObject rb = op.getJsonObject("requestBody");
                if (rb != null) {
                    JsonObject content = rb.getJsonObject("content");
                    if (content != null && content.getJsonObject("application/json") != null) {
                        HttpRequestBody body0 = new HttpRequestBody();
                        Object schema = content.getJsonObject("application/json").get("schema");
                        body0.setBody((schema == null ? "{}" : schema.toString()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        req.setHttpRequestBody(body0);
                    }
                }

                ApiBridgeDefinition def = new ApiBridgeDefinition();
                def.setRequest(req);
                def.setInputParams(inputParams);
                Script script = new Script();
                script.setScriptCode(scriptCode);
                script.setScriptName(scriptName);
                script.setDslType("API_BRIDGE");
                script.setSourceCode(def.toJson());
                script.setDescription(summary);
                script.setCreatorId(userId);
                script.setStatus(0);
                script.setExposeAs("NONE");
                boolean ok = scriptService.saveScript(script);
                Map<String, Object> entry = new HashMap<>();
                entry.put("scriptCode", scriptCode);
                entry.put("scriptName", scriptName);
                entry.put("method", method.toUpperCase());
                entry.put("path", path);
                entry.put("created", ok);
                created.add(entry);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", created.size());
        result.put("items", created);
        return result;
    }

    /**
     * FEATURE051: 复制脚本
     * <p>
     * 复制一份脚本但生成新的 scriptCode (后缀 _copy_时间戳)
     */
    @Operation(summary = "复制脚本")
    @PostMapping("/copy")
    public Map<String, Object> copy(@RequestParam String scriptCode) {
        Script src = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (src == null) {
            result.put("success", false);
            result.put("message", "脚本不存在: " + scriptCode);
            return result;
        }
        Script copy = new Script();
        copy.setScriptCode(scriptCode + "_copy_" + System.currentTimeMillis());
        copy.setScriptName(src.getScriptName() + " (副本)");
        copy.setDslType(src.getDslType());
        copy.setSourceCode(src.getSourceCode());
        copy.setOutputSchema(src.getOutputSchema());
        copy.setDescription(src.getDescription());
        copy.setExposeAs("NONE");
        copy.setStatus(0);
        boolean ok = scriptService.saveScript(copy);
        result.put("success", ok);
        result.put("data", ok ? copy : null);
        result.put("message", ok ? "复制成功" : "复制失败");
        return result;
    }

    /**
     * FEATURE051: 导出脚本 (JSON)
     */
    @Operation(summary = "导出脚本(JSON)")
    @GetMapping("/export")
    public Map<String, Object> export(@RequestParam String scriptCode) {
        Script src = scriptService.getByScriptCode(scriptCode);
        Map<String, Object> result = new HashMap<>();
        if (src == null) {
            result.put("success", false);
            result.put("message", "脚本不存在: " + scriptCode);
            return result;
        }
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("scriptCode", src.getScriptCode());
        exportData.put("scriptName", src.getScriptName());
        exportData.put("dslType", src.getDslType());
        exportData.put("sourceCode", src.getSourceCode());
        exportData.put("outputSchema", src.getOutputSchema());
        exportData.put("description", src.getDescription());
        exportData.put("exportedAt", System.currentTimeMillis());
        exportData.put("version", "1.0");
        result.put("success", true);
        result.put("data", exportData);
        return result;
    }

    /**
     * FEATURE051: 导入脚本 (JSON)
     */
    @Operation(summary = "导入脚本(JSON)")
    @PostMapping("/import")
    public Map<String, Object> importJson(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String scriptCode = (String) body.get("scriptCode");
            String scriptName = (String) body.get("scriptName");
            String dslType = (String) body.get("dslType");
            String sourceCode = (String) body.get("sourceCode");
            if (scriptCode == null || scriptCode.isEmpty()) {
                result.put("success", false);
                result.put("message", "scriptCode 不能为空");
                return result;
            }
            // 检查是否已存在
            if (scriptService.getByScriptCode(scriptCode) != null) {
                // 自动重命名
                scriptCode = scriptCode + "_imp_" + System.currentTimeMillis();
            }
            Script script = new Script();
            script.setScriptCode(scriptCode);
            script.setScriptName(scriptName == null ? scriptCode : scriptName);
            script.setDslType(dslType == null ? "EL" : dslType);
            script.setSourceCode(sourceCode);
            script.setOutputSchema((String) body.get("outputSchema"));
            script.setDescription((String) body.get("description"));
            script.setCreatorId(userId);
            script.setExposeAs("NONE");
            script.setStatus(0);
            boolean ok = scriptService.saveScript(script);
            result.put("success", ok);
            result.put("data", ok ? script : null);
            result.put("message", ok ? "导入成功" : "导入失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 扫描字符串中的 {@code ${var}} 占位符并把变量名收集到 out 中。
     *
     * @param text 待扫描文本
     * @param out  收集到的占位符变量名集合
     */
    private void scanPlaceholders(String text, Set<String> out) {
        if (text == null) {
            return;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}").matcher(text);
        while (m.find()) out.add(m.group(1));
    }

    /**
     * 构造统一格式的错误响应。
     *
     * @param msg 错误提示信息
     * @return 包含 success=false 与 message 的 Map
     */
    private Map<String, Object> error(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", false);
        r.put("message", msg);
        return r;
    }
}
