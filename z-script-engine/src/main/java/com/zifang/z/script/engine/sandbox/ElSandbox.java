package com.zifang.z.script.engine.sandbox;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.Map;

/**
 * SpEL 沙箱 (D03 · FEATURE055)。
 * <p>
 * 用 {@link SimpleEvaluationContext} 替换默认的 {@code StandardEvaluationContext}。
 * 这是 Spring 官方推荐的“不可信表达式”求值上下文：结构上<b>禁止</b>
 * <ul>
 *   <li>类型引用 {@code T(java.lang.Runtime)} —— 无 TypeLocator；</li>
 *   <li>构造器 {@code new java.lang.ProcessBuilder(..)}；</li>
 *   <li>Bean 引用 {@code @beanName}。</li>
 * </ul>
 * 仅保留属性读取 + 实例方法调用，满足脚本做数据变换的正常需求，同时堵住 SpEL 注入 RCE。
 */
public class ElSandbox {

    private final ExpressionParser parser = new SpelExpressionParser();

    public Object eval(String sourceCode, Map<String, Object> params) {
        SimpleEvaluationContext.Builder builder =
                SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods();
        SimpleEvaluationContext context = builder.build();
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                context.setVariable(e.getKey(), e.getValue());
            }
        }
        try {
            Expression expression = parser.parseExpression(sourceCode);
            return expression.getValue(context);
        } catch (EvaluationException ee) {
            String msg = ee.getMessage();
            // SimpleEvaluationContext 拒绝类型/构造器/Bean 引用时抛 EvaluationException
            if (msg != null && (msg.contains("type") || msg.contains("Type")
                    || msg.contains("constructor") || msg.contains("Constructor")
                    || msg.contains("bean") || msg.contains("Bean"))) {
                throw new ScriptSecurityException("SpEL 表达式被沙箱拦截: " + msg, ee);
            }
            throw new RuntimeException(msg, ee);
        }
    }
}
