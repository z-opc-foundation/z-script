package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * z-script 标签实体 (FEATURE051)
 * <p>
 * 对应表: z_script_tag (生产库已存在)
 * 实际字段: id, tag_name, color, description, create_time
 */
@BizModel(name = "z-script 标签实体", module = "z-script", description = "z-script 标签实体 (FEATURE051) 对应表: z_script_tag (生产库已存在) 实际字段: id, tag_name, color, description, create_time")
@TableName("z_script_tag")
public class ScriptTagDO {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tag_name")
    @BizField(name = "标签名", type = BizField.FieldType.STRING)
    private String tagName;

    @TableField("color")
    @BizField(name = "UI 显示颜色", type = BizField.FieldType.STRING)
    private String color;

    @TableField("description")
    @BizField(name = "标签描述", type = BizField.FieldType.STRING)
    private String description;

    @TableField("create_time")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;

    // ========== 业务侧别名 (兼容 Controller/Service 调用) ==========
    @TableField(exist = false)
    @BizField(name = "tagCategory", type = BizField.FieldType.STRING)
    private String tagCategory;
    @TableField(exist = false)
    @BizField(name = "tagColor", type = BizField.FieldType.STRING)
    private String tagColor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTagColor() {
        return color;
    }

    public void setTagColor(String tagColor) {
        this.color = tagColor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getTagCategory() {
        return tagCategory;
    }

    public void setTagCategory(String tagCategory) {
        this.tagCategory = tagCategory;
    }
}



