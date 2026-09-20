package com.zifang.z.script.engine.sandbox;

import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.engine.ExecutionResult;
import com.zifang.z.script.engine.ScriptEngine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D03 (FEATURE055) 脚本沙箱自测。
 * <p>
 * 覆盖三类保证：
 * <ol>
 *   <li>正常脚本放行 —— 合法的数据变换脚本正确求值；</li>
 *   <li>危险操作拦截 —— RCE / 反射逃逸 / 文件与进程访问被 {@link ScriptSecurityException} 阻断；</li>
 *   <li>超时中断 —— 死循环被 {@link ScriptTimeoutException} 强制打断。</li>
 * </ol>
 */
public class ScriptSandboxTest {

    private final GroovySandbox groovy = new GroovySandbox(SandboxPolicy.defaultPolicy());
    private final ElSandbox el = new ElSandbox();

    // ---------------------------------------------------------------------
    // 1. 正常脚本放行
    // ---------------------------------------------------------------------

    @Test
    public void groovy_allows_normal_arithmetic_and_binding() {
        Map<String, Object> b = new HashMap<>();
        b.put("x", 3);
        b.put("y", 4);
        Object r = groovy.eval("x * y + 1", b);
        assertEquals(13, ((Number) r).intValue());
    }

    @Test
    public void groovy_allows_collections_and_closures() {
        Map<String, Object> b = new HashMap<>();
        b.put("nums", Arrays.asList(1, 2, 3, 4));
        Object r = groovy.eval("nums.findAll { it % 2 == 0 }.sum()", b);
        assertEquals(6, ((Number) r).intValue());
    }

    @Test
    public void el_allows_property_and_instance_method() {
        Map<String, Object> p = new HashMap<>();
        p.put("name", "world");
        Object r = el.eval("'hello ' + #name.toUpperCase()", p);
        assertEquals("hello WORLD", r);
    }

    // ---------------------------------------------------------------------
    // 2. 危险操作拦截 (Groovy)
    // ---------------------------------------------------------------------

    @Test
    public void groovy_blocks_system_exit() {
        assertThrows(ScriptSecurityException.class,
                () -> groovy.eval("System.exit(0)", new HashMap<>()));
    }

    @Test
    public void groovy_blocks_runtime_exec() {
        assertThrows(ScriptSecurityException.class,
                () -> groovy.eval("Runtime.getRuntime().exec('id')", new HashMap<>()));
    }

    @Test
    public void groovy_blocks_fully_qualified_runtime() {
        // indirectImportCheck 应捕获全限定名直连
        assertThrows(ScriptSecurityException.class,
                () -> groovy.eval("java.lang.Runtime.getRuntime().exec('id')", new HashMap<>()));
    }

    @Test
    public void groovy_blocks_string_execute_rce() {
        // Groovy 扩展方法 String.execute() 直接起进程 —— 方法名黑名单拦截
        assertThrows(ScriptSecurityException.class,
                () -> groovy.eval("'id'.execute().text", new HashMap<>()));
    }

    @Test
    public void groovy_blocks_class_forname_reflection() {
        assertThrows(ScriptSecurityException.class,
                () -> groovy.eval("Class.forName('java.lang.Runtime')", new HashMap<>()));
    }

    @Test
    public void groovy_blocks_getclass_escape() {
        assertThrows(ScriptSecurityException.class,
                () -> groovy.eval("this.getClass().getClassLoader()", new HashMap<>()));
    }

    @Test
    public void groovy_blocks_file_import() {
        assertThrows(ScriptSecurityException.class,
                () -> groovy.eval("import java.io.File; new File('/etc/passwd').text", new HashMap<>()));
    }

    // ---------------------------------------------------------------------
    // 2b. 危险操作拦截 (SpEL)
    // ---------------------------------------------------------------------

    @Test
    public void el_blocks_type_reference_rce() {
        // SimpleEvaluationContext 结构上不支持 T() 类型引用
        assertThrows(RuntimeException.class,
                () -> el.eval("T(java.lang.Runtime).getRuntime().exec('id')", new HashMap<>()));
    }

    @Test
    public void el_blocks_constructor() {
        // SimpleEvaluationContext 不支持构造器
        assertThrows(RuntimeException.class,
                () -> el.eval("new java.lang.ProcessBuilder('id').start()", new HashMap<>()));
    }

    // ---------------------------------------------------------------------
    // 3. 超时中断
    // ---------------------------------------------------------------------

    @Test
    public void sandbox_interrupts_infinite_loop() {
        SandboxPolicy shortTimeout = new SandboxPolicy(
                500L,
                SandboxPolicy.defaultPolicy().getDisallowedImports(),
                SandboxPolicy.defaultPolicy().getDisallowedStarImports(),
                SandboxPolicy.defaultPolicy().getDisallowedReceivers(),
                SandboxPolicy.defaultPolicy().getDisallowedMethodNames());
        GroovySandbox g = new GroovySandbox(shortTimeout);
        SandboxExecutor executor = new SandboxExecutor(2);
        try {
            assertThrows(ScriptTimeoutException.class,
                    () -> executor.execute(() -> g.eval("while (true) {}", new HashMap<>()),
                            shortTimeout.getTimeoutMs()));
        } finally {
            executor.shutdown();
        }
    }

    // ---------------------------------------------------------------------
    // 4. 端到端：通过 ScriptEngine 验证沙箱已默认接管 GROOVY/EL
    // ---------------------------------------------------------------------

    @Test
    public void engine_normal_groovy_ok() {
        ScriptEngine engine = new ScriptEngine();
        Script s = new Script();
        s.setScriptCode("t-ok");
        s.setDslType("GROOVY");
        s.setSourceCode("params.a + params.b");
        Map<String, Object> p = new HashMap<>();
        p.put("a", 10);
        p.put("b", 5);
        ExecutionResult r = engine.execute(s, p);
        assertTrue(r.isSuccess());
        assertEquals(15, ((Number) r.getData()).intValue());
    }

    @Test
    public void engine_blocks_malicious_groovy() {
        ScriptEngine engine = new ScriptEngine();
        Script s = new Script();
        s.setScriptCode("t-evil");
        s.setDslType("GROOVY");
        s.setSourceCode("'rm -rf /'.execute()");
        ExecutionResult r = engine.execute(s, new HashMap<>());
        assertFalse(r.isSuccess(), "恶意脚本必须执行失败");
    }
}
