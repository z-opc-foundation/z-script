package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * z-script 标签关联表 (FEATURE051)
 * <p>
 * 对应表: z_script_tag_rel
 * 实际字段: script_id + tag_id (联合主键), create_time
 * 无自增 id
 */
@BizModel(name = "z-script 标签关联表", module = "z-script", description = "z-script 标签关联表 (FEATURE051) 对应表: z_script_tag_rel 实际字段: script_id + tag_id (联合主键), create_time 无自增 id")
@TableName("z_script_tag_rel")
public class ScriptTagRelDO {

    /**
     * 联合主键 (script_id, tag_id) 中的第一部分, 用作 @TableId 标识
     */
    @TableId(value = "script_id", type = IdType.INPUT)
    @BizField(name = "关联 z_script.id (联合主键第一部分, @TableId INPUT)", type = BizField.FieldType.INTEGER)
    private Long scriptId;

    @TableField("tag_id")
    @BizField(name = "关联 z_script_tag.id", type = BizField.FieldType.INTEGER)
    private Long tagId;

    @TableField("create_time")
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date createTime;

    public Long getScriptId() {
        return scriptId;
    }

    public void setScriptId(Long scriptId) {
        this.scriptId = scriptId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}



