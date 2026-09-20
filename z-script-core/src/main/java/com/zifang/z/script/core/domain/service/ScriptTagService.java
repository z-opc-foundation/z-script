package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.ScriptTagDO;

import java.util.List;

public interface ScriptTagService extends IService<ScriptTagDO> {

    ScriptTagDO createTag(String tagName, String tagCategory, String tagColor);

    List<ScriptTagDO> listAll();

    void addTagToScript(Long scriptId, Long tagId);

    void removeTagFromScript(Long scriptId, Long tagId);

    List<ScriptTagDO> listTagsOfScript(Long scriptId);

    List<Long> listScriptIdsByTag(Long tagId);

    boolean deleteTag(Long tagId);
}