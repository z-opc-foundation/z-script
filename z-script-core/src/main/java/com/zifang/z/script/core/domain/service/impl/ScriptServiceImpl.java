package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.meta.api.annotation.BizMethod;
import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.core.domain.mapper.ScriptMapper;
import com.zifang.z.script.core.domain.service.ScriptService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScriptServiceImpl extends ServiceImpl<ScriptMapper, Script> implements ScriptService {

    @Override
    @BizMethod(name = "getByScriptCode", module = "z-script", description = "", operation = BizMethod.Operation.QUERY)
    public Script getByScriptCode(String scriptCode) {
        return getOne(new LambdaQueryWrapper<Script>()
                .eq(Script::getScriptCode, scriptCode));
    }

    @Override
    @BizMethod(name = "listByDslType", module = "z-script", description = "", operation = BizMethod.Operation.QUERY)
    public List<Script> listByDslType(String dslType) {
        return list(new LambdaQueryWrapper<Script>()
                .eq(Script::getDslType, dslType)
                .eq(Script::getStatus, 1)
                .orderByDesc(Script::getCreateTime));
    }

    @Override
    @BizMethod(name = "listByExposeAs", module = "z-script", description = "", operation = BizMethod.Operation.QUERY)
    public List<Script> listByExposeAs(String exposeAs) {
        return list(new LambdaQueryWrapper<Script>()
                .eq(Script::getExposeAs, exposeAs)
                .eq(Script::getStatus, 1)
                .orderByDesc(Script::getCreateTime));
    }

    @Override
    @BizMethod(name = "saveScript", module = "z-script", description = "", operation = BizMethod.Operation.CREATE)
    public boolean saveScript(Script script) {
        return save(script);
    }

    @Override
    @BizMethod(name = "updateScript", module = "z-script", description = "", operation = BizMethod.Operation.UPDATE)
    public boolean updateScript(Script script) {
        return updateById(script);
    }

    @Override
    @BizMethod(name = "deleteByScriptCode", module = "z-script", description = "", operation = BizMethod.Operation.DELETE)
    public boolean deleteByScriptCode(String scriptCode) {
        Script script = getByScriptCode(scriptCode);
        if (script == null) {
            return false;
        }

        return removeById(script.getId());
    }
}



