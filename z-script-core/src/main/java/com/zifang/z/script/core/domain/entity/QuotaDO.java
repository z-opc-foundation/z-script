package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * z-script 配额实体 (FEATURE051)
 * <p>
 * 对应表: z_script_quota
 * 用途: API Key 调用频次限制 (日级 + 分钟级 + 并发)
 */
@BizModel(name = "z-script 配额实体", module = "z-script", description = "z-script 配额实体 (FEATURE051) 对应表: z_script_quota 用途: API Key 调用频次限制 (日级 + 分钟级 + 并发)")
@TableName("z_script_quota")
public class QuotaDO {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 关联 z_script_api_key.id
     */
    @BizField(name = "关联 z_script_api_key.id", type = BizField.FieldType.INTEGER)
    private Long apiKeyId;

    /**
     * 每日最大调用次数
     */
    @BizField(name = "每日最大调用次数", type = BizField.FieldType.INTEGER)
    private Long maxPerDay;

    /**
     * 每分钟最大调用次数
     */
    @BizField(name = "每分钟最大调用次数", type = BizField.FieldType.INTEGER)
    private Long maxPerMinute;

    /**
     * 最大并发数
     */
    @BizField(name = "最大并发数", type = BizField.FieldType.INTEGER)
    private Integer maxConcurrent;

    /**
     * 今日已用
     */
    @BizField(name = "今日已用", type = BizField.FieldType.INTEGER)
    private Long usedToday;

    /**
     * 今日日期 (跨天重置标记)
     */
    @BizField(name = "今日日期 (跨天重置标记)", type = BizField.FieldType.DATETIME)
    private Date usedTodayDate;

    /**
     * 当前分钟窗口已用
     */
    @BizField(name = "当前分钟窗口已用", type = BizField.FieldType.INTEGER)
    private Long usedMinute;

    /**
     * 当前分钟窗口 (yyyyMMddHHmm)
     */
    @BizField(name = "当前分钟窗口 (yyyyMMddHHmm)", type = BizField.FieldType.STRING)
    private String usedMinuteWindow;

    /**
     * 告警阈值百分比 (默认 80)
     */
    @BizField(name = "告警阈值百分比 (默认 80)", type = BizField.FieldType.INTEGER)
    private Integer alertThreshold;

    /**
     * 上次告警时间 (防重复)
     */
    @BizField(name = "上次告警时间 (防重复)", type = BizField.FieldType.DATETIME)
    private Date alertedAt;

    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private Date updateTime;

    // ========== Getter / Setter ==========

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

    public Long getMaxPerDay() {
        return maxPerDay;
    }

    public void setMaxPerDay(Long maxPerDay) {
        this.maxPerDay = maxPerDay;
    }

    public Long getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(Long maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public Integer getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(Integer maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public Long getUsedToday() {
        return usedToday;
    }

    public void setUsedToday(Long usedToday) {
        this.usedToday = usedToday;
    }

    public Date getUsedTodayDate() {
        return usedTodayDate;
    }

    public void setUsedTodayDate(Date usedTodayDate) {
        this.usedTodayDate = usedTodayDate;
    }

    public Long getUsedMinute() {
        return usedMinute;
    }

    public void setUsedMinute(Long usedMinute) {
        this.usedMinute = usedMinute;
    }

    public String getUsedMinuteWindow() {
        return usedMinuteWindow;
    }

    public void setUsedMinuteWindow(String usedMinuteWindow) {
        this.usedMinuteWindow = usedMinuteWindow;
    }

    public Integer getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Integer alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public Date getAlertedAt() {
        return alertedAt;
    }

    public void setAlertedAt(Date alertedAt) {
        this.alertedAt = alertedAt;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}



