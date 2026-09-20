package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Mock 测试用例实体 (FEATURE051)
 * <p>
 * 对应表: z_mock_test_case
 * 用途: 定义 Mock 测试用例的请求和断言规则
 *
 * @author zifang
 * @since 1.0.0
 */
@TableName("z_mock_test_case")
public class MockTestCase {

    @TableId(type = IdType.AUTO)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 用例编码
     */
    private String caseCode;
    /**
     * 用例名称
     */
    private String caseName;
    /**
     * 用例分组
     */
    private String caseGroup;
    /**
     * 环境编码
     */
    private String envCode;
    /**
     * HTTP 请求方法
     */
    private String requestMethod;
    /**
     * 请求 URL
     */
    private String requestUrl;
    /**
     * 请求头 (JSON)
     */
    private String requestHeaders;
    /**
     * 查询参数 (JSON)
     */
    private String requestQuery;
    /**
     * 请求体 (JSON)
     */
    private String requestBody;
    /**
     * 期望的 HTTP 状态码
     */
    private Integer expectedStatus;
    /**
     * 期望的响应头 (JSON)
     */
    private String expectedHeaders;
    /**
     * 期望的响应体
     */
    private String expectedBody;
    /**
     * 响应体匹配类型 (EXACT/PARTIAL/REGEX)
     */
    private String expectedBodyMatchType;
    /**
     * 断言规则 (JSON)
     */
    private String assertions;
    /**
     * 提取规则 (JSON)
     */
    private String extractRules;
    /**
     * 依赖的其他用例编码 (JSON)
     */
    private String dependsOn;
    /**
     * 运行次数
     */
    private Long runCount;
    /**
     * 通过次数
     */
    private Long passCount;
    /**
     * 失败次数
     */
    private Long failCount;
    /**
     * 最后一次运行时间
     */
    private Date lastRunTime;
    /**
     * 最后一次运行结果
     */
    private String lastRunResult;
    /**
     * 优先级 (HIGH/MEDIUM/LOW)
     */
    private String priority;
    /**
     * 标签 (JSON 数组)
     */
    private String tags;
    /**
     * 状态 (1=启用 0=禁用)
     */
    private Integer status;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 所有者 ID
     */
    private String ownerId;
    /**
     * 租户编码
     */
    private String tenantCode;
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

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String v) {
        this.caseCode = v;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String v) {
        this.caseName = v;
    }

    public String getCaseGroup() {
        return caseGroup;
    }

    public void setCaseGroup(String v) {
        this.caseGroup = v;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String v) {
        this.envCode = v;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String v) {
        this.requestMethod = v;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String v) {
        this.requestUrl = v;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String v) {
        this.requestHeaders = v;
    }

    public String getRequestQuery() {
        return requestQuery;
    }

    public void setRequestQuery(String v) {
        this.requestQuery = v;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String v) {
        this.requestBody = v;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer v) {
        this.expectedStatus = v;
    }

    public String getExpectedHeaders() {
        return expectedHeaders;
    }

    public void setExpectedHeaders(String v) {
        this.expectedHeaders = v;
    }

    public String getExpectedBody() {
        return expectedBody;
    }

    public void setExpectedBody(String v) {
        this.expectedBody = v;
    }

    public String getExpectedBodyMatchType() {
        return expectedBodyMatchType;
    }

    public void setExpectedBodyMatchType(String v) {
        this.expectedBodyMatchType = v;
    }

    public String getAssertions() {
        return assertions;
    }

    public void setAssertions(String v) {
        this.assertions = v;
    }

    public String getExtractRules() {
        return extractRules;
    }

    public void setExtractRules(String v) {
        this.extractRules = v;
    }

    public String getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(String v) {
        this.dependsOn = v;
    }

    public Long getRunCount() {
        return runCount;
    }

    public void setRunCount(Long v) {
        this.runCount = v;
    }

    public Long getPassCount() {
        return passCount;
    }

    public void setPassCount(Long v) {
        this.passCount = v;
    }

    public Long getFailCount() {
        return failCount;
    }

    public void setFailCount(Long v) {
        this.failCount = v;
    }

    public Date getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(Date v) {
        this.lastRunTime = v;
    }

    public String getLastRunResult() {
        return lastRunResult;
    }

    public void setLastRunResult(String v) {
        this.lastRunResult = v;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String v) {
        this.priority = v;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String v) {
        this.tags = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        this.status = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String v) {
        this.ownerId = v;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String v) {
        this.tenantCode = v;
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
