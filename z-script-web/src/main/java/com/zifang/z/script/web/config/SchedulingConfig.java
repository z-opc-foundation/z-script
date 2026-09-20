package com.zifang.z.script.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FEATURE051 - t18 启用 Spring 定时任务
 * <p>
 * 启用后 InvokeLogCleanupJob 的 @Scheduled 注解才会生效
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
