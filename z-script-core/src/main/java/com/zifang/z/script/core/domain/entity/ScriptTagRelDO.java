package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * z-script 标签关联表 (FEATURE051)
 * <p>
 * 对应表: z_script_tag_rel
 * 实际字段: script_id + tag_id (联合主键), create_time
 * 无自增 id
 */
@TableName("z_script_tag_rel")
public class ScriptTagRelDO {

    /**
     * 联合主键 (script_id, tag_id) 中的第一部分, 用作 @TableId 标识
     */
    @TableId(value = "script_id", type = IdType.INPUT)
    private Long scriptId;

    @TableField("tag_id")
    private Long tagId;

    @TableField("create_time")
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
