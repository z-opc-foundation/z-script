package com.zifang.z.script.web.config;

import com.zifang.z.script.engine.*;
import com.zifang.z.script.engine.messaging.ScriptMessageSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.zifang.z.script")
public class ZScriptWebAutoConfiguration {

    /**
     * 脚本引擎。宿主若提供 {@link ScriptMessageSender}（例如 z-opc 用 z-msg-client 实现），
     * 就装进引擎 —— 脚本里于是有 {@code msg.send(...)}，而 AK 留在宿主侧。
     */
    @Bean
    public ScriptEngine scriptEngine(ObjectProvider<ScriptMessageSender> messageSenderProvider) {
        ScriptEngine engine = new ScriptEngine();
        messageSenderProvider.ifAvailable(engine::setMessageSender);
        return engine;
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
