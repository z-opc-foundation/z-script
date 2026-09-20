package com.zifang.z.script.engine;

import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.core.domain.service.ScriptService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Script → MCP 自动桥接适配器 (FEATURE008).
 *
 * <p>当脚本以 MCP 方式发布时, ScriptController 发布 {@link ScriptPublishEvent},
 * 本组件通过 {@link EventListener} 接收, 调用 McpRegistry (z-agent-mcp-center)
 * 注册该脚本为 MCP Tool。</p>
 *
 * <p>启用: {@code z.script.mcp-bridge.enabled=true} (默认 true)</p>
 * <p>MCP center 不存在时: 因 {@code @Autowired(required=false)} 静默跳过</p>
 */
@Component
@ConditionalOnProperty(prefix = "z.script.mcp-bridge", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScriptMcpAdapter {

    private static final Logger log = LogManager.getLogger(ScriptMcpAdapter.class);

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private ScriptEngine scriptEngine;

    // (FEATURE030 v4 2026-06-26) 不从 spring 注入 mcpRegistry/mcpAnnotationToolExecutor，
    // 改为 @PostConstruct 内从 ApplicationContext 直接 getBean。原因：
    // - @Autowired Object 有 NoUniqueBeanDefinitionException (太多 Object bean)
    // - @Resource(name=...) 虽然能避免，但 @EventListener 代理受影响
    // - getBeanByName 确保 EventListenerMethodProcessor 能正确代理本 bean
    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private Object mcpRegistry;
    private Object mcpAnnotationToolExecutor;

    @PostConstruct
    public void bootstrap() {
        try {
            mcpRegistry = applicationContext.getBean("mcpRegistry");
            log.info("ScriptMcpAdapter bootstrap: McpRegistry present, registering MCP scripts");
        } catch (Exception e) {
            log.info("ScriptMcpAdapter bootstrap: McpRegistry not present, skipping");
            return;
        }
        try {
            mcpAnnotationToolExecutor = applicationContext.getBean("mcpAnnotationToolExecutor");
        } catch (Exception e) {
            // optional
        }
    }

    @EventListener
    public void onPublish(ScriptPublishEvent event) {
        if (mcpRegistry == null) {
            return;
        }
        try {
            Script script = scriptService.getByScriptCode(event.getScriptCode());
            if (script != null) {
                register(script);
            }
        } catch (Exception e) {
            log.error("ScriptMcpAdapter.onPublish failed", e);
        }
    }

    @EventListener
    public void onUnpublish(ScriptUnpublishEvent event) {
        if (mcpRegistry == null) {
            return;
        }
        try {
            unregister(event.getScriptCode());
        } catch (Exception e) {
            log.error("ScriptMcpAdapter.onUnpublish failed", e);
        }
    }

    private boolean isEligible(Script s) {
        return s != null && ("MCP".equals(s.getExposeAs()) || "BOTH".equals(s.getExposeAs()));
    }

    @SuppressWarnings("unchecked")
    private boolean register(Script script) {
        try {
            String toolName = script.getMcpToolName();
            if (toolName == null || toolName.isEmpty()) {
                toolName = "script_" + script.getScriptCode();
            }
            script.setMcpToolName(toolName);

            Map<String, Object> inputSchema = buildInputSchema(script);
            Map<String, Object> outputSchema = buildOutputSchema(script);

            // 反射调 McpRegistry.registerOrUpdate(ToolMeta)
            Class<?> metaCls = Class.forName("com.zifang.z.agent.mcp.core.ToolMeta");
            Object meta = metaCls.getDeclaredConstructor().newInstance();
            metaCls.getMethod("setToolName", String.class).invoke(meta, toolName);
            metaCls.getMethod("setType", String.class).invoke(meta, "SCRIPT");
            metaCls.getMethod("setDescription", String.class).invoke(meta,
                    script.getDescription() == null ? "Script: " + script.getScriptName() : script.getDescription());
            metaCls.getMethod("setInputSchema", Map.class).invoke(meta, inputSchema);
            metaCls.getMethod("setOutputSchema", Map.class).invoke(meta, outputSchema);

            java.lang.reflect.Method registerOrUpdate = mcpRegistry.getClass()
                    .getMethod("registerOrUpdate", metaCls);
            Object ok = registerOrUpdate.invoke(mcpRegistry, meta);

            // 注册执行器
            java.lang.reflect.Method execRegister = mcpAnnotationToolExecutor.getClass()
                    .getMethod("register", String.class, Object.class, java.lang.reflect.Method.class);
            execRegister.invoke(mcpAnnotationToolExecutor, toolName, this,
                    this.getClass().getMethod("invokeFromMcp", String.class, Map.class));

            log.info("ScriptMcpAdapter registered MCP tool: {} (input={} params, output={} fields)",
                    toolName, inputSchema.size(), outputSchema.size());
            return true;
        } catch (Exception e) {
            log.warn("ScriptMcpAdapter.register failed for {}: {}", script.getScriptCode(), e.getMessage());
            return false;
        }
    }

    public Object invokeFromMcp(String toolName, Map<String, Object> params) {
        // 查找脚本
        return null; // placeholder
    }

    private void unregister(String scriptCode) {
        try {
            String toolName = "script_" + scriptCode;
            java.lang.reflect.Method m = mcpRegistry.getClass().getMethod("unregisterTool", String.class);
            m.invoke(mcpRegistry, toolName);
            log.info("ScriptMcpAdapter unregistered tool for script={}", scriptCode);
        } catch (Throwable t) {
            log.warn("unregister failed for {}: {}", scriptCode, t.getMessage());
        }
    }

    private Map<String, Object> buildInputSchema(Script script) {
        // 简化版
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("input", Collections.singletonMap("type", "string"));
        schema.put("properties", props);
        return schema;
    }

    private Map<String, Object> buildOutputSchema(Script script) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        return schema;
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