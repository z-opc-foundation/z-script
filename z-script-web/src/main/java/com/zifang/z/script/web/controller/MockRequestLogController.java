package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.script.core.domain.entity.MockRequestLog;
import com.zifang.z.script.core.domain.mapper.MockRequestLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 请求日志 API。
 * <p>
 * API 基础路径: /api/mock-platform/request-logs
 * 所属模块: z-script-web
 * 鉴权: 暂未接入统一鉴权
 * <p>
 * 提供:
 * <ul>
 *   <li>最近的请求列表</li>
 *   <li>按路径/mockCode/状态过滤</li>
 *   <li>未匹配请求统计</li>
 *   <li>按 mockCode 的响应时间分布</li>
 * </ul>
 */
@Tag(name = "Mock-请求日志")
@RestController
@RequestMapping("/api/mock-platform/request-logs")
public class MockRequestLogController {

    @Autowired
    private MockRequestLogMapper logMapper;

    /**
     * 查询最近的请求日志，支持按路径、mockCode、matched 过滤；limit 最大 500。
     *
     * @param path     路径模糊匹配，可选
     * @param mockCode 端点编码精确匹配，可选
     * @param matched  1=已匹配，0=未匹配，可选
     * @param limit    返回条数上限，默认 50
     * @return MockRequestLog 列表
     */
    @Operation(summary = "请求日志列表")
    @GetMapping("/list")
    public Object list(@RequestParam(required = false) String path,
                       @RequestParam(required = false) String mockCode,
                       @RequestParam(required = false) Integer matched,
                       @RequestParam(required = false, defaultValue = "50") Integer limit) {
        QueryWrapper<MockRequestLog> qw = new QueryWrapper<>();
        if (path != null && !path.isEmpty()) {
            qw.like("request_path", path);
        }

        if (mockCode != null && !mockCode.isEmpty()) {
            qw.eq("mock_code", mockCode);
        }

        if (matched != null) {
            qw.eq("matched", matched);
        }

        qw.orderByDesc("id").last("LIMIT " + Math.min(limit, 500));
        return logMapper.selectList(qw);
    }

    /**
     * 按请求路径维度查询历史日志，limit 最大 200。
     *
     * @param path  请求路径
     * @param limit 返回条数上限，默认 20
     * @return MockRequestLog 列表
     */
    @Operation(summary = "按路径查询请求日志")
    @GetMapping("/by-path")
    public Object byPath(@RequestParam String path,
                         @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return logMapper.selectByPath(path, Math.min(limit, 200));
    }

    /**
     * 请求日志统计概览：总数、匹配数、未匹配数以及按 mockCode 的耗时分布（Top 20）。
     *
     * @return 包含 total、matched、unmatched、byMockCode 的 Map
     */
    @Operation(summary = "请求日志统计概览")
    @GetMapping("/stats")
    public Object stats() {
        Map<String, Object> s = new HashMap<>();
        s.put("total", logMapper.selectCount(null));
        s.put("matched", logMapper.selectCount(new QueryWrapper<MockRequestLog>().eq("matched", 1)));
        s.put("unmatched", logMapper.countUnmatched());
        s.put("byMockCode", logMapper.statsByMockCode(20));
        return s;
    }

    /**
     * 清理过期日志，仅保留最近 N 条（按 id 降序）。{@code keepLast} 缺省 1000。
     *
     * @param keepLast 保留的最近条数，可选，默认 1000
     * @return 包含 success、deleted、remaining 的结果 Map
     */
    @Operation(summary = "清理过期请求日志")
    @DeleteMapping("/clear")
    public Object clear(@RequestParam(required = false) Integer keepLast) {
        if (keepLast == null) {
            keepLast = 1000;
        }

        // 仅保留最近 N 条
        List<MockRequestLog> all = logMapper.selectList(
                new QueryWrapper<MockRequestLog>().orderByDesc("id"));
        int deleted = 0;
        if (all.size() > keepLast) {
            for (int i = keepLast; i < all.size(); i++) {
                logMapper.deleteById(all.get(i).getId());
                deleted++;
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("deleted", deleted);
        r.put("remaining", all.size() - deleted);
        return r;
    }
}
