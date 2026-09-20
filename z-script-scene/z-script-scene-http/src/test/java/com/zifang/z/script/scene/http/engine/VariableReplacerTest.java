package com.zifang.z.script.scene.http.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * VariableReplacer 单元测试 — FEATURE052 §3.1 链路编排基础能力.
 */
class VariableReplacerTest {

    @Test
    void replaces_simple_variable() {
        Map<String, Object> v = new HashMap<>();
        v.put("TOKEN", "abc-123");
        assertEquals("Bearer abc-123", VariableReplacer.replace("Bearer ${TOKEN}", v));
    }

    @Test
    void replaces_multiple_variables_in_one_string() {
        Map<String, Object> v = new HashMap<>();
        v.put("A", "X");
        v.put("B", "Y");
        assertEquals("X-Y-X", VariableReplacer.replace("${A}-${B}-${A}", v));
    }

    @Test
    void keeps_unknown_variable_unchanged_for_observability() {
        Map<String, Object> v = new HashMap<>();
        v.put("KNOWN", "v");
        assertEquals("${UNKNOWN}-v", VariableReplacer.replace("${UNKNOWN}-${KNOWN}", v));
    }

    @Test
    void null_template_returns_null() {
        assertNull(VariableReplacer.replace(null, new HashMap<>()));
    }

    @Test
    void no_variables_returns_unchanged() {
        assertEquals("plain text", VariableReplacer.replace("plain text", new HashMap<>()));
    }

    @Test
    void deep_replace_resolves_nested_once() {
        Map<String, Object> v = new HashMap<>();
        v.put("A", "${B}");
        v.put("B", "hello");
        // depth-first: A → "hello" (因为第二轮 ${B} 在 v 里能解析)
        assertEquals("hello", VariableReplacer.replaceDeep("${A}", v));
    }

    @Test
    void empty_vars_map_keeps_placeholder() {
        assertEquals("${X}", VariableReplacer.replace("${X}", null));
        assertEquals("${X}", VariableReplacer.replace("${X}", new HashMap<>()));
    }

    @Test
    void null_value_rendered_as_null_string() {
        Map<String, Object> v = new HashMap<>();
        v.put("A", null);
        assertEquals("null", VariableReplacer.replace("${A}", v));
    }

    @Test
    void dollar_without_braces_passes_through() {
        Map<String, Object> v = new HashMap<>();
        assertEquals("price $5", VariableReplacer.replace("price $5", v));
    }

    @Test
    void unterminated_placeholder_keeps_unchanged() {
        Map<String, Object> v = new HashMap<>();
        v.put("X", "v");
        assertEquals("${X", VariableReplacer.replace("${X", v));
    }
}
