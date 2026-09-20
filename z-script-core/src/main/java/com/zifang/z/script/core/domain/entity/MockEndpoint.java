package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Mock 端点配置实体 (FEATURE051)
 * <p>
 * 对应表: z_mock_endpoint
 * 用途: 定义 Mock 端点的匹配规则和响应模板
 *
 * @author zifang
 * @since 1.0.0
 */
@TableName("z_mock_endpoint")
public class MockEndpoint {

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
     * Mock 端点名称
     */
    private String mockName;
    /**
     * 环境编码
     */
    private String envCode;
    /**
     * 项目编码
     */
    private String projectCode;
    /**
     * 模块编码
     */
    private String moduleCode;
    /**
     * 请求路径
     */
    private String path;
    /**
     * HTTP 方法 (GET/POST/PUT/DELETE 等)
     */
    private String method;

    // Request matching
    /**
     * URL 匹配正则模式
     */
    private String matchUrlPattern;
    /**
     * 请求头匹配规则 (JSON)
     */
    private String matchHeaders;
    /**
     * 查询参数匹配规则 (JSON)
     */
    private String matchQuery;
    /**
     * 请求体匹配规则 (JSON)
     */
    private String matchBody;
    /**
     * 请求体匹配类型 (JSON/TEXT/RAW)
     */
    private String matchBodyType;

    // Response
    /**
     * 响应模板 (支持变量替换)
     */
    private String responseTemplate;
    /**
     * 响应头配置 (JSON)
     */
    private String responseHeaders;
    /**
     * 响应延迟毫秒数
     */
    private Integer delayMs;
    /**
     * HTTP 状态码
     */
    private Integer statusCode;

    // Priority & Scenario
    /**
     * 优先级 (数值越大优先级越高)
     */
    private Integer priority;
    /**
     * 关联场景编码
     */
    private String scenarioCode;
    /**
     * 进入此端点所需的前置状态
     */
    private String requiredState;
    /**
     * 匹配成功后转换到的新状态
     */
    private String newState;

    // Fault injection
    /**
     * 故障注入类型 (RANDOM/CONSTANT/PERCENTAGE)
     */
    private String faultType;
    /**
     * 故障注入配置 (JSON)
     */
    private String faultConfig;

    // Proxy mode
    /**
     * 代理转发地址 (如非空则代理到该地址)
     */
    private String proxyTo;

    // Statistics
    /**
     * 命中次数
     */
    private Long hitCount;
    /**
     * 验证次数
     */
    private Long verifyCount;
    /**
     * 最后命中时间
     */
    private Date lastHitTime;

    // Tags
    /**
     * 标签 (JSON 数组)
     */
    private String tags;
    /**
     * 描述信息
     */
    private String description;

    // Links
    /**
     * 关联脚本编码
     */
    private String scriptCode;
    /**
     * 来源录制编码 (如果是从录制生成的)
     */
    private String sourceRecording;

    // Status
    /**
     * 状态 (1=启用 0=禁用)
     */
    private Integer status;
    /**
     * 租户编码
     */
    private String tenantCode;
    /**
     * 创建人 ID
     */
    private String creatorId;

    @TableLogic
    /**
     * 逻辑删除标识
     */
    private Integer deleted;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

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

    public String getMockName() {
        return mockName;
    }

    public void setMockName(String v) {
        this.mockName = v;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String v) {
        this.envCode = v;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String v) {
        this.projectCode = v;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String v) {
        this.moduleCode = v;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String v) {
        this.path = v;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String v) {
        this.method = v;
    }

    public String getMatchUrlPattern() {
        return matchUrlPattern;
    }

    public void setMatchUrlPattern(String v) {
        this.matchUrlPattern = v;
    }

    public String getMatchHeaders() {
        return matchHeaders;
    }

    public void setMatchHeaders(String v) {
        this.matchHeaders = v;
    }

    public String getMatchQuery() {
        return matchQuery;
    }

    public void setMatchQuery(String v) {
        this.matchQuery = v;
    }

    public String getMatchBody() {
        return matchBody;
    }

    public void setMatchBody(String v) {
        this.matchBody = v;
    }

    public String getMatchBodyType() {
        return matchBodyType;
    }

    public void setMatchBodyType(String v) {
        this.matchBodyType = v;
    }

    public String getResponseTemplate() {
        return responseTemplate;
    }

    public void setResponseTemplate(String v) {
        this.responseTemplate = v;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String v) {
        this.responseHeaders = v;
    }

    public Integer getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(Integer v) {
        this.delayMs = v;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer v) {
        this.statusCode = v;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer v) {
        this.priority = v;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String v) {
        this.scenarioCode = v;
    }

    public String getRequiredState() {
        return requiredState;
    }

    public void setRequiredState(String v) {
        this.requiredState = v;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String v) {
        this.newState = v;
    }

    public String getFaultType() {
        return faultType;
    }

    public void setFaultType(String v) {
        this.faultType = v;
    }

    public String getFaultConfig() {
        return faultConfig;
    }

    public void setFaultConfig(String v) {
        this.faultConfig = v;
    }

    public String getProxyTo() {
        return proxyTo;
    }

    public void setProxyTo(String v) {
        this.proxyTo = v;
    }

    public Long getHitCount() {
        return hitCount;
    }

    public void setHitCount(Long v) {
        this.hitCount = v;
    }

    public Long getVerifyCount() {
        return verifyCount;
    }

    public void setVerifyCount(Long v) {
        this.verifyCount = v;
    }

    public Date getLastHitTime() {
        return lastHitTime;
    }

    public void setLastHitTime(Date v) {
        this.lastHitTime = v;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String v) {
        this.tags = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public String getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(String v) {
        this.scriptCode = v;
    }

    public String getSourceRecording() {
        return sourceRecording;
    }

    public void setSourceRecording(String v) {
        this.sourceRecording = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        this.status = v;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String v) {
        this.tenantCode = v;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String v) {
        this.creatorId = v;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer v) {
        this.deleted = v;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date v) {
        this.createTime = v;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date v) {
        this.updateTime = v;
    }
}
