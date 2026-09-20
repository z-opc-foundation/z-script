package com.zifang.z.script.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * z-script 独立可启动应用（脚本平台 + Mock 平台 + Web 控制台）。
 * <p>
 * 组件扫描落在 {@code com.zifang.z.script}，因此 web 层的
 * {@code ZScriptWebAutoConfiguration} 与 scene-http 层的自动配置一并生效。
 */
@SpringBootApplication(scanBasePackages = "com.zifang.z.script")
public class ZScriptAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZScriptAdminApplication.class, args);
    }
}
