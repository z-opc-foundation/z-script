package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.ScriptVersionDO;

import java.util.List;

public interface ScriptVersionService extends IService<ScriptVersionDO> {

    /**
     * 发布新版本 (自动将旧生产降级, 新版本变生产)
     */
    ScriptVersionDO publishVersion(Long scriptId, String scriptCode, String versionNo,
                                   String dslType, String dslContent, String outputMapping,
                                   String changeLog);

    /**
     * 列出脚本的所有版本
     */
    List<ScriptVersionDO> listByScriptId(Long scriptId);

    /**
     * 根据灰度策略选择要执行的版本
     */
    ScriptVersionDO selectVersion(Long scriptId);

    /**
     * 设置灰度 (canaryWeight 0-100)
     */
    boolean setCanary(Long versionId, Integer canaryWeight);

    /**
     * 升级灰度为生产
     */
    boolean promoteToProd(Long versionId);

    /**
     * 下线版本
     */
    boolean offline(Long versionId);
}