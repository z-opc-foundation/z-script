package com.zifang.z.script.engine;

import com.zifang.util.expr.el.ElEvaluator;
import com.zifang.util.expr.groovy.GroovyExecutor;
import com.zifang.util.expr.lua.LuaExecutor;
import com.zifang.util.expr.sql.SqlParser;
import com.zifang.util.expr.sql.SqlStatement;
import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.engine.sandbox.ElSandbox;
import com.zifang.z.script.engine.sandbox.GroovySandbox;
import com.zifang.z.script.engine.sandbox.SandboxExecutor;
import com.zifang.z.script.engine.sandbox.SandboxPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * z-script 脚本执行引擎
 * <p>
 * 基于 z-util expr 模块提供多语言脚本执行能力:
 * <p>
 * | DSL类型    | 执行器                         | 说明                        |
 * |-----------|-------------------------------|-----------------------------|
 * | EL        | ElEvaluator (SpEL)           | Spring表达式语言，支持属性访问、方法调用 |
 * | GROOVY    | GroovyExecutor (GroovyShell) | Groovy脚本，完整JVM脚本语言      |
 * | LUA       | LuaExecutor (Luaj)           | Lua脚本，纯Java实现             |
 * | SQL       | SqlParser                    | SQL解析，提取语句结构             |
 * | MOCK      | MockTemplateEngine           | JSON模板渲染 (@pick/@datetime等)  |
 * | API_BRIDGE| HTTP转发 (预留)                 | API桥接，将任意HTTP API包装为脚本   |
 */
public class ScriptEngine {

    private static final Logger log = LogManager.getLogger(ScriptEngine.class);

    private final ElEvaluator elEvaluator;
    private final GroovyExecutor groovyExecutor;
    private final LuaExecutor luaExecutor;
    private final SqlParser sqlParser;
    private final MockTemplateEngine mockTemplateEngine;

    // D03 (FEATURE055): 脚本沙箱。GROOVY/EL 默认走沙箱执行，可通过构造器关闭以回退到裸执行器。
    private final boolean sandboxEnabled;
    private final SandboxPolicy sandboxPolicy;
    private final GroovySandbox groovySandbox;
    private final ElSandbox elSandbox;
    private final SandboxExecutor sandboxExecutor;

    public ScriptEngine() {
        this(true, SandboxPolicy.defaultPolicy());
    }

    /**
     * @param sandboxEnabled 是否为 GROOVY/EL 启用安全沙箱 (超时/黑白名单/受限上下文)
     * @param sandboxPolicy  沙箱策略；{@code null} 时使用 {@link SandboxPolicy#defaultPolicy()}
     */
    public ScriptEngine(boolean sandboxEnabled, SandboxPolicy sandboxPolicy) {
        this.elEvaluator = new ElEvaluator();
        this.groovyExecutor = new GroovyExecutor();
        this.luaExecutor = new LuaExecutor();
        this.sqlParser = new SqlParser();
        this.mockTemplateEngine = new MockTemplateEngine();

        this.sandboxEnabled = sandboxEnabled;
        this.sandboxPolicy = sandboxPolicy != null ? sandboxPolicy : SandboxPolicy.defaultPolicy();
        this.groovySandbox = new GroovySandbox(this.sandboxPolicy);
        this.elSandbox = new ElSandbox();
        this.sandboxExecutor = new SandboxExecutor(Math.max(2, Runtime.getRuntime().availableProcessors()));
        log.info("ScriptEngine initialized with EL, Groovy, Lua, SQL, MOCK engines (sandbox={}, timeout={}ms)",
                sandboxEnabled, this.sandboxPolicy.getTimeoutMs());
    }

