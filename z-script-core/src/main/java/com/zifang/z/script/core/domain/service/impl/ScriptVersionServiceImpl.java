package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.script.core.domain.entity.ScriptVersionDO;
import com.zifang.z.script.core.domain.mapper.ScriptVersionMapper;
import com.zifang.z.script.core.domain.service.ScriptVersionService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * z-script 版本服务实现 (FEATURE051)
 * <p>
 * 状态枚举 (varchar): DRAFT / PUBLISHED / GRAY / DEPRECATED
 * 灰度算法: grayPercentage% 的请求使用 GRAY 版本, 其它用 PUBLISHED 版本
 */
@Service
public class ScriptVersionServiceImpl extends ServiceImpl<ScriptVersionMapper, ScriptVersionDO> implements ScriptVersionService {

    private static final Random RANDOM = new Random();

    @Override
    public ScriptVersionDO publishVersion(Long scriptId, String scriptCode, String versionNo,
                                          String dslType, String dslContent, String outputMapping,
                                          String changeLog) {
        // 1. 把现有 PUBLISHED 版本降级为 DEPRECATED
        update(new LambdaUpdateWrapper<ScriptVersionDO>()
                .set(ScriptVersionDO::getStatus, "DEPRECATED")
                .eq(ScriptVersionDO::getScriptId, scriptId)
                .eq(ScriptVersionDO::getStatus, "PUBLISHED"));

        // 2. 把所有版本 is_current 设为 0
        update(new LambdaUpdateWrapper<ScriptVersionDO>()
                .set(ScriptVersionDO::getIsCurrent, 0)
                .eq(ScriptVersionDO::getScriptId, scriptId));

        // 3. 新版本默认 PUBLISHED + is_current=1
        ScriptVersionDO v = new ScriptVersionDO();
        v.setScriptId(scriptId);
        v.setVersionNo(parseVersion(versionNo));
        v.setSourceCode(dslContent);
        v.setChangelog(changeLog);
        v.setStatus("PUBLISHED");
        v.setGrayPercentage(0);
        v.setIsCurrent(1);
        v.setPublishedBy("system");
        v.setPublishedAt(new Date());
        v.setCreateTime(new Date());
        v.setUpdateTime(new Date());
        // 业务侧字段 (exist=false) 仅在内存中保留
        v.setScriptCode(scriptCode);
        v.setDslType(dslType);
        v.setOutputMapping(outputMapping);
        save(v);
        return v;
    }

    @Override
    public List<ScriptVersionDO> listByScriptId(Long scriptId) {
        return list(new LambdaQueryWrapper<ScriptVersionDO>()
                .eq(ScriptVersionDO::getScriptId, scriptId)
                .orderByDesc(ScriptVersionDO::getVersionNo));
    }

    @Override
    public ScriptVersionDO selectVersion(Long scriptId) {
        // 1. 找到当前生产版本 (is_current=1)
        ScriptVersionDO prod = getOne(new LambdaQueryWrapper<ScriptVersionDO>()
                .eq(ScriptVersionDO::getScriptId, scriptId)
                .eq(ScriptVersionDO::getIsCurrent, 1)
                .last("LIMIT 1"));

        // 2. 找到 GRAY 版本
        ScriptVersionDO gray = getOne(new LambdaQueryWrapper<ScriptVersionDO>()
                .eq(ScriptVersionDO::getScriptId, scriptId)
                .eq(ScriptVersionDO::getStatus, "GRAY")
                .last("LIMIT 1"));

        // 3. 灰度判定
        if (gray != null && gray.getGrayPercentage() != null && gray.getGrayPercentage() > 0) {
            int dice = RANDOM.nextInt(100);
            if (dice < gray.getGrayPercentage()) {
                return gray;
            }
        }
        return prod;
    }

    @Override
    public boolean setCanary(Long versionId, Integer canaryWeight) {
        if (canaryWeight < 0) {
            canaryWeight = 0;
        }

        if (canaryWeight > 100) {
            canaryWeight = 100;
        }


        ScriptVersionDO v = getById(versionId);
        if (v == null) {
            return false;
        }


        // 设为 GRAY (灰度)
        if (canaryWeight > 0) {
            return update(new LambdaUpdateWrapper<ScriptVersionDO>()
                    .set(ScriptVersionDO::getGrayPercentage, canaryWeight)
                    .set(ScriptVersionDO::getStatus, "GRAY")
                    .set(ScriptVersionDO::getIsCurrent, 0)
                    .set(ScriptVersionDO::getUpdateTime, new Date())
                    .eq(ScriptVersionDO::getId, versionId));
        } else {
            // 取消灰度 = 降级
            return update(new LambdaUpdateWrapper<ScriptVersionDO>()
                    .set(ScriptVersionDO::getGrayPercentage, 0)
                    .set(ScriptVersionDO::getStatus, "DEPRECATED")
                    .set(ScriptVersionDO::getUpdateTime, new Date())
                    .eq(ScriptVersionDO::getId, versionId));
        }
    }

    @Override
    public boolean promoteToProd(Long versionId) {
        ScriptVersionDO v = getById(versionId);
        if (v == null) {
            return false;
        }

        // 旧生产降级
        update(new LambdaUpdateWrapper<ScriptVersionDO>()
                .set(ScriptVersionDO::getStatus, "DEPRECATED")
                .set(ScriptVersionDO::getIsCurrent, 0)
                .eq(ScriptVersionDO::getScriptId, v.getScriptId())
                .eq(ScriptVersionDO::getIsCurrent, 1));
        // 当前版本升级为生产
        return update(new LambdaUpdateWrapper<ScriptVersionDO>()
                .set(ScriptVersionDO::getStatus, "PUBLISHED")
                .set(ScriptVersionDO::getIsCurrent, 1)
                .set(ScriptVersionDO::getGrayPercentage, 0)
                .set(ScriptVersionDO::getUpdateTime, new Date())
                .eq(ScriptVersionDO::getId, versionId));
    }

    @Override
    public boolean offline(Long versionId) {
        return update(new LambdaUpdateWrapper<ScriptVersionDO>()
                .set(ScriptVersionDO::getStatus, "DEPRECATED")
                .set(ScriptVersionDO::getIsCurrent, 0)
                .set(ScriptVersionDO::getUpdateTime, new Date())
                .eq(ScriptVersionDO::getId, versionId));
    }

    /**
     * 解析 versionNo 字符串 -> Integer (兼容 v1.0.0 / v2.1 / 100 等)
     */
    private Integer parseVersion(String versionNo) {
        if (versionNo == null) {
            return 1;
        }

        String num = versionNo.replaceAll("[^0-9]", "");
        if (num.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
