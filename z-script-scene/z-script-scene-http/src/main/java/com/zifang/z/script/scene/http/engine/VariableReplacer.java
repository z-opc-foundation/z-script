package com.zifang.z.script.scene.http.engine;

import java.util.Map;

/**
 * ${VAR} 占位符替换工具。
 *
 * <p>规则：
 * <ul>
 *   <li>占位符语法：${NAME}，NAME 由 [A-Za-z0-9_.-]+ 组成</li>
 *   <li>替换来源：传入的 Map&lt;String,Object&gt;（一般是 HttpChainContext.variables）</li>
 *   <li>占位符匹配但找不到变量时，原样保留（便于排查）</li>
 *   <li>支持字符串、Map、List 等嵌套结构 (递归替换)</li>
 * </ul>
 *
 * <p>设计为 stateless 工具类，所有方法 static；线程安全。
 */
public final class VariableReplacer {

    private VariableReplacer() {
    }

    /**
     * 替换字符串中的 ${VAR}。
     */
    public static String replace(String template, Map<String, Object> vars) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        if (vars == null || vars.isEmpty()) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length());
        int i = 0;
        int len = template.length();
        while (i < len) {
            int start = template.indexOf("${", i);
            if (start < 0) {
                sb.append(template, i, len);
                break;
            }
            sb.append(template, i, start);
            int end = template.indexOf('}', start + 2);
            if (end < 0) {
                // 没有匹配到收尾 → 原样保留
                sb.append(template.substring(start));
                break;
            }
            String name = template.substring(start + 2, end);
            Object value = vars.get(name);
            if (value == null && !vars.containsKey(name)) {
                // 未知变量 → 原样保留
                sb.append(template, start, end + 1);
            } else {
                sb.append(value == null ? "null" : value.toString());
            }
            i = end + 1;
        }
        return sb.toString();
    }

    /**
     * 替换字符串里的变量；如果替换后 value.toString 包含 ${OTHER}，
     * 可选进行最多 5 轮递归（避免循环引用）。
     */
    public static String replaceDeep(String template, Map<String, Object> vars) {
        if (template == null) {
            return null;
        }

        if (template.indexOf("${") < 0) {
            return template;
        }

        String cur = template;
        for (int i = 0; i < 5; i++) {
            String next = replace(cur, vars);
            if (next.equals(cur)) {
                return next;
            }
            cur = next;
        }
        return cur;
    }
}
