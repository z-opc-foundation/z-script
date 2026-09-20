package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.script.core.domain.entity.ScriptTagDO;
import com.zifang.z.script.core.domain.entity.ScriptTagRelDO;
import com.zifang.z.script.core.domain.mapper.ScriptTagMapper;
import com.zifang.z.script.core.domain.mapper.ScriptTagRelMapper;
import com.zifang.z.script.core.domain.service.ScriptTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * z-script 标签服务实现 (FEATURE051)
 */
@Service
public class ScriptTagServiceImpl extends ServiceImpl<ScriptTagMapper, ScriptTagDO> implements ScriptTagService {

    @Autowired
    private ScriptTagRelMapper tagRelMapper;

    @Override
    public ScriptTagDO createTag(String tagName, String tagCategory, String tagColor) {
        ScriptTagDO tag = new ScriptTagDO();
        tag.setTagName(tagName);
        tag.setColor(tagColor != null ? tagColor : "blue");
        tag.setDescription(tagCategory);
        tag.setCreateTime(new Date());
        save(tag);
        return tag;
    }

    @Override
    public List<ScriptTagDO> listAll() {
        return list(new LambdaQueryWrapper<ScriptTagDO>().orderByAsc(ScriptTagDO::getId));
    }

    @Override
    public void addTagToScript(Long scriptId, Long tagId) {
        // 防重复
        Long exist = tagRelMapper.selectCount(new LambdaQueryWrapper<ScriptTagRelDO>()
                .eq(ScriptTagRelDO::getScriptId, scriptId)
                .eq(ScriptTagRelDO::getTagId, tagId));
        if (exist != null && exist > 0) {
            return;
        }

        ScriptTagRelDO rel = new ScriptTagRelDO();
        rel.setScriptId(scriptId);
        rel.setTagId(tagId);
        rel.setCreateTime(new Date());
        tagRelMapper.insert(rel);
    }

    @Override
    public void removeTagFromScript(Long scriptId, Long tagId) {
        tagRelMapper.delete(new LambdaQueryWrapper<ScriptTagRelDO>()
                .eq(ScriptTagRelDO::getScriptId, scriptId)
                .eq(ScriptTagRelDO::getTagId, tagId));
    }

    @Override
    public List<ScriptTagDO> listTagsOfScript(Long scriptId) {
        List<ScriptTagRelDO> rels = tagRelMapper.selectList(new LambdaQueryWrapper<ScriptTagRelDO>()
                .eq(ScriptTagRelDO::getScriptId, scriptId));
        if (rels == null || rels.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> tagIds = rels.stream().map(ScriptTagRelDO::getTagId).collect(Collectors.toList());
        return listByIds(tagIds);
    }

    @Override
    public List<Long> listScriptIdsByTag(Long tagId) {
        List<ScriptTagRelDO> rels = tagRelMapper.selectList(new LambdaQueryWrapper<ScriptTagRelDO>()
                .eq(ScriptTagRelDO::getTagId, tagId));
        if (rels == null || rels.isEmpty()) {
            return new ArrayList<>();
        }
        return rels.stream().map(ScriptTagRelDO::getScriptId).collect(Collectors.toList());
    }

    @Override
    public boolean deleteTag(Long tagId) {
        // 先删关联
        tagRelMapper.delete(new LambdaQueryWrapper<ScriptTagRelDO>()
                .eq(ScriptTagRelDO::getTagId, tagId));
        return removeById(tagId);
    }
}
