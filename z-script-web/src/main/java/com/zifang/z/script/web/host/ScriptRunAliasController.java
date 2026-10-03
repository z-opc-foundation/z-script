package com.zifang.z.script.web.host;

import com.zifang.util.core.meta.Result;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 前端别名控制器 (2026-10-03 自 z-opc main-starter 的 E2EAliasController script-run 段平移):
 * 前端 makeApi('scriptRun') 调 {@code /api/script-run/list}, 真实历史表 {@code z_script_execution_history}
 * 尚未建 — 当前返空列表兜底, 等表加好 SQL 之后改走 z-script 自带执行历史接口.
 * <p>
 * 开关 z.script.host.enabled (默认关): 寄生 all-in-one 模式由宿主打开,
 * standalone 模式不开 (独立部署的前端不走 z-opc 这条别名).
 */
@RestController
@ConditionalOnProperty(prefix = "z.script.host", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ScriptRunAliasController {

    @GetMapping("/api/script-run/list")
    public Result<List<Map<String, Object>>> scriptRunHistory() {
        return Result.success(new ArrayList<>());
    }
}