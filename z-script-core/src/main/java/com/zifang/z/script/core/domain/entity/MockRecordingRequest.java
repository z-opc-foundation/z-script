package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 录制的请求详情 - 存储录制会话中每一个请求的完整快照
 * 用于：
 * 1. PlaybackEngine 回放（按 sequence 顺序还原请求）
 * 2. 转换为 Mock 端点（一键导入录制数据为 Mock endpoint）
 * 3. 审计/调试
 */
@TableName("z_mock_recording_request")
public class MockRecordingRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordingCode;
    private Integer sequence;
    private String requestMethod;
    private String requestUrl;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseHeaders;
    private String responseBody;
    private Long responseTime;
    private String matchedEndpoint;
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordingCode() {
        return recordingCode;
    }

    public void setRecordingCode(String v) {
        this.recordingCode = v;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer v) {
        this.sequence = v;
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

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String v) {
        this.responseHeaders = v;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String v) {
        this.responseBody = v;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long v) {
        this.responseTime = v;
    }

    public String getMatchedEndpoint() {
        return matchedEndpoint;
    }

    public void setMatchedEndpoint(String v) {
        this.matchedEndpoint = v;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date v) {
        this.createTime = v;
    }
}
