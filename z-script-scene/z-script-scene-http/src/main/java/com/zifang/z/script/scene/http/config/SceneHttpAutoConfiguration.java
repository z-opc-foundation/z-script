package com.zifang.z.script.scene.http.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-script-scene-http 模块自动配置入口 (FEATURE052 Phase 1).
 *
 * <p>扫描规则：
 * <ul>
 *   <li>engine 子包 — HttpChainExecutor 等引擎组件 (@Component)</li>
 *   <li>web 子包 — HttpChainController (含 @RestController)</li>
 * </ul>
 *
 * <p>调用方只要引入 {@code io.github.yuku123:z-script-scene-http} 依赖即可，
 * 本类经 {@code META-INF/spring.factories} 的 EnableAutoConfiguration 键自动装配，
 * 不需要（也不应该）让调用方的 {@code @SpringBootApplication} 去扫 z-script 的包 ——
 * 包根不同，扫描永远扫不到。
 */
@Configuration
@ComponentScan(basePackages = {
        "com.zifang.z.script.scene.http.engine",
        "com.zifang.z.script.scene.http.web"
})
public class SceneHttpAutoConfiguration {
}
