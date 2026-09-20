package com.zifang.z.script.web.config;

import com.zifang.z.script.engine.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.zifang.z.script")
public class ZScriptWebAutoConfiguration {

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
