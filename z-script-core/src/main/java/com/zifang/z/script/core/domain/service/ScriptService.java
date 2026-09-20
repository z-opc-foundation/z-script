package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.Script;

import java.util.List;

public interface ScriptService extends IService<Script> {

    Script getByScriptCode(String scriptCode);

    List<Script> listByDslType(String dslType);

    List<Script> listByExposeAs(String exposeAs);

    boolean saveScript(Script script);

    boolean updateScript(Script script);

    boolean deleteByScriptCode(String scriptCode);
}
