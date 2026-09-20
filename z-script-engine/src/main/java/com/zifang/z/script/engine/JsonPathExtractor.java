package com.zifang.z.script.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonArray;
import com.zifang.util.json.model.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 极简 JSONPath 实现 —— 满足 API_BRIDGE 的输出字段提取。
 * <p>
 * 支持的语法:
 * $.foo              顶层字段 foo
 * $.foo.bar          foo 的子字段 bar
 * $.foo[0]           foo 数组的第一个元素
 * $.foo[*]           foo 数组的展开 (返回 List)
 * $.foo[*].bar       foo 数组每个元素的 bar 字段 (返回 List)
 * $                  根对象本身
 * <p>
 * 不支持 (但可平滑扩展):
 * - 过滤器表达式 ($.foo[?(@.x==1)])
 * - 通配符 * 单独使用
 * <p>
 * 这是一个有意为之的最小子集，覆盖 90% 实际场景，避免引入 jayway/JsonPath 之类的额外依赖。
 */
public final class JsonPathExtractor {

    private static final Pattern ARRAY_INDEX = Pattern.compile("\\[(\\d+|\\*)\\]");

    private JsonPathExtractor() {
    }

    public static Object extract(Object root, String jsonPath) {
        if (root == null || jsonPath == null || jsonPath.isEmpty()) {
            return null;
        }
        // 归一化: 缺 $ 前缀则补上
        String path = jsonPath.trim();
        if (!path.startsWith("$")) {
            path = "$" + path;
        }
        return walk(root, path);
    }

    private static Object walk(Object current, String remaining) {
        if (current == null) {
            return null;
        }

        // 剩余只剩 $，返回根
        if ("$".equals(remaining) || "$.".equals(remaining)) {
            return current;
        }


        // 解析下一个段
        // 先看是否是 $[index]
        if (remaining.startsWith("$[")) {
            int end = remaining.indexOf(']');
            if (end < 0) {
                return null;
            }

            String inside = remaining.substring(2, end);
            String rest = remaining.substring(end + 1);
            // 处理数组
            if (current instanceof JsonArray) {
                JsonArray arr = (JsonArray) current;
                if ("*".equals(inside)) {
                    // 展开: rest 接在每个元素后面
                    if (rest.isEmpty() || ".".equals(rest)) {
                        return arr;
                    }
                    List<Object> out = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject itemObj = arr.getJsonObject(i);
                        Object item = itemObj != null ? walk(itemObj, rest.startsWith(".") ? "$" + rest : rest) : null;
                        if (item != null) {
                            out.add(item);
                        }

                    }
                    return out;
                }
                try {
                    int idx = Integer.parseInt(inside);
                    if (idx < 0 || idx >= arr.size()) {
                        return null;
                    }
                    JsonObject nextObj = arr.getJsonObject(idx);
                    if (nextObj != null) {
                        if (rest.isEmpty()) {
                            return nextObj;
                        }
                        return walk(nextObj, rest.startsWith(".") ? "$" + rest : "$" + rest);
                    }
                    if (rest.isEmpty()) {
                        return null;
                    }
                    return null;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        // 解析 $.field 或 $.field.field2
        if (remaining.startsWith("$.")) {
            String after = remaining.substring(2);
            // 取第一个 . 或 [ 之前
            int nextDot = after.indexOf('.');
            int nextBracket = after.indexOf('[');
            int splitAt = -1;
            if (nextDot == -1) {
                splitAt = nextBracket;
            } else if (nextBracket == -1) splitAt = nextDot;
            else splitAt = Math.min(nextDot, nextBracket);

            String field;
            String rest;
            if (splitAt == -1) {
                field = after;
                rest = "";
            } else {
                field = after.substring(0, splitAt);
                rest = after.substring(splitAt);
            }
            if (field.isEmpty()) {
                // 路径以 . 开头，跳过并继续解析
                return walk(current, "$" + rest);
            }

            // 拿字段
            Object next = null;
            if (current instanceof JsonObject) {
                JsonObject jo = (JsonObject) current;
                next = jo.getJsonObject(field);
                if (next == null) {
                    next = jo.getJsonArray(field);
                }

                if (next == null) {
                    for (Map.Entry<String, Object> entry : jo.getAllKeyValue()) {
                        if (entry.getKey().equals(field)) {
                            next = entry.getValue();
                            break;
                        }
                    }
                }
            } else if (current instanceof Map) {
                next = ((java.util.Map<?, ?>) current).get(field);
            } else if (current instanceof JsonArray) {
                // 对数组做字段展开: 收集所有元素的 field 值
                JsonArray arr = (JsonArray) current;
                List<Object> out = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject itemObj = arr.getJsonObject(i);
                    if (itemObj != null) {
                        Object v = walk(itemObj, "$." + field + rest);
                        if (v != null) {
                            out.add(v);
                        }

                    }
                }
                return out.isEmpty() ? null : out;
            }
            if (rest.isEmpty()) {
                return next;
            }
            if (next == null) {
                return null;
            }

            return walk(next, "$" + rest);
        }
        return null;
    }

    /**
     * 一次性将 body 字符串解析成对象后再提取，便于 API 调用层包装。
     */
    public static Object extractFromJson(String body, String jsonPath) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            Object root = JsonUtil.parseObject(body);
            return extract(root, jsonPath);
        } catch (Exception e) {
            return null;
        }
    }
}
