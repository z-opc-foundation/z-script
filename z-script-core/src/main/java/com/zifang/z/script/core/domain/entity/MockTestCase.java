package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

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
@BizModel(name = "Mock 测试用例实体", module = "z-script", description = "Mock 测试用例实体 (FEATURE051) 对应表: z_mock_test_case 用途: 定义 Mock 测试用例的请求和断言规则")
@TableName("z_mock_test_case")
public class MockTestCase {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 用例编码
     */
    @BizField(name = "用例编码", type = BizField.FieldType.STRING)
    private String caseCode;
    /**
     * 用例名称
     */
    @BizField(name = "用例名称", type = BizField.FieldType.STRING)
    private String caseName;
    /**
     * 用例分组
     */
    @BizField(name = "用例分组", type = BizField.FieldType.STRING)
    private String caseGroup;
    /**
     * 环境编码
     */
    @BizField(name = "环境编码", type = BizField.FieldType.STRING)
    private String envCode;
    /**
     * HTTP 请求方法
     */
    @BizField(name = "HTTP 请求方法", type = BizField.FieldType.STRING)
    private String requestMethod;
    /**
     * 请求 URL
     */
    @BizField(name = "请求 URL", type = BizField.FieldType.STRING)
    private String requestUrl;
    /**
     * 请求头 (JSON)
     */
    @BizField(name = "请求头 (JSON)", type = BizField.FieldType.STRING)
    private String requestHeaders;
    /**
     * 查询参数 (JSON)
     */
    @BizField(name = "查询参数 (JSON)", type = BizField.FieldType.STRING)
    private String requestQuery;
    /**
     * 请求体 (JSON)
     */
    @BizField(name = "请求体 (JSON)", type = BizField.FieldType.STRING)
    private String requestBody;
    /**
     * 期望的 HTTP 状态码
     */
    @BizField(name = "期望的 HTTP 状态码", type = BizField.FieldType.INTEGER)
    private Integer expectedStatus;
    /**
     * 期望的响应头 (JSON)
     */
    @BizField(name = "期望的响应头 (JSON)", type = BizField.FieldType.STRING)
    private String expectedHeaders;
    /**
     * 期望的响应体
     */
    @BizField(name = "期望的响应体", type = BizField.FieldType.STRING)
    private String expectedBody;
    /**
     * 响应体匹配类型 (EXACT/PARTIAL/REGEX)
     */
    @BizField(name = "响应体匹配类型 (EXACT/PARTIAL/REGEX)", type = BizField.FieldType.STRING)
    private String expectedBodyMatchType;
    /**
     * 断言规则 (JSON)
     */
    @BizField(name = "断言规则 (JSON)", type = BizField.FieldType.STRING)
    private String assertions;
    /**
     * 提取规则 (JSON)
     */
    @BizField(name = "提取规则 (JSON)", type = BizField.FieldType.STRING)
    private String extractRules;
    /**
     * 依赖的其他用例编码 (JSON)
     */
    @BizField(name = "依赖的其他用例编码 (JSON)", type = BizField.FieldType.STRING)
    private String dependsOn;
    /**
     * 运行次数
     */
    @BizField(name = "运行次数", type = BizField.FieldType.INTEGER)
    private Long runCount;
    /**
     * 通过次数
     */
    @BizField(name = "通过次数", type = BizField.FieldType.INTEGER)
    private Long passCount;
    /**
     * 失败次数
     */
    @BizField(name = "失败次数", type = BizField.FieldType.INTEGER)
    private Long failCount;
    /**
     * 最后一次运行时间
     */
    @BizField(name = "最后一次运行时间", type = BizField.FieldType.DATETIME)
    private Date lastRunTime;
    /**
     * 最后一次运行结果
     */
    @BizField(name = "最后一次运行结果", type = BizField.FieldType.STRING)
    private String lastRunResult;
    /**
     * 优先级 (HIGH/MEDIUM/LOW)
     */
    @BizField(name = "优先级 (HIGH/MEDIUM/LOW)", type = BizField.FieldType.STRING)
    private String priority;
    /**
     * 标签 (JSON 数组)
     */
    @BizField(name = "标签 (JSON 数组)", type = BizField.FieldType.STRING)
    private String tags;
    /**
     * 状态 (1=启用 0=禁用)
     */
    @BizField(name = "0=禁用, 1=启用", type = BizField.FieldType.INTEGER)
    private Integer status;
    /**
     * 描述信息
     */
    @BizField(name = "描述信息", type = BizField.FieldType.STRING)
    private String description;
    /**
     * 所有者 ID
     */
    @BizField(name = "所有者 ID", type = BizField.FieldType.STRING)
    private String ownerId;
    /**
     * 租户编码
     */
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;
    @TableLogic @BizField(name = "逻辑删除: 0=正常, 1=已删", type = BizField.FieldType.INTEGER)
    /**
     * 逻辑删除标识
     */
    private Integer deleted;
    /**
     * 创建时间
     */
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;
    /**
     * 更新时间
     */
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
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



