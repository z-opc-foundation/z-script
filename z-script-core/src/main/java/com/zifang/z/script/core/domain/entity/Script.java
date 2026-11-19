package com.zifang.z.script.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * z-script 脚本实体 (FEATURE051)
 * <p>
 * 对应表: z_script
 * 用途: 存储脚本定义、DSL 类型、源码、暴露方式等核心信息
 *
 * @author zifang
 * @since 1.0.0
 */
@BizModel(name = "z-script 脚本实体", module = "z-script", description = "z-script 脚本实体 (FEATURE051) 对应表: z_script 用途: 存储脚本定义、DSL 类型、源码、暴露方式等核心信息")
@TableName("z_script")
public class Script {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键 ID", type = BizField.FieldType.INTEGER)
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 脚本唯一编码 (全局唯一)
     */
    @BizField(name = "脚本唯一编码 (全局唯一)", type = BizField.FieldType.STRING)
    private String scriptCode;
    /**
     * 脚本名称
     */
    @BizField(name = "脚本名称", type = BizField.FieldType.STRING)
    private String scriptName;
    /**
     * DSL 类型 (GROOVY/GRAVITON/EL 等)
     */
    @BizField(name = "DSL 类型 (GROOVY/GRAVITON/EL 等)", type = BizField.FieldType.STRING)
    private String dslType;
    /**
     * 脚本源代码
     */
    @BizField(name = "脚本源代码", type = BizField.FieldType.STRING)
    private String sourceCode;
    /**
     * 暴露方式 (API/MCP/HTTP)
     */
    @BizField(name = "暴露方式 (API/MCP/HTTP)", type = BizField.FieldType.STRING)
    private String exposeAs;
    /**
     * HTTP 路径
     */
    @BizField(name = "HTTP 路径", type = BizField.FieldType.STRING)
    private String httpPath;
    /**
     * MCP 工具名称
     */
    @BizField(name = "MCP 工具名称", type = BizField.FieldType.STRING)
    private String mcpToolName;
    /**
     * 输入参数 Schema (JSON)
     */
    @BizField(name = "输入参数 Schema (JSON)", type = BizField.FieldType.STRING)
    private String inputSchema;
    /**
     * 输出结果 Schema (JSON)
     */
    @BizField(name = "输出结果 Schema (JSON)", type = BizField.FieldType.STRING)
    private String outputSchema;
    /**
     * 版本号
     */
    @BizField(name = "版本号", type = BizField.FieldType.STRING)
    private String version;
    /**
     * 描述信息
     */
    @BizField(name = "描述信息", type = BizField.FieldType.STRING)
    private String description;
    /**
     * 状态 (1=启用 0=禁用)
     */
    @BizField(name = "0=草稿, 1=启用, 2=禁用", type = BizField.FieldType.INTEGER)
    private Integer status;
    /**
     * 创建人 ID
     */
    @BizField(name = "创建人 ID", type = BizField.FieldType.STRING)
    private String creatorId;
    /**
     * 租户编码
     */
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;
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

    public String getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getDslType() {
        return dslType;
    }

    public void setDslType(String dslType) {
        this.dslType = dslType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getExposeAs() {
        return exposeAs;
    }

    public void setExposeAs(String exposeAs) {
        this.exposeAs = exposeAs;
    }

    public String getHttpPath() {
        return httpPath;
    }

    public void setHttpPath(String httpPath) {
        this.httpPath = httpPath;
    }

    public String getMcpToolName() {
        return mcpToolName;
    }

    public void setMcpToolName(String mcpToolName) {
        this.mcpToolName = mcpToolName;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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



