package com.zifang.z.script.web.config;

import com.zifang.z.script.engine.ScriptEngine;
import com.zifang.z.script.engine.messaging.ScriptMessageSender;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把宿主的 {@link ScriptMessageSender} 装进容器里<b>每一个</b> {@link ScriptEngine}。
 * <p>
 * <b>为什么不能只在 {@link ZScriptWebAutoConfiguration#scriptEngine()} 里接</b>：
 * 一个 JVM 里可能有多处注册引擎 —— z-script-web 自己一处、z-team 的
 * {@code ScriptEngineHostConfig} 又一处 —— 而宿主把
 * {@code spring.main.allow-bean-definition-overriding} 开着
 * ⇒ 同名 bean <b>后注册的覆盖先注册的</b>
 * ⇒ "在某个创建点接线"等于"赌哪个 bean 最后生效"
 * （2026-11-16 实测：z-team 那份赢 ⇒ 脚本里 {@code msg.send(...)} 报
 * 「宿主没有提供发消息能力」，而宿主明明提供了 sender）。
 * <p>
 * 后置处理器不看"谁创建的"，只管"容器里最后活着的那个" ⇒ 谁赢都接得上。
 * <p>
 * <b>失败即抛</b>：{@link ScriptMessageSender} 是可选 SPI，宿主没提供就<b>不</b>接
 * （脚本里 {@code msg.send(...)} 会拿到一句人话而不是空指针）；但宿主提供了却装不上，
 * 那是要炸的（否则表现为"脚本偶发发不出消息"，查起来极难）。
 */
@Configuration
public class ScriptMessageSenderWiringConfig {

    @Bean
    public static BeanPostProcessor scriptMessageSenderWiring(
            ObjectProvider<ScriptMessageSender> messageSenderProvider) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof ScriptEngine) {
                    ScriptMessageSender sender = messageSenderProvider.getIfAvailable();
                    if (sender != null) {
                        ((ScriptEngine) bean).setMessageSender(sender);
                    }
                }
                return bean;
            }
        };
    }
}
