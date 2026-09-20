package com.zifang.z.script.engine.sandbox;

/**
 * 脚本触碰沙箱安全边界时抛出 (违反导入/接收者/方法名黑名单)。
 * (D03 · FEATURE055)
 */
public class ScriptSecurityException extends RuntimeException {

    public ScriptSecurityException(String message) {
        super(message);
    }

    public ScriptSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
