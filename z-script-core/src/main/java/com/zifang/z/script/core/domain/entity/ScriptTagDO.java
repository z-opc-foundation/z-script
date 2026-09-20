package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * z-script 标签实体 (FEATURE051)
 * <p>
 * 对应表: z_script_tag (生产库已存在)
 * 实际字段: id, tag_name, color, description, create_time
 */
@TableName("z_script_tag")
public class ScriptTagDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tag_name")
    private String tagName;

    @TableField("color")
    private String color;

    @TableField("description")
    private String description;

    @TableField("create_time")
    private Date createTime;

    // ========== 业务侧别名 (兼容 Controller/Service 调用) ==========
    @TableField(exist = false)
    private String tagCategory;
    @TableField(exist = false)
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
