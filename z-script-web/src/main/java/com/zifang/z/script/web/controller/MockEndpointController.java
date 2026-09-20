package com.zifang.z.script.web.controller;

import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.service.MockEndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 端点管理控制器。
 * <p>
 * API 基础路径: /api/mock
 * 所属模块: z-script-web
 * 鉴权: 暂未接入统一鉴权
 * <p>
 * 主要端点:
 * <ul>
 *   <li>GET    /api/mock/list                  — 查询全部有效 Mock 端点</li>
 *   <li>GET    /api/mock/byCode?mockCode={code} — 按 mockCode 查询 Mock 端点详情</li>
 *   <li>POST   /api/mock                       — 创建 Mock 端点</li>
 *   <li>PUT    /api/mock?mockCode={code}        — 更新 Mock 端点</li>
 *   <li>DELETE /api/mock?mockCode={code}        — 删除 Mock 端点</li>
 *   <li>POST   /api/mock/toggle?mockCode={code}&status={n} — 启用/禁用 Mock 端点</li>
 * </ul>
 */
@Tag(name = "Mock端点管理")
@RestController
@RequestMapping("/api/mock")
public class MockEndpointController {

    @Resource
    private MockEndpointService mockEndpointService;

    /**
     * 查询全部有效 Mock 端点。
     *
     * @return 包含 success、data 的结果 Map；data 为有效 Mock 端点列表
     */
    @Operation(summary = "Mock端点列表")
    @GetMapping("/list")
    public Map<String, Object> list() {
        List<MockEndpoint> endpoints = mockEndpointService.listActive();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", endpoints);
        return result;
    }

    /**
     * 按 mockCode 查询 Mock 端点详情。
     *
     * @param mockCode Mock 端点编码
     * @return 存在时返回 success=true 与 data；不存在时返回 success=false 与提示信息
     */
    @Operation(summary = "Mock端点详情")
    @GetMapping("/byCode")
    public Map<String, Object> get(@RequestParam String mockCode) {
        MockEndpoint endpoint = mockEndpointService.getByMockCode(mockCode);
        Map<String, Object> result = new HashMap<>();
        if (endpoint != null) {
            result.put("success", true);
            result.put("data", endpoint);
        } else {
            result.put("success", false);
            result.put("message", "Mock端点不存在: " + mockCode);
        }
        return result;
    }

    /**
     * 创建 Mock 端点。若未指定 mockCode，会自动生成。
     *
     * @param endpoint Mock 端点实体
     * @return 成功时返回 success=true、data 与可访问路径；失败时返回 success=false 与提示信息
     */
    @Operation(summary = "创建Mock端点")
    @PostMapping
    public Map<String, Object> create(@RequestBody MockEndpoint endpoint) {
        if (endpoint.getMockCode() == null || endpoint.getMockCode().isEmpty()) {
            endpoint.setMockCode("mock-" + RandomUtil.uuidShort(8));
        }
        boolean ok = mockEndpointService.saveMockEndpoint(endpoint);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("data", ok ? endpoint : null);
        result.put("message", ok ? "创建成功，访问: /mock" + endpoint.getPath() : "创建失败");
        return result;
    }

    /**
     * 按 mockCode 更新 Mock 端点。
     *
     * @param mockCode 目标 Mock 端点编码
     * @param endpoint 待写入的 Mock 端点实体
     * @return 成功时返回 success=true；端点不存在时返回 success=false 与提示信息
     */
    @Operation(summary = "更新Mock端点")
    @PutMapping
    public Map<String, Object> update(@RequestParam String mockCode, @RequestBody MockEndpoint endpoint) {
        MockEndpoint existing = mockEndpointService.getByMockCode(mockCode);
        Map<String, Object> result = new HashMap<>();
        if (existing == null) {
            result.put("success", false);
            result.put("message", "Mock端点不存在: " + mockCode);
            return result;
        }
        endpoint.setId(existing.getId());
        boolean ok = mockEndpointService.updateMockEndpoint(endpoint);
        result.put("success", ok);
        return result;
    }

    /**
     * 按 mockCode 删除 Mock 端点。
     *
     * @param mockCode 目标 Mock 端点编码
     * @return success 表示是否删除成功
     */
    @Operation(summary = "删除Mock端点")
    @DeleteMapping
    public Map<String, Object> delete(@RequestParam String mockCode) {
        boolean ok = mockEndpointService.deleteByMockCode(mockCode);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        return result;
    }

    /**
     * 切换 Mock 端点的启用状态。{@code status=1} 启用，{@code status=0} 禁用。
     *
     * @param mockCode 目标 Mock 端点编码
     * @param status   目标状态值
     * @return 端点存在时返回 success=true 与提示信息；不存在时返回 success=false
     */
    @Operation(summary = "启用/禁用Mock端点")
    @PostMapping("/toggle")
    public Map<String, Object> toggle(@RequestParam String mockCode, @RequestParam Integer status) {
        MockEndpoint endpoint = mockEndpointService.getByMockCode(mockCode);
        Map<String, Object> result = new HashMap<>();
        if (endpoint == null) {
            result.put("success", false);
            result.put("message", "Mock端点不存在");
            return result;
        }
        endpoint.setStatus(status);
        mockEndpointService.updateMockEndpoint(endpoint);
        result.put("success", true);
        result.put("message", status == 1 ? "已启用" : "已禁用");
        return result;
    }
}
