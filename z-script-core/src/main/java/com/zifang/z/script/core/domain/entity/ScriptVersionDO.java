package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * z-script 版本实体 (FEATURE051)
 * <p>
 * 对应表: z_script_version (生产库已存在)
 * 实际字段: id, script_id, version_no(int), source_code, changelog,
 * status, gray_percentage, gray_api_keys, published_by, published_at,
 * is_current, create_time, update_time
 */
@BizModel(name = "z-script 版本实体", module = "z-script", description = "z-script 版本实体 (FEATURE051) 对应表: z_script_version (生产库已存在) 实际字段: id, script_id, version_no(int), source_code, changelog, status, gray_percentage, gray_api_keys, published_by, published_at, is_current, create_time, update_time")
@TableName("z_script_version")
public class ScriptVersionDO {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("script_id")
    @BizField(name = "关联 z_script.id", type = BizField.FieldType.INTEGER)
    private Long scriptId;

    @TableField("version_no")
    @BizField(name = "版本号 (1, 2, 3...)", type = BizField.FieldType.INTEGER)
    private Integer versionNo;

    @TableField("source_code")
    @BizField(name = "该版本源代码", type = BizField.FieldType.STRING)
    private String sourceCode;

    @TableField("changelog")
    @BizField(name = "变更说明", type = BizField.FieldType.STRING)
    private String changelog;

    /**
     * DRAFT/PUBLISHED/DEPRECATED
     */
    @TableField("status")
    @BizField(name = "DRAFT/PUBLISHED/DEPRECATED", type = BizField.FieldType.STRING)
    private String status;

    @TableField("gray_percentage")
    @BizField(name = "灰度比例 0-100", type = BizField.FieldType.INTEGER)
    private Integer grayPercentage;

    @TableField("gray_api_keys")
    @BizField(name = "指定灰度的 API Key ID 列表 (JSON)", type = BizField.FieldType.STRING)
    private String grayApiKeys;

    @TableField("published_by")
    @BizField(name = "发布人", type = BizField.FieldType.STRING)
    private String publishedBy;

    @TableField("published_at")
    @BizField(name = "发布时间", type = BizField.FieldType.DATETIME)
    private Date publishedAt;

    @TableField("is_current")
    @BizField(name = "是否当前生效版本", type = BizField.FieldType.INTEGER)
    private Integer isCurrent;

    @TableField("create_time")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;

    @TableField("update_time")
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private Date updateTime;

    // ========== 业务侧别名 (兼容 Controller/Service 调用) ==========
    @TableField(exist = false)
    @BizField(name = "scriptCode", type = BizField.FieldType.STRING)
    private String scriptCode;
    @TableField(exist = false)
    @BizField(name = "dslType", type = BizField.FieldType.STRING)
    private String dslType;
    @TableField(exist = false)
    @BizField(name = "dslContent", type = BizField.FieldType.STRING)
    private String dslContent;
    @TableField(exist = false)
    @BizField(name = "outputMapping", type = BizField.FieldType.STRING)
    private String outputMapping;
    @TableField(exist = false)
    @BizField(name = "canaryWeight", type = BizField.FieldType.INTEGER)
    private Integer canaryWeight;
    @TableField(exist = false)
    @BizField(name = "changeLog", type = BizField.FieldType.STRING)
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



