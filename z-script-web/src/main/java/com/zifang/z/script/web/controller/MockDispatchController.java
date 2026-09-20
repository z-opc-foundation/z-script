package com.zifang.z.script.web.controller;

import com.zifang.z.script.engine.MockEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一 Mock 调度控制器。
 * <p>
 * API 基础路径: /api/mock/**
 * 所属模块: z-script-web
 * 鉴权: app + AK（{@code X-Api-Key}）；/api 前缀已被 ApiKeyAuthInterceptor 默认拒绝式拦截
 * <p>
 * 主要端点:
 * <ul>
 *   <li>ALL /api/mock/{envCode}/** — 按环境编码路由 Mock 请求</li>
 *   <li>ALL /api/mock/** — 缺省环境(default)路由</li>
 * </ul>
 * 是 Mock 平台的核心入口，所有 Mock 请求都走这里，由 MockEngine 解析并匹配 MockEndpoint。
 */
@Tag(name = "Mock-请求调度")
@RestController
@RequestMapping("/api/mock")
public class MockDispatchController {

    @Autowired
    private MockEngine mockEngine;

    /**
     * Mock 请求统一入口。
     * <p>
     * 支持两种路径形态：
     * <ul>
     *   <li>{@code GET /api/mock/{envCode}/any/path} — 使用路径中的 envCode</li>
     *   <li>{@code GET /api/mock/any/path} — 通过 {@code X-Mock-Env} Header 或默认 {@code default}</li>
     * </ul>
     * 该方法会剥离前缀并将重写后的路径传给 MockEngine 进行匹配。
     *
     * @param envCode 环境编码，可选（缺省时取 Header 或 default）
     * @param request 原始 HttpServletRequest
     * @return MockEngine 产生的响应结果（kind/statusCode/body/durationMs/matched/mockCode）
     */
    @Operation(summary = "Mock请求统一入口")
    @RequestMapping(value = {"/{envCode}/**", "/**"})
    public Object dispatch(@PathVariable(required = false) String envCode,
                           HttpServletRequest request) {
        // Get full path, strip the /api/mock prefix
        String fullPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            fullPath = fullPath.substring(contextPath.length());
        }
        String mockPath = stripPrefix(fullPath, "/api/mock");

        // If first segment is an env code, strip it
        if (envCode != null && !envCode.isEmpty()) {
            mockPath = stripPrefix(mockPath, "/" + envCode);
        } else {
            // Try to find env code from header or default
            envCode = request.getHeader("X-Mock-Env");
            if (envCode == null || envCode.isEmpty()) {
                envCode = "default";
            }
        }

        // Reconstruct the request URI for the matcher
        final String finalEnv = envCode;
        final String finalPath = mockPath;
        // We need to override the request URI for matcher
        request.setAttribute("MOCK_PATH", finalPath);
        MockEngine.MockResponse response = mockEngine.handleRequest(finalEnv, new WrappedRequest(request, finalPath));
        return formatResponse(response);
    }

    /**
     * 将 MockEngine 的 MockResponse 转换为接口友好的 Map 结构。
     *
     * @param response MockEngine 返回的响应
     * @return 包含 kind、statusCode、body、durationMs、matched、mockCode 的 Map
     */
    private Object formatResponse(MockEngine.MockResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("kind", response.getKind().name());
        map.put("statusCode", response.getStatusCode());
        map.put("body", response.getBody());
        map.put("durationMs", response.getDurationMs());
        map.put("matched", response.isMatched());
        map.put("mockCode", response.getMockCode());
        return map;
    }

    /**
     * 从字符串头部移除指定前缀。
     *
     * @param s      原始字符串
     * @param prefix 待移除的前缀
     * @return 移除前缀后的字符串；若 s 或 prefix 为空或不匹配则按规则返回
     */
    private String stripPrefix(String s, String prefix) {
        if (s == null) {
            return "";
        }

        if (prefix == null || prefix.isEmpty()) {
            return s;
        }
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    /**
     * 包装 Request 以重写路径，用于 MockEngine 内部按 mock 路径进行匹配。
     */
    static class WrappedRequest extends javax.servlet.http.HttpServletRequestWrapper {
        private final String newPath;

        /**
         * 构造一个指定新路径的 Request 包装器。
         *
         * @param request 原始 HttpServletRequest
         * @param newPath 重写后的路径
         */
        WrappedRequest(HttpServletRequest request, String newPath) {
            super(request);
            this.newPath = newPath;
        }

        /**
         * @return 重写后的请求 URI（mock 路径）
         */
        @Override
        public String getRequestURI() {
            return newPath;
        }

        /**
         * @return 重写后的 Servlet 路径（mock 路径）
         */
        @Override
        public String getServletPath() {
            return newPath;
        }

        /**
         * @return 重写后的 PathInfo（mock 路径）
         */
        @Override
        public String getPathInfo() {
            return newPath;
        }
    }
}
