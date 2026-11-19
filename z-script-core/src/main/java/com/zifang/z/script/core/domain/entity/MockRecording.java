package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

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
@BizModel(name = "Mock 录制会话实体", module = "z-script", description = "Mock 录制会话实体 (FEATURE051) 对应表: z_mock_recording 用途: 记录一次 Mock 录制会话的元信息")
@TableName("z_mock_recording")
public class MockRecording {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 录制会话编码
     */
    @BizField(name = "录制会话编码", type = BizField.FieldType.STRING)
    private String recordingCode;
    /**
     * 录制会话名称
     */
    @BizField(name = "录制会话名称", type = BizField.FieldType.STRING)
    private String recordingName;
    /**
     * 目标 URL (被录制的原始服务地址)
     */
    @BizField(name = "目标 URL (被录制的原始服务地址)", type = BizField.FieldType.STRING)
    private String targetUrl;
    /**
     * 录制状态 (RUNNING/STOPPED/COMPLETED)
     */
    @BizField(name = "录制状态 (RUNNING/STOPPED/COMPLETED)", type = BizField.FieldType.STRING)
    private String recordStatus;
    /**
     * 录制的请求数量
     */
    @BizField(name = "录制的请求数量", type = BizField.FieldType.INTEGER)
    private Long totalRequests;
    /**
     * 录制文件路径
     */
    @BizField(name = "录制文件路径", type = BizField.FieldType.STRING)
    private String filePath;
    /**
     * 描述信息
     */
    @BizField(name = "描述信息", type = BizField.FieldType.STRING)
    private String description;
    /**
     * 开始时间
     */
    @BizField(name = "开始时间", type = BizField.FieldType.DATETIME)
    private Date startTime;
    /**
     * 结束时间
     */
    @BizField(name = "结束时间", type = BizField.FieldType.DATETIME)
    private Date endTime;
    /**
     * 租户编码
     */
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;
    /**
     * 创建人 ID
     */
    @BizField(name = "创建人 ID", type = BizField.FieldType.STRING)
    private String creatorId;
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



