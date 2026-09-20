package com.zifang.z.script.scene.http.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 整条 HTTP 链路的执行结果（汇总）。
 *
 * <p>调用方（z-qa / z-agent / 单元测试）收到此对象后：
 * <ul>
 *   <li>可遍历 {@link #steps} 查看每步的 StepExecutionResult（含完整溯源）</li>
 *   <li>读 {@link #result} 决定是否通过</li>
 *   <li>读 {@link #variablesFinal} 获取链路最后一刻的变量快照</li>
 * </ul>
 */
public class ChainExecutionResult {

    /**
     * 链路执行 ID（用于日志关联，可选；调用方填）。
     */
    private String chainId;

    /**
     * 链路整体结果：pass / fail / error / aborted。
     */
    private String result;

    /**
     * 各步骤的执行结果（按 stepNo 升序）。
     */
    private List<StepExecutionResult> steps = new ArrayList<>();

    /**
     * 通过步数 / 失败步数 / 跳过步数 / 总步数（汇总）。
     */
    private int totalSteps;
    private int passSteps;
    private int failSteps;
    private int skipSteps;

    /**
     * 链路变量快照（执行结束时的最终值，方便链路之间透传）。
     */
    private Map<String, Object> variablesFinal = new LinkedHashMap<>();

    /**
     * 总耗时（毫秒）。
     */
    private long durationMs;

    /**
     * 失败时的简短错误信息（如果有）。
     */
    private String errorMessage;

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<StepExecutionResult> getSteps() {
        return steps;
    }

    public void setSteps(List<StepExecutionResult> steps) {
        this.steps = steps;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getPassSteps() {
        return passSteps;
    }

    public void setPassSteps(int passSteps) {
        this.passSteps = passSteps;
    }

    public int getFailSteps() {
        return failSteps;
    }

    public void setFailSteps(int failSteps) {
        this.failSteps = failSteps;
    }

    public int getSkipSteps() {
        return skipSteps;
    }

    public void setSkipSteps(int skipSteps) {
        this.skipSteps = skipSteps;
    }

    public Map<String, Object> getVariablesFinal() {
        return variablesFinal;
    }

    public void setVariablesFinal(Map<String, Object> variablesFinal) {
        this.variablesFinal = variablesFinal;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
