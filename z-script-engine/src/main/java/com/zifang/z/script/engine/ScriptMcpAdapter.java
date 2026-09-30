package com.zifang.z.script.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.mcp.api.dto.CallToolResult;
import com.zifang.z.mcp.core.registry.McpRegistry;
import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.core.domain.service.ScriptService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Script → MCP 自动桥接适配器 (FEATURE008).
 *
 * <p>脚本以 MCP 方式发布时, {@code ScriptController} 发布 {@link ScriptPublishEvent},
 * 本组件把该脚本挂进 z-mcp 的 {@link McpRegistry}，执行体直接调
 * {@link ScriptEngine#execute(Script, Map)}。</p>
 *
 * <p>启用: {@code z.script.mcp-bridge.enabled=true} (默认 true)</p>
 * <p>{@code z.mcp.enabled=false} 时容器里没有 McpRegistry: 桥接退场，脚本中心自己照常启动。</p>
 *
 * <p>注册表是内存版的，所以进程重启后靠 {@link ApplicationReadyEvent} 把已上线的脚本重新挂回去 ——
 * 只等 publish 事件的话，一次重启就把这个功能清空了。</p>
 */
@Component
@ConditionalOnProperty(prefix = "z.script.mcp-bridge", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScriptMcpAdapter {

    private static final Logger log = LogManager.getLogger(ScriptMcpAdapter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 脚本没填 inputSchema 时的兜底: 协议要求 inputSchema 必须是对象且不得为 null. */
    private static final String EMPTY_OBJECT_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private ScriptEngine scriptEngine;

    @Autowired
    private ObjectProvider<McpRegistry> mcpRegistryProvider;

    @EventListener
    public void onPublish(ScriptPublishEvent event) {
        McpRegistry registry = mcpRegistryProvider.getIfAvailable();
        if (registry == null) {
            log.info("ScriptMcpAdapter: 容器里没有 McpRegistry (z.mcp.enabled=false?)，脚本 {} 不桥接",
                    event.getScriptCode());
            return;
        }
        Script script = scriptService.getByScriptCode(event.getScriptCode());
        if (script != null) {
            register(registry, script);
        }
    }

    @EventListener
    public void onUnpublish(ScriptUnpublishEvent event) {
        McpRegistry registry = mcpRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        // publish/create 都按 script_{code} 写 mcpToolName，取消发布时那一格已被清空，只能按同一规则算
        registry.unregister(toolNameOf(event.getScriptCode()));
        log.info("ScriptMcpAdapter: 已摘除脚本 {} 的 MCP 工具", event.getScriptCode());
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        McpRegistry registry = mcpRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        // 库件的启动期动作不该把宿主应用带倒: 脚本表还没建好时只记一条错误
        try {
            List<Script> scripts = new ArrayList<>();
            scripts.addAll(scriptService.listByExposeAs("MCP"));
            scripts.addAll(scriptService.listByExposeAs("BOTH"));
            int ok = 0;
            for (Script script : scripts) {
                if (register(registry, script)) ok++;
            }
            if (!scripts.isEmpty()) {
                log.info("ScriptMcpAdapter: 重启后挂回 {} 个 MCP 工具 (候选 {} 个脚本)", ok, scripts.size());
            }
        } catch (Exception e) {
            log.error("ScriptMcpAdapter: 启动期重挂 MCP 工具失败", e);
        }
    }

    /**
     * 把脚本挂成 MCP 工具。
     *
     * <p>z-mcp 的注册表拒绝静默覆盖同名工具，所以重复发布走"先摘后挂"。
     * 工具名不合法(协议要求 {@code [A-Za-z0-9_.-]} 且 ≤128)由注册表当场抛出来，
     * 这里只负责把它记成一条看得见的错误 —— 静默失败是这个功能以前坏掉的真正原因。</p>
     *
     * @return 是否挂上
     */
    private boolean register(McpRegistry registry, Script script) {
        String toolName = toolName(script);
        registry.unregister(toolName);
        try {
            final String scriptCode = script.getScriptCode();
            registry.tool(toolName)
                    .title(script.getScriptName())
                    .description(script.getDescription() == null
                            ? "Script: " + script.getScriptName() : script.getDescription())
                    .inputSchema(orFallback(script.getInputSchema()))
                    .outputSchema(script.getOutputSchema())
                    .register(arguments -> run(scriptCode, arguments));
            log.info("ScriptMcpAdapter: 脚本 {} 已暴露为 MCP 工具 {}", scriptCode, toolName);
            return true;
        } catch (RuntimeException e) {
            log.error("ScriptMcpAdapter: 脚本 " + script.getScriptCode() + " 暴露为 MCP 工具失败: "
                    + e.getMessage(), e);
            return false;
        }
    }

    /** 每次调用重新取脚本，改完源码不必重新发布。 */
    private CallToolResult run(String scriptCode, Map<String, Object> arguments) {
        Script script = scriptService.getByScriptCode(scriptCode);
        if (script == null) {
            return CallToolResult.executionError("脚本不存在: " + scriptCode);
        }
        ExecutionResult result = scriptEngine.execute(script, arguments);
        if (!result.isSuccess()) {
            return CallToolResult.executionError(String.valueOf(result.getErrorMessage()));
        }
        String text = toJson(result.getData());
        String declared = script.getOutputSchema();
        // 广告了 outputSchema 就必须交回 structuredContent，否则协议层判"输出违反自己的 schema";
        // 没广告的脚本只交文本块, 免得客户端拿到一份它没要过的结构化结果。
        return declared == null || declared.trim().isEmpty()
                ? CallToolResult.text(text) : CallToolResult.structured(result.getData(), text);
    }

    /** 工具名: publish/create 写进 mcpToolName 的就是 script_{code}，历史数据缺这一格时按同一规则补。 */
    private static String toolName(Script script) {
        String name = script.getMcpToolName();
        return name == null || name.trim().isEmpty()
                ? toolNameOf(script.getScriptCode()) : name.trim();
    }

    private static String toolNameOf(String scriptCode) {
        return "script_" + scriptCode;
    }

    private static String orFallback(String schemaJson) {
        return schemaJson == null || schemaJson.trim().isEmpty() ? EMPTY_OBJECT_SCHEMA : schemaJson;
    }

    private static String toJson(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    // ---- 内部事件类 ----

    public static class ScriptPublishEvent extends org.springframework.context.ApplicationEvent {
        private final String scriptCode;

        public ScriptPublishEvent(Object source, String scriptCode) {
            super(source);
            this.scriptCode = scriptCode;
        }

        public String getScriptCode() {
            return scriptCode;
        }
    }

    public static class ScriptUnpublishEvent extends org.springframework.context.ApplicationEvent {
        private final String scriptCode;

        public ScriptUnpublishEvent(Object source, String scriptCode) {
            super(source);
            this.scriptCode = scriptCode;
        }

        public String getScriptCode() {
            return scriptCode;
        }
    }
}
