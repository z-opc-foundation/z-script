package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * z-script 版本实体 (FEATURE051)
 * <p>
 * 对应表: z_script_version (生产库已存在)
 * 实际字段: id, script_id, version_no(int), source_code, changelog,
 * status, gray_percentage, gray_api_keys, published_by, published_at,
 * is_current, create_time, update_time
 */
@TableName("z_script_version")
public class ScriptVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("script_id")
    private Long scriptId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("source_code")
    private String sourceCode;

    @TableField("changelog")
    private String changelog;

    /**
     * DRAFT/PUBLISHED/DEPRECATED
     */
    @TableField("status")
    private String status;

    @TableField("gray_percentage")
    private Integer grayPercentage;

    @TableField("gray_api_keys")
    private String grayApiKeys;

    @TableField("published_by")
    private String publishedBy;

    @TableField("published_at")
    private Date publishedAt;

    @TableField("is_current")
    private Integer isCurrent;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    // ========== 业务侧别名 (兼容 Controller/Service 调用) ==========
    @TableField(exist = false)
    private String scriptCode;
    @TableField(exist = false)
    private String dslType;
    @TableField(exist = false)
    private String dslContent;
    @TableField(exist = false)
    private String outputMapping;
    @TableField(exist = false)
    private Integer canaryWeight;
    @TableField(exist = false)
    private String changeLog;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScriptId() {
        return scriptId;
    }

    public void setScriptId(Long scriptId) {
        this.scriptId = scriptId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getVersionNoStr() {
        return versionNo == null ? null : String.valueOf(versionNo);
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getDslContent() {
        return sourceCode;
    }

    public void setDslContent(String dslContent) {
        this.sourceCode = dslContent;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public String getChangeLog() {
        return changelog;
    }

    public void setChangeLog(String changeLog) {
        this.changelog = changeLog;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getGrayPercentage() {
        return grayPercentage;
    }

    public void setGrayPercentage(Integer grayPercentage) {
        this.grayPercentage = grayPercentage;
    }

    public Integer getCanaryWeight() {
        return grayPercentage;
    }

    public void setCanaryWeight(Integer canaryWeight) {
        this.grayPercentage = canaryWeight;
    }

    public String getGrayApiKeys() {
        return grayApiKeys;
    }

    public void setGrayApiKeys(String grayApiKeys) {
        this.grayApiKeys = grayApiKeys;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Integer isCurrent) {
        this.isCurrent = isCurrent;
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

    public String getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
    }

    public String getDslType() {
        return dslType;
    }

    public void setDslType(String dslType) {
        this.dslType = dslType;
    }

    public String getOutputMapping() {
        return outputMapping;
    }

    public void setOutputMapping(String outputMapping) {
        this.outputMapping = outputMapping;
    }
}
