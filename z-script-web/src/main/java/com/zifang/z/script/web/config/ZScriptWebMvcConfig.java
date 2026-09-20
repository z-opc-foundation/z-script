package com.zifang.z.script.web.config;

import com.zifang.z.script.web.interceptor.ApiKeyAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * z-script Web MVC 配置 (FEATURE051 / FEATURE055 · D04 已整改)
 * <p>
 * 注册 API Key 鉴权拦截器：z-script 是自带鉴权的中间件，
 * <b>唯一的认证方式就是 app + AK</b>（{@code X-Api-Key}，可选 HMAC 签名），
 * 不挂 z-ctc 4A / SSO —— 控制台与被调用方走同一套。
 * <p>
 * <b>默认拒绝</b>：拦截 {@code /api/**}（仓内所有管理面 Controller 都在这个前缀下）与历史运行时路由
 * {@code /run/**}，只放行「创建第一把 Key」这一个引导端点（此时调用方手上还没有 Key）。
 * <p>
 * <b>D04 整改要点</b>:
 * <ul>
 *   <li>把 {@code /api/script/run} (Ad-hoc 执行) 纳入拦截, 关闭"未鉴权可执行任意脚本"旁路;</li>
 *   <li>移除"开发期"过宽的 excludePathPatterns (list / byCode / import-curl / preview-mapping), 这些端点本身应受 API Key 鉴权;</li>
 *   <li>孵化时进一步整改：原先逐条列举 addPathPatterns 会让未点名的端点（如
 *       {@code /api/mock-platform/**} 整套 Mock 管理面）静默免鉴权，改为白名单 exclude + 黑名单 {@code /api/**};</li>
 *   <li>孵化时补拦 {@code /run/**}：ScriptRunController 与 /api/script-run 提供的是同一件事的两扇门，
 *       而前者不在 /api 前缀下，早先只拦 /api 时它成了一条免鉴权的脚本执行入口。</li>
 * </ul>
 */
@Configuration
public class ZScriptWebMvcConfig implements WebMvcConfigurer {

    @Resource
    private ApiKeyAuthInterceptor apiKeyAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor)
                // /api/**：仓内所有管理面 Controller，脚本的 HTTP 暴露（/api/script-run/**）
                // 与 Mock 分发（/api/mock/**）本身也在该前缀下，无需再点名。
                // /run/**：ScriptRunController 的历史路由，不在 /api 前缀下，却是「已发布脚本」
                // 的另一扇门（publish 后 httpPath 就是 /run/{code}），不点名等于留了一条免鉴权的执行旁路。
                .addPathPatterns("/api/**", "/run/**")
                .excludePathPatterns(
                        // 引导：创建 Key 时调用方还没有 Key，这是唯一必须免鉴权的端点。
                        // 精确匹配 /api/script/api-key，不含 /page、/list-enabled、/{id} 等读端点。
                        "/api/script/api-key"
                );
    }
}
