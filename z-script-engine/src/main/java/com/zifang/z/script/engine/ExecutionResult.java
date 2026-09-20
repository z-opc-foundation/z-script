package com.zifang.z.script.engine;

/**
 * 脚本执行结果
 */
public class ExecutionResult {

    private boolean success;
    private Object data;
    private String errorMessage;
    private long durationMs;

    public static ExecutionResult ok(Object data, long durationMs) {
        ExecutionResult r = new ExecutionResult();
        r.success = true;
        r.data = data;
        r.durationMs = durationMs;
        return r;
    }

    public static ExecutionResult fail(String errorMessage, long durationMs) {
        ExecutionResult r = new ExecutionResult();
        r.success = false;
        r.errorMessage = errorMessage;
        r.durationMs = durationMs;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
