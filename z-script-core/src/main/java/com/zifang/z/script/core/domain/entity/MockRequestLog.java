package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * Mock 请求日志实体 (FEATURE051)
 * <p>
 * 对应表: z_mock_request_log
 * 用途: 记录每一次 Mock 请求的详细信息用于审计和调试
 *
 * @author zifang
 * @since 1.0.0
 */
@BizModel(name = "Mock 请求日志实体", module = "z-script", description = "Mock 请求日志实体 (FEATURE051) 对应表: z_mock_request_log 用途: 记录每一次 Mock 请求的详细信息用于审计和调试")
@TableName("z_mock_request_log")
public class MockRequestLog {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * Mock 端点编码
     */
    @BizField(name = "Mock 端点编码", type = BizField.FieldType.STRING)
    private String mockCode;
    /**
     * 环境编码
     */
    @BizField(name = "环境编码", type = BizField.FieldType.STRING)
    private String envCode;
    /**
     * 请求路径
     */
    @BizField(name = "请求路径", type = BizField.FieldType.STRING)
    private String requestPath;
    /**
     * HTTP 请求方法
     */
    @BizField(name = "HTTP 请求方法", type = BizField.FieldType.STRING)
    private String requestMethod;
    /**
     * 请求头 (JSON)
     */
    @BizField(name = "请求头 (JSON)", type = BizField.FieldType.STRING)
    private String requestHeaders;
    /**
     * 请求体 (JSON)
     */
    @BizField(name = "请求体 (JSON)", type = BizField.FieldType.STRING)
    private String requestBody;
    /**
     * HTTP 响应状态码
     */
    @BizField(name = "HTTP 响应状态码", type = BizField.FieldType.INTEGER)
    private Integer responseStatus;
    /**
     * 响应体内容
     */
    @BizField(name = "响应体内容", type = BizField.FieldType.STRING)
    private String responseBody;
    /**
     * 是否匹配到端点 (1=匹配 0=未匹配)
     */
    @BizField(name = "是否匹配到 Mock", type = BizField.FieldType.INTEGER)
    private Integer matched;
    /**
     * 回退类型 (NONE/FALLBACK/DEFAULT)
     */
    @BizField(name = "回退类型 (NONE/FALLBACK/DEFAULT)", type = BizField.FieldType.STRING)
    private String fallbackType;
    /**
     * 响应耗时 (毫秒)
     */
    @BizField(name = "响应耗时 (毫秒)", type = BizField.FieldType.INTEGER)
    private Long durationMs;
    /**
     * 客户端 IP 地址
     */
    @BizField(name = "客户端 IP 地址", type = BizField.FieldType.STRING)
    private String clientIp;
    /**
     * 用户代理字符串
     */
    @BizField(name = "用户代理字符串", type = BizField.FieldType.STRING)
    private String userAgent;
    /**
     * 场景状态 (JSON)
     */
    @BizField(name = "场景状态 (JSON)", type = BizField.FieldType.STRING)
    private String scenarioState;
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

    public String getMockCode() {
        return mockCode;
    }

    public void setMockCode(String v) {
        this.mockCode = v;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String v) {
        this.envCode = v;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String v) {
        this.requestPath = v;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String v) {
        this.requestMethod = v;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String v) {
        this.requestHeaders = v;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String v) {
        this.requestBody = v;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer v) {
        this.responseStatus = v;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String v) {
        this.responseBody = v;
    }

    public Integer getMatched() {
        return matched;
    }

    public void setMatched(Integer v) {
        this.matched = v;
    }

    public String getFallbackType() {
        return fallbackType;
    }

    public void setFallbackType(String v) {
        this.fallbackType = v;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long v) {
        this.durationMs = v;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String v) {
        this.clientIp = v;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String v) {
        this.userAgent = v;
    }

    public String getScenarioState() {
        return scenarioState;
    }

    public void setScenarioState(String v) {
        this.scenarioState = v;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date v) {
        this.createTime = v;
    }
}



