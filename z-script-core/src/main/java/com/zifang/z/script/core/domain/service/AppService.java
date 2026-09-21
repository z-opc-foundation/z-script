package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.AppDO;

import java.util.List;

/**
 * z-script 应用服务接口。应用是权限模型的中心：AK 挂应用，脚本列表挂应用。
 */
public interface AppService extends IService<AppDO> {

    /**
     * 按 app_code 查询
     */
    AppDO getByAppCode(String appCode);

    /**
     * 按应用编码取应用，不存在则创建（签发第一把 Key 时的引导路径，避免先建应用再签 Key 两步走）。
     * scope 取签 Key 请求里的值（控制台传 ALL），仅在建应用那一刻生效；已有应用不受影响。
     */
    AppDO findOrCreate(String appCode, String ownerId, String scope);

    /**
     * 校验应用对某脚本的访问权（ALL 放行；SPECIFIC 看 allowed_scripts JSON 是否包含 scriptCode）
     */
    boolean verifyScope(AppDO app, String scriptCode);

    /**
     * 覆盖应用的脚本列表（scope 置 SPECIFIC）；传 scope=ALL 时只改范围、不清列表
     */
    boolean saveScripts(String appCode, String scope, List<String> scripts);

    /**
     * 列出所有启用的应用
     */
    List<AppDO> listEnabled();
}
