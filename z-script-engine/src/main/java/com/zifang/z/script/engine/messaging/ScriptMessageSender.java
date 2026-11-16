package com.zifang.z.script.engine.messaging;

import java.util.Map;

/**
 * 脚本发消息的能力接缝（宿主实现）。
 * <p>
 * <b>为什么不直接让脚本打 HTTP</b>：脚本里的 HTTP 调用要自带凭据，而凭据一旦写进脚本正文
 * 就会跟着脚本进库、进版本、进日志 —— 明文密钥的禁区。做成 SPI 之后：
 * 宿主（z-opc）用 {@code z-msg-client} 实现这个接口，AK 配在宿主的环境变量里，
 * 脚本只写「发什么、发给谁」，看不到任何凭据。
 * <p>
 * 引擎侧绑定名是 {@code msg}（Groovy）：{@code msg.send([channel: 'IN_APP', user_id: 1, content: '…'])}。
 */
public interface ScriptMessageSender {

    /**
     * 发一条消息。
     *
     * @param payload 至少要有 {@code channel}、{@code user_id} 或 {@code receiver}、{@code content}
     *                （键名走平台口径 snake_case；camelCase 也被服务端接受）
     * @return 投递结果（至少含 msgId）
     */
    Map<String, Object> send(Map<String, Object> payload);

    /**
     * 能力是否就绪（宿主配没配 AK、能不能连上）。脚本里可以先问一句再发，
     * 免得定时任务在宿主没配好时刷一串失败日志。
     */
    boolean isAvailable();

    /**
     * 不可用时的原因（人话），{@link #isAvailable()} 为 true 时返回 null。
     */
    default String unavailableReason() {
        return null;
    }
}
