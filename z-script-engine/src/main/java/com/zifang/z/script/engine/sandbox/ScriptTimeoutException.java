package com.zifang.z.script.engine.sandbox;

/**
 * 脚本执行超过沙箱墙钟时限时抛出。
 * (D03 · FEATURE055)
 */
public class ScriptTimeoutException extends RuntimeException {

    public ScriptTimeoutException(String message) {
        super(message);
    }

    public ScriptTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
