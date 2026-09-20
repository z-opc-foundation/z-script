package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Mock 录制会话实体 (FEATURE051)
 * <p>
 * 对应表: z_mock_recording
 * 用途: 记录一次 Mock 录制会话的元信息
 *
 * @author zifang
 * @since 1.0.0
 */
@TableName("z_mock_recording")
public class MockRecording {

    @TableId(type = IdType.AUTO)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 录制会话编码
     */
    private String recordingCode;
    /**
     * 录制会话名称
     */
    private String recordingName;
    /**
     * 目标 URL (被录制的原始服务地址)
     */
    private String targetUrl;
    /**
     * 录制状态 (RUNNING/STOPPED/COMPLETED)
     */
    private String recordStatus;
    /**
     * 录制的请求数量
     */
    private Long totalRequests;
    /**
     * 录制文件路径
     */
    private String filePath;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 开始时间
     */
    private Date startTime;
    /**
     * 结束时间
     */
    private Date endTime;
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

    public String getRecordingCode() {
        return recordingCode;
    }

    public void setRecordingCode(String v) {
        this.recordingCode = v;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String v) {
        this.recordingName = v;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String v) {
        this.targetUrl = v;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(String v) {
        this.recordStatus = v;
    }

    public Long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Long v) {
        this.totalRequests = v;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String v) {
        this.filePath = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date v) {
        this.startTime = v;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date v) {
        this.endTime = v;
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
