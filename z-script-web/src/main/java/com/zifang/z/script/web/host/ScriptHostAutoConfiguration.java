package com.zifang.z.script.web.host;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 宿主侧胶水装配 (2026-10-03 自 z-opc main-starter 平移):
 * ScriptRunAliasController (/api/script-run/list 前端别名面).
 *
 * <p>跟随 z.script.host.enabled 开关 (默认关): 寄生 all-in-one 模式由宿主打开.
 */
@Configuration
@ConditionalOnProperty(prefix = "z.script.host", name = "enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = "com.zifang.z.script.web.host")
public class ScriptHostAutoConfiguration {
}