package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

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
@BizModel(name = "z-script 调用日志实体", module = "z-script", description = "z-script 调用日志实体 (FEATURE051) 对应表: z_script_invoke_log 字段映射 (生产 DB 已存在的字段, 不得随意改动): id, api_key_id, app_name, script_id, script_code, invoke_ip, invoke_method, invoke_path, invoke_params, invoke_status, http_status, duration_ms, error_message, invoked_at")
@TableName("z_script_invoke_log")
public class InvokeLogDO {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("api_key_id")
    @BizField(name = "调用方 (NULL=未鉴权/匿名)", type = BizField.FieldType.INTEGER)
    private Long apiKeyId;

    @TableField("app_name")
    @BizField(name = "调用方应用名 (冗余)", type = BizField.FieldType.STRING)
    private String appName;

    @TableField("script_id")
    @BizField(name = "脚本 ID", type = BizField.FieldType.INTEGER)
    private Long scriptId;

    @TableField("script_code")
    @BizField(name = "脚本编码 (冗余方便查询)", type = BizField.FieldType.STRING)
    private String scriptCode;

    @TableField("invoke_ip")
    @BizField(name = "调用方 IP", type = BizField.FieldType.STRING)
    private String invokeIp;

    @TableField("invoke_method")
    @BizField(name = "HTTP 方法", type = BizField.FieldType.STRING)
    private String invokeMethod;

    @TableField("invoke_path")
    @BizField(name = "调用路径", type = BizField.FieldType.STRING)
    private String invokePath;

    @TableField("invoke_params")
    @BizField(name = "入参 (脱敏后)", type = BizField.FieldType.STRING)
    private String invokeParams;

    @TableField("invoke_status")
    @BizField(name = "1=成功 0=失败", type = BizField.FieldType.INTEGER)
    private Integer invokeStatus;

    @TableField("http_status")
    @BizField(name = "HTTP 状态码", type = BizField.FieldType.INTEGER)
    private Integer httpStatus;

    @TableField("duration_ms")
    @BizField(name = "耗时 (毫秒)", type = BizField.FieldType.INTEGER)
    private Long durationMs;

    @TableField("error_message")
    @BizField(name = "错误信息", type = BizField.FieldType.STRING)
    private String errorMessage;

    @TableField("invoked_at")
    @BizField(name = "调用时间", type = BizField.FieldType.DATETIME)
    private Date invokedAt;

    // 业务侧临时字段 - 不持久化 (兼容 Interceptor 旧调用, 后续可扩展到独立列)
    @TableField(exist = false)
    @BizField(name = "dslType", type = BizField.FieldType.STRING)
    private String dslType;
    @TableField(exist = false)
    @BizField(name = "requestSize", type = BizField.FieldType.INTEGER)
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

