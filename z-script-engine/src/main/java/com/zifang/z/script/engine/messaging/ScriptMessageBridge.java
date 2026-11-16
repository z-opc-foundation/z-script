package com.zifang.z.script.engine.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 绑给脚本的 {@code msg} 对象：脚本能看见的方法只有发消息。
 * <p>
 * 刻意不把 {@link ScriptMessageSender} 原对象直接塞进 binding ——
 * 那样脚本能反射拿到实现类上的任何方法（反射面一下就大了）。
 * 这里只暴露两个发消息的方法，别的一律不给。
 */
public class ScriptMessageBridge {

    private final ScriptMessageSender sender;

    public ScriptMessageBridge(ScriptMessageSender sender) {
        this.sender = sender;
    }

    /**
     * 发一条消息。
     * <pre>
     * msg.send([channel: 'IN_APP', user_id: 1001, subject: '标题', content: '正文'])
     * </pre>
     */
    public Object send(Map<String, Object> payload) {
        if (sender == null || !sender.isAvailable()) {
            String reason = sender == null ? "宿主没有提供发消息能力" : sender.unavailableReason();
            throw new IllegalStateException("当前不能发消息：" + reason);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        if (payload != null) {
            body.putAll(payload);
        }
        return sender.send(body);
    }

    /**
     * 发一条消息的便捷写法（站内信）。
     * <pre>
     * msg.send('IN_APP', 1001L, '正文')
     * </pre>
     */
    public Object send(String channel, Object userId, String content) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("channel", channel);
        if (userId != null) {
            body.put("user_id", userId);
        }
        body.put("content", content);
        return send(body);
    }

    /**
     * 能力是否就绪。
     */
    public boolean isAvailable() {
        return sender != null && sender.isAvailable();
    }
}