    /**
     * 执行脚本
     *
     * @param script 脚本实体
     * @param params 输入参数
     * @return 执行结果
     */
    public ExecutionResult execute(Script script, Map<String, Object> params) {
        long start = System.currentTimeMillis();
        String dslType = script.getDslType();

        try {
            Object result;
            switch (dslType) {
                case "EL":
                    result = executeEl(script.getSourceCode(), params);
                    break;
                case "GROOVY":
                    result = executeGroovy(script.getSourceCode(), params);
                    break;
                case "LUA":
                    result = executeLua(script.getSourceCode(), params);
                    break;
                case "SQL":
                    result = executeSql(script.getSourceCode(), params);
                    break;
                case "MOCK":
                    result = executeMock(script.getSourceCode(), params);
                    break;
                case "API_BRIDGE":
                    result = executeApiBridge(script.getSourceCode(), params);
                    break;
                default:
                    return ExecutionResult.fail("Unsupported DSL type: " + dslType,
                            System.currentTimeMillis() - start);
            }
            return ExecutionResult.ok(result, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Script execution failed: {} ({})", script.getScriptCode(), dslType, e);
            return ExecutionResult.fail(e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    /**
     * EL: Spring Expression Language (SpEL) 执行
     * <p>
     * 使用 z-util-expr-el 的 ElEvaluator
     * 支持: 算术运算, 比较, 逻辑运算, 三元表达式, 属性访问, 方法调用
     * <p>
     * 示例: #name + ' is ' + #age + ' years old'
     * 示例: #data.?[#this.status == 1]
     */
    private Object executeEl(String sourceCode, Map<String, Object> params) {
        if (sandboxEnabled) {
            return sandboxExecutor.execute(
                    () -> elSandbox.eval(sourceCode, params), sandboxPolicy.getTimeoutMs());
        }
        if (params != null && !params.isEmpty()) {
            return elEvaluator.eval(sourceCode, params);
        }
        return elEvaluator.eval(sourceCode);
    }

    /**
     * GROOVY: Groovy脚本执行
     * <p>
     * 使用 z-util-expr-groovy 的 GroovyExecutor (基于GroovyShell)
     * 支持: 完整Groovy语法, 变量绑定, 闭包, Java互操作
     * <p>
     * 示例:
     * def result = params.name + ' processed'
     * return result
     */
    private Object executeGroovy(String sourceCode, Map<String, Object> params) {
        // Wrap params into a Groovy binding
        Map<String, Object> bindings = new HashMap<>();
        if (params != null) {
            bindings.putAll(params);
        }
        bindings.put("params", params != null ? params : new HashMap<>());
        bindings.put("__json", new JsonHelper());
        if (sandboxEnabled) {
            return sandboxExecutor.execute(
                    () -> groovySandbox.eval(sourceCode, bindings), sandboxPolicy.getTimeoutMs());
        }
        return groovyExecutor.eval(sourceCode, bindings);
    }

    /**
     * LUA: Lua脚本执行
     * <p>
     * 使用 z-util-expr-lua 的 LuaExecutor (基于Luaj纯Java实现)
     * 支持: Lua语法, 函数定义/调用, 表操作, 全局变量绑定
     * <p>
     * 示例:
     * function greet(name)
     * return "Hello, " .. name
     * end
     * return greet(params_name)
     */
    private Object executeLua(String sourceCode, Map<String, Object> params) {
        // Bind params as Lua globals
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                luaExecutor.bind(entry.getKey(), entry.getValue());
            }
        }
        return luaExecutor.eval(sourceCode);
    }

    /**
     * SQL: SQL语句解析
     * <p>
     * 使用 z-util-expr-sql 的 SqlParser
     * 解析SQL语句结构，提取表名、列名、WHERE条件、JOIN等
     * 返回解析后的结构化SqlStatement (JSON序列化)
     * <p>
     * 示例:
     * SELECT id, name FROM users WHERE status = 1 ORDER BY created_at DESC
     */
    private Object executeSql(String sourceCode, Map<String, Object> params) {
        // Support named placeholder substitution
        String sql = sourceCode;
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                sql = sql.replace(":" + entry.getKey(),
                        entry.getValue() == null ? "NULL" : entry.getValue().toString());
            }
        }
        SqlStatement statement = sqlParser.parse(sql);
        // Return the parsed statement as a map for JSON serialization
        Map<String, Object> result = new HashMap<>();
        result.put("type", statement.getType() != null ? statement.getType().name() : "UNKNOWN");
        result.put("tableName", statement.getTableName());
        result.put("columns", statement.getColumns());
        result.put("rawSql", statement.getRawSql());
        result.put("orderBy", statement.getOrderBy());
        result.put("limit", statement.getLimit());
        result.put("offset", statement.getOffset());
        return result;
    }

    /**
     * MOCK: 模板渲染引擎
     * <p>
     * 使用内置的 MockTemplateEngine
     * 支持 @pick, @sequence, @datetime, @uuid, @int, @name, @email 等表达式
     */
    private Object executeMock(String sourceCode, Map<String, Object> params) throws Exception {
        return mockTemplateEngine.render(sourceCode, params);
    }

    /**
     * API_BRIDGE: HTTP API 桥接转发 (已实现)
     * <p>
     * 将已有的HTTP API包装为脚本，实现"任意接口转MCP"的能力。
     * sourceCode 是 ApiBridgeDefinition 的 JSON:
     * - request:      z-util-http HttpRequestDefinition
     * - inputParams:  用户传入的参数如何映射到 request
     * - outputMapping: JSONPath → named output 字段映射
     */
    private Object executeApiBridge(String sourceCode, Map<String, Object> params) {
        ApiBridgeDefinition def = ApiBridgeDefinition.fromJson(sourceCode);
        if (def.getRequest() == null) {
            throw new IllegalArgumentException("API_BRIDGE script missing 'request' definition");
        }
        // 1) 把用户参数合并进 request
        HttpRequestDefinition req = def.materialize(params);

        // 2) 通过 z-util-http 的 HttpExecutor 执行
        HttpExecutor executor = HttpExecutor.getDefault();
        HttpExecutionResult result = executor.execute(req);

        // 3) 把结果按 outputMapping 提取成工具输出
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("_httpStatus", result.getStatus());
        out.put("_durationMs", result.getDurationMs());
        if (!result.isSuccess()) {
            out.put("_error", result.getError());
            out.put("_errorType", result.getErrorType());
            return out;
        }
        Object body = result.getBodyObject();
        if (body == null && result.getBody() != null) {
            try {
                body = JsonUtil.parseObject(result.getBody());
            } catch (Exception ignore) {
            }
        }
        Map<String, Object> mapped = def.extractOutput(body);
        out.putAll(mapped);
        // 完整 response 始终可用
        out.put("_rawBody", result.getBody());
        return out;
    }

    /**
     * JSON helper exposed to Groovy/EL scripts for JSON manipulation
     */
    public static class JsonHelper {
        public String stringify(Object obj) {
            return JsonUtil.toJson(obj);
        }

        public Object parse(String json) {
            return JsonUtil.parseObject(json);
        }
    }
}
