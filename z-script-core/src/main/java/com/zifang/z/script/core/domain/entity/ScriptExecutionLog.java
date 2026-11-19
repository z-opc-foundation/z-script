package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * Script 执行日志实体 (FEATURE051)
 * <p>
 * 对应表: z_script_execution_log
 * 用途: 记录每次脚本执行的输入参数、输出结果和执行状态
 *
 * @author zifang
 * @since 1.0.0
 */
@BizModel(name = "Script 执行日志实体", module = "z-script", description = "Script 执行日志实体 (FEATURE051) 对应表: z_script_execution_log 用途: 记录每次脚本执行的输入参数、输出结果和执行状态")
@TableName("z_script_execution_log")
public class ScriptExecutionLog {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 脚本编码
     */
    @BizField(name = "脚本编码", type = BizField.FieldType.STRING)
    private String scriptCode;
    /**
     * 执行类型 (API/MCP/TEST)
     */
    @BizField(name = "执行类型 (API/MCP/TEST)", type = BizField.FieldType.STRING)
    private String executeType;
    /**
     * 输入参数 (JSON)
     */
    @BizField(name = "输入参数 (JSON)", type = BizField.FieldType.STRING)
    private String inputParams;
    /**
     * 输出结果 (JSON)
     */
    @BizField(name = "输出结果 (JSON)", type = BizField.FieldType.STRING)
    private String outputResult;
    /**
     * 执行成功标志 (1=成功 0=失败)
     */
    @BizField(name = "1=成功 0=失败", type = BizField.FieldType.INTEGER)
    private Integer success;
    /**
     * 错误信息
     */
    @BizField(name = "错误信息", type = BizField.FieldType.STRING)
    private String errorMessage;
    /**
     * 执行耗时 (毫秒)
     */
    @BizField(name = "执行耗时 (毫秒)", type = BizField.FieldType.INTEGER)
    private Long durationMs;
    /**
     * 执行人 ID
     */
    @BizField(name = "执行人 ID", type = BizField.FieldType.STRING)
    private String executorId;
    /**
     * 创建时间
     */
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
    }

    public String getExecuteType() {
        return executeType;
    }

    public void setExecuteType(String executeType) {
        this.executeType = executeType;
    }

    public String getInputParams() {
        return inputParams;
    }

    public void setInputParams(String inputParams) {
        this.inputParams = inputParams;
    }

    public String getOutputResult() {
        return outputResult;
    }

    public void setOutputResult(String outputResult) {
        this.outputResult = outputResult;
    }

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}



