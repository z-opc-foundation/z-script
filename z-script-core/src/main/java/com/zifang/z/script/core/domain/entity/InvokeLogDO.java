package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * z-script 调用日志实体 (FEATURE051)
 * <p>
 * 对应表: z_script_invoke_log
 * 字段映射 (生产 DB 已存在的字段, 不得随意改动):
 * id, api_key_id, app_name, script_id, script_code, invoke_ip, invoke_method,
 * invoke_path, invoke_params, invoke_status, http_status, duration_ms,
 * error_message, invoked_at
 */
@TableName("z_script_invoke_log")
public class InvokeLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("api_key_id")
    private Long apiKeyId;

    @TableField("app_name")
    private String appName;

    @TableField("script_id")
    private Long scriptId;

    @TableField("script_code")
    private String scriptCode;

    @TableField("invoke_ip")
    private String invokeIp;

    @TableField("invoke_method")
    private String invokeMethod;

    @TableField("invoke_path")
    private String invokePath;

    @TableField("invoke_params")
    private String invokeParams;

    @TableField("invoke_status")
    private Integer invokeStatus;

    @TableField("http_status")
    private Integer httpStatus;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("error_message")
    private String errorMessage;

    @TableField("invoked_at")
    private Date invokedAt;

    // 业务侧临时字段 - 不持久化 (兼容 Interceptor 旧调用, 后续可扩展到独立列)
    @TableField(exist = false)
    private String dslType;
    @TableField(exist = false)
    private Integer requestSize;

    // ========== 业务侧别名 Getter/Setter (兼容 Service 调用) ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Long getScriptId() {
        return scriptId;
    }

    public void setScriptId(Long scriptId) {
        this.scriptId = scriptId;
    }

    public String getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
    }

    public String getClientIp() {
        return invokeIp;
    }

    public void setClientIp(String clientIp) {
        this.invokeIp = clientIp;
    }

    public String getHttpMethod() {
        return invokeMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.invokeMethod = httpMethod;
    }

    public String getPath() {
        return invokePath;
    }

    public void setPath(String path) {
        this.invokePath = path;
    }

    public String getInvokeParams() {
        return invokeParams;
    }

    public void setInvokeParams(String invokeParams) {
        this.invokeParams = invokeParams;
    }

    public Integer getSuccess() {
        return invokeStatus;
    }

    public void setSuccess(Integer success) {
        this.invokeStatus = success;
    }

    public Integer getInvokeStatus() {
        return invokeStatus;
    }

    public void setInvokeStatus(Integer invokeStatus) {
        this.invokeStatus = invokeStatus;
    }

    public Integer getStatusCode() {
        return httpStatus;
    }

    public void setStatusCode(Integer statusCode) {
        this.httpStatus = statusCode;
    }

    public Long getCostMs() {
        return durationMs;
    }

    public void setCostMs(Long costMs) {
        this.durationMs = costMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getCreateTime() {
        return invokedAt;
    }

    public void setCreateTime(Date createTime) {
        this.invokedAt = createTime;
    }

    public Date getInvokedAt() {
        return invokedAt;
    }

    public void setInvokedAt(Date invokedAt) {
        this.invokedAt = invokedAt;
    }

    public String getDslType() {
        return dslType;
    }

    public void setDslType(String dslType) {
        this.dslType = dslType;
    }

    public Integer getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(Integer requestSize) {
        this.requestSize = requestSize;
    }
}