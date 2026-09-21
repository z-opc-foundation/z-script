package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.script.core.domain.entity.AppDO;
import com.zifang.z.script.core.domain.mapper.AppMapper;
import com.zifang.z.script.core.domain.service.AppService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * z-script 应用服务实现。应用是权限模型的中心：AK 挂应用、脚本列表挂应用。
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, AppDO> implements AppService {

    private static final Logger log = LogManager.getLogger(AppServiceImpl.class);

    @Override
    public AppDO getByAppCode(String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<AppDO>()
                .eq(AppDO::getAppCode, appCode)
                .last("LIMIT 1"));
    }

    @Override
    public AppDO findOrCreate(String appCode, String ownerId, String scope) {
        AppDO existing = getByAppCode(appCode);
        if (existing != null) {
            return existing;
        }
        AppDO app = new AppDO();
        app.setAppCode(appCode);
        app.setAppName(appCode);
        app.setOwnerId(ownerId);
        // scope 跟签 Key 请求走（控制台引导传 ALL）；显式请求 SPECIFIC 时列表为空，
        // 需要再调 saveScripts 配置可访问的脚本。
        app.setScope(scope == null || scope.trim().isEmpty() ? "ALL" : scope.trim());
        app.setAllowedScripts("[]");
        app.setStatus(1);
        app.setTenantCode("default");
        app.setCreateTime(new Date());
        app.setUpdateTime(new Date());
        save(app);
        log.info("应用自动创建(签 Key 引导): appCode={}, scope={}, ownerId={}", appCode, app.getScope(), ownerId);
        return app;
    }

    @Override
    public boolean verifyScope(AppDO app, String scriptCode) {
        String scope = app.getScope();
        if (scope == null || "ALL".equals(scope)) {
            return true;
        }
        if ("READ_ONLY".equals(scope)) {
            return true;
        }
        if ("SPECIFIC".equals(scope)) {
            String allowed = app.getAllowedScripts();
            if (allowed == null || allowed.isEmpty()) {
                return false;
            }
            try {
                List<String> scripts = JsonUtil.fromJson(allowed, List.class);
                return scripts != null && scriptCode != null && scripts.contains(scriptCode);
            } catch (Exception e) {
                log.error("应用 allowed_scripts 解析失败: appCode={}, allowed={}", app.getAppCode(), allowed, e);
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean saveScripts(String appCode, String scope, List<String> scripts) {
        AppDO app = getByAppCode(appCode);
        if (app == null) {
            return false;
        }
        if ("ALL".equals(scope)) {
            app.setScope("ALL");
        } else {
            app.setScope("SPECIFIC");
            app.setAllowedScripts(scripts == null ? "[]" : JsonUtil.toJson(scripts));
        }
        app.setUpdateTime(new Date());
        return updateById(app);
    }

    @Override
    public List<AppDO> listEnabled() {
        return list(new LambdaQueryWrapper<AppDO>().eq(AppDO::getStatus, 1));
    }
}
