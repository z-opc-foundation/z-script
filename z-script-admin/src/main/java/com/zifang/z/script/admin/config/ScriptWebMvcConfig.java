package com.zifang.z.script.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * z-script-admin Web 配置 —— SPA history fallback。
 *
 * <p>前端是 BrowserRouter，刷新 /z-script/console 这类路径时 Spring 找不到静态资源就直接 404，
 * 浏览器拿不到 index.html 就是白屏。vite dev / vite preview 自带 fallback，把这个洞完全掩盖了，只有线上现形。
 *
 * <p>部署形态下 context-path 是 {@code /script}，本地构建时是 {@code /}。两者都成立：
 * servlet 在进资源处理器之前就把 context-path 剥掉了，所以 resourcePath 恒为 {@code assets/...}，
 * 这里不需要按环境分支。
 *
 * <p>{@code api/} 与 {@code assets/} 返回 null 是必须的：让它们照常落到 404 / 静态资源，
 * 否则 API 404 会被悄悄回成 index.html，浏览器拿 HTML 去 parse JSON，报一个跟真实原因无关的错。
 *
 * <p>放在 admin 而不是 starter：静态资源回落是应用层的事，写进 starter 会连带给所有宿主套上 index.html 回落。
 */
@Configuration
public class ScriptWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("assets/")) {
                            return null;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }
}
