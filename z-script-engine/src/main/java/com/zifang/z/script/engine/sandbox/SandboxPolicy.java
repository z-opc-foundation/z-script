package com.zifang.z.script.engine.sandbox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 脚本沙箱策略 (D03 · FEATURE055)。
 * <p>
 * 集中定义脚本执行的安全边界，遵循行业通行做法：
 * <ul>
 *   <li><b>超时中断</b> —— 单次执行墙钟时限，配合线程池 + Groovy {@code @ThreadInterrupt} 打断死循环。</li>
 *   <li><b>导入黑名单</b> —— 禁止 import 危险类/包 (Runtime/ProcessBuilder/反射/IO/网络...)，
 *       并开启 indirect 检查捕获全限定名直接调用 (如 {@code java.lang.Runtime.getRuntime()})。</li>
 *   <li><b>接收者黑名单</b> —— 禁止对危险类型发起方法调用。</li>
 *   <li><b>方法名黑名单</b> —— 捕获接收者类型无法静态识别的调用 (如 {@code "cmd".execute()}、
 *       {@code obj.getClass()}、{@code Class.forName(..)})。</li>
 * </ul>
 * 该策略不可变，通过 {@link #defaultPolicy()} 获取行业默认强度，或用构造器自定义。
 */
public final class SandboxPolicy {

    /**
     * 单次脚本执行的墙钟超时 (毫秒)。超时后线程被中断。
     */
    private final long timeoutMs;

    /**
     * 禁止 import 的全限定类名。
     */
    private final List<String> disallowedImports;

    /**
     * 禁止 import 的包 (star import)。
     */
    private final List<String> disallowedStarImports;

    /**
     * 禁止作为方法调用接收者的全限定类名。
     */
    private final List<String> disallowedReceivers;

    /**
     * 禁止调用的方法名 (不论接收者类型)。
     */
    private final List<String> disallowedMethodNames;

    public SandboxPolicy(long timeoutMs,
                         List<String> disallowedImports,
                         List<String> disallowedStarImports,
                         List<String> disallowedReceivers,
                         List<String> disallowedMethodNames) {
        this.timeoutMs = timeoutMs;
        this.disallowedImports = Collections.unmodifiableList(disallowedImports);
        this.disallowedStarImports = Collections.unmodifiableList(disallowedStarImports);
        this.disallowedReceivers = Collections.unmodifiableList(disallowedReceivers);
        this.disallowedMethodNames = Collections.unmodifiableList(disallowedMethodNames);
    }

    /**
     * 行业默认策略：5s 超时 + 覆盖常见 RCE / 逃逸向量的黑名单。
     */
    public static SandboxPolicy defaultPolicy() {
        List<String> imports = Arrays.asList(
                "java.lang.Runtime",
                "java.lang.ProcessBuilder",
                "java.lang.Process",
                "java.lang.Thread",
                "java.lang.ThreadGroup",
                "java.lang.Class",
                "java.lang.ClassLoader",
                "java.lang.System",
                "java.lang.Compiler",
                "java.lang.Shutdown",
                "java.io.File",
                "java.io.FileInputStream",
                "java.io.FileOutputStream",
                "java.io.FileReader",
                "java.io.FileWriter",
                "java.io.RandomAccessFile",
                "groovy.lang.GroovyShell",
                "groovy.lang.GroovyClassLoader",
                "groovy.util.Eval",
                "javax.script.ScriptEngineManager"
        );
        List<String> starImports = Arrays.asList(
                "java.lang.reflect",
                "java.nio.file",
                "java.net",
                "javax.script",
                "sun.misc",
                "jdk.internal"
        );
        List<String> receivers = Arrays.asList(
                "java.lang.System",
                "java.lang.Runtime",
                "java.lang.ProcessBuilder",
                "java.lang.Process",
                "java.lang.Thread",
                "java.lang.Class",
                "java.lang.ClassLoader",
                "java.io.File",
                "groovy.util.Eval",
                "java.lang.reflect.Method",
                "java.lang.reflect.Field",
                "java.lang.reflect.Constructor"
        );
        List<String> methods = Arrays.asList(
                "execute",       // "cmd".execute() -> Process (Groovy 扩展方法, RCE)
                "executeMethod",
                "getRuntime",
                "exec",
                "exit",
                "halt",
                "forName",       // Class.forName(..) 反射逃逸
                "newInstance",
                "getClass",      // obj.getClass().forName(..) 逃逸链起点
                "getClassLoader",
                "loadClass",
                "load",
                "loadLibrary",
                "getMethod",
                "getDeclaredMethod",
                "getMethods",
                "getDeclaredMethods",
                "invoke",
                "getField",
                "getDeclaredField",
                "setAccessible",
                "sleep",         // 阻塞占用沙箱线程
                "wait",
                "notify",
                "notifyAll"
        );
        return new SandboxPolicy(5000L, imports, starImports, receivers, methods);
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public List<String> getDisallowedImports() {
        return disallowedImports;
    }

    public List<String> getDisallowedStarImports() {
        return disallowedStarImports;
    }

    public List<String> getDisallowedReceivers() {
        return disallowedReceivers;
    }

    public List<String> getDisallowedMethodNames() {
        return disallowedMethodNames;
    }
}
