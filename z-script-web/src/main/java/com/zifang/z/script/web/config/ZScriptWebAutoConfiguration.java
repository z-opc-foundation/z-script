package com.zifang.z.script.web.config;

import com.zifang.z.script.engine.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.zifang.z.script")
public class ZScriptWebAutoConfiguration {

    /**
     * 脚本引擎。
     * <p>
     * 发消息能力（{@code msg.send(...)}）由 {@link ScriptMessageSenderWiringConfig} 的后置处理器装进来，
     * 不在这里接 —— 一个 JVM 里可能有多处注册引擎（z-team 也注册同名 bean），
     * 在某个创建点接线等于赌"哪个 bean 最后生效"。
     */
    @Bean
    public ScriptEngine scriptEngine() {
        return new ScriptEngine();
    }

    @Bean
    public RequestMatcherEngine requestMatcherEngine() {
        return new RequestMatcherEngine();
    }

    @Bean
    public FaultInjectionEngine faultInjectionEngine() {
        return new FaultInjectionEngine();
    }

    @Bean
    public AssertionEngine assertionEngine() {
        return new AssertionEngine();
    }

    @Bean
    public MockTemplateEngine mockTemplateEngine() {
        return new MockTemplateEngine();
    }

    @Bean
    public MockProxyClient mockProxyClient() {
        return new MockProxyClient();
    }

}
