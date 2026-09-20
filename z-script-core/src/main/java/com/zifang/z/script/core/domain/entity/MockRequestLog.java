package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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
@TableName("z_mock_request_log")
public class MockRequestLog {

    @TableId(type = IdType.AUTO)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * Mock 端点编码
     */
    private String mockCode;
    /**
     * 环境编码
     */
    private String envCode;
    /**
     * 请求路径
     */
    private String requestPath;
    /**
     * HTTP 请求方法
     */
    private String requestMethod;
    /**
     * 请求头 (JSON)
     */
    private String requestHeaders;
    /**
     * 请求体 (JSON)
     */
    private String requestBody;
    /**
     * HTTP 响应状态码
     */
    private Integer responseStatus;
    /**
     * 响应体内容
     */
    private String responseBody;
    /**
     * 是否匹配到端点 (1=匹配 0=未匹配)
     */
    private Integer matched;
    /**
     * 回退类型 (NONE/FALLBACK/DEFAULT)
     */
    private String fallbackType;
    /**
     * 响应耗时 (毫秒)
     */
    private Long durationMs;
    /**
     * 客户端 IP 地址
     */
    private String clientIp;
    /**
     * 用户代理字符串
     */
    private String userAgent;
    /**
     * 场景状态 (JSON)
     */
    private String scenarioState;
    /**
     * 创建时间
     */
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
