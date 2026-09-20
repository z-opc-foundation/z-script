package com.zifang.z.script.engine.sandbox;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.ThreadInterrupt;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Groovy 沙箱 (D03 · FEATURE055)。
 * <p>
 * 用行业标准的 {@link SecureASTCustomizer} 在<b>编译期</b>拦截危险语法：
 * <ul>
 *   <li>导入黑名单 + {@code indirectImportCheck} —— 阻断 {@code import java.lang.Runtime}
 *       以及 {@code java.lang.Runtime.getRuntime()} 这类全限定名直连。</li>
 *   <li>接收者黑名单 —— 阻断对 System/Runtime/Thread/Class 等类型的方法调用。</li>
 *   <li>方法名黑名单 ({@link MethodNameGuard}) —— 阻断 {@code "cmd".execute()}、
 *       {@code obj.getClass()}、{@code Class.forName(..)} 等接收者类型静态不可知的逃逸。</li>
 * </ul>
 * 同时注入 {@link ThreadInterrupt} AST 变换，使脚本中的循环/方法带上中断检查点，
 * 配合 {@link SandboxExecutor} 的超时中断可真正打断死循环。
 * <p>
 * 每次执行都用<b>全新</b>的 {@link GroovyShell} + 独立 {@link Binding}，避免跨脚本状态泄漏。
 * 违反策略时编译失败，统一转成 {@link ScriptSecurityException}。
 */
public class GroovySandbox {

    private final SandboxPolicy policy;

    public GroovySandbox(SandboxPolicy policy) {
        this.policy = policy;
    }

    private static boolean containsSecurity(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SecurityException) {
                return true;
            }
            String msg = c.getMessage();
            if (msg != null && (msg.contains("SecurityException")
                    || msg.contains("Indirect import")
                    || msg.contains("Importing")
                    || msg.contains("not allowed"))) {
                return true;
            }
            // SecureASTCustomizer 把违规当作编译错误存进 ErrorCollector,
            // 而不是挂在 cause 链上, 需单独扫描.
            if (c instanceof MultipleCompilationErrorsException
                    && collectorHasSecurity((MultipleCompilationErrorsException) c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean collectorHasSecurity(MultipleCompilationErrorsException mce) {
        if (mce.getErrorCollector() == null || mce.getErrorCollector().getErrors() == null) {
            return false;
        }
        for (Object o : mce.getErrorCollector().getErrors()) {
            Message message = (Message) o;
            if (message instanceof ExceptionMessage) {
                Throwable cause = ((ExceptionMessage) message).getCause();
                for (Throwable c = cause; c != null; c = c.getCause()) {
                    if (c instanceof SecurityException) {
                        return true;
                    }
                }
            }
            if (message instanceof SyntaxErrorMessage) {
                String em = ((SyntaxErrorMessage) message).getCause() != null
                        ? ((SyntaxErrorMessage) message).getCause().getMessage() : null;
                if (em != null && (em.contains("not allowed")
                        || em.contains("Indirect import")
                        || em.contains("Importing")
                        || em.contains("SecurityException"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage();
    }

    public Object eval(String sourceCode, Map<String, Object> bindings) {
        CompilerConfiguration config = buildConfig();
        Binding binding = new Binding();
        if (bindings != null) {
            for (Map.Entry<String, Object> e : bindings.entrySet()) {
                binding.setVariable(e.getKey(), e.getValue());
            }
        }
        GroovyShell shell = new GroovyShell(
                Thread.currentThread().getContextClassLoader(), binding, config);
        try {
            return shell.evaluate(sourceCode);
        } catch (SecurityException se) {
            throw new ScriptSecurityException("脚本被沙箱拦截: " + se.getMessage(), se);
        } catch (Throwable t) {
            // 编译期由 SecureAST/MethodNameGuard 抛出的 SecurityException 会被包进
            // MultipleCompilationErrorsException 或 GroovyBugError(属 Error), 需顺链路探测。
            if (containsSecurity(t)) {
                throw new ScriptSecurityException("脚本被沙箱拦截: " + rootMessage(t), t);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error && !(t instanceof AssertionError)) {
                throw (Error) t;
            }
            throw new RuntimeException(t.getMessage(), t);
        }
    }

    private CompilerConfiguration buildConfig() {
        CompilerConfiguration config = new CompilerConfiguration();

        SecureASTCustomizer secure = new SecureASTCustomizer();
        secure.setIndirectImportCheckEnabled(true);
        secure.setDisallowedImports(policy.getDisallowedImports());
        secure.setDisallowedStarImports(policy.getDisallowedStarImports());
        secure.setDisallowedReceivers(policy.getDisallowedReceivers());

        // 死循环可中断：给所有类/方法注入 Thread.currentThread().isInterrupted() 检查点
        ASTTransformationCustomizer threadInterrupt =
                new ASTTransformationCustomizer(ThreadInterrupt.class);

        config.addCompilationCustomizers(
                secure,
                threadInterrupt,
                new MethodNameGuard(policy.getDisallowedMethodNames()));
        return config;
    }

    /**
     * 方法名黑名单守卫：编译期遍历 AST，对命中黑名单的方法调用抛 {@link SecurityException}。
     * 补 {@link SecureASTCustomizer} 的接收者黑名单覆盖不到的场景。
     */
    static final class MethodNameGuard extends CompilationCustomizer {

        private final Set<String> blocked;

        MethodNameGuard(java.util.List<String> names) {
            super(CompilePhase.SEMANTIC_ANALYSIS);
            this.blocked = new HashSet<>(names);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            Visitor visitor = new Visitor(source, blocked);
            visitor.visitClass(classNode);
        }

        private static final class Visitor extends ClassCodeVisitorSupport {
            private final SourceUnit source;
            private final Set<String> blocked;

            Visitor(SourceUnit source, Set<String> blocked) {
                this.source = source;
                this.blocked = blocked;
            }

            @Override
            protected SourceUnit getSourceUnit() {
                return source;
            }

            @Override
            public void visitMethodCallExpression(MethodCallExpression call) {
                String name = call.getMethodAsString();
                if (name != null && blocked.contains(name)) {
                    throw new SecurityException("方法调用被禁止: " + name + "()");
                }
                super.visitMethodCallExpression(call);
            }

            @Override
            public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                String name = call.getMethod();
                if (name != null && blocked.contains(name)) {
                    throw new SecurityException("静态方法调用被禁止: " + name + "()");
                }
                super.visitStaticMethodCallExpression(call);
            }
        }
    }
}
