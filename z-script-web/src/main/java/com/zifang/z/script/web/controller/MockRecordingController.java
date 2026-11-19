package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.entity.MockRecording;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.engine.PlaybackEngine;
import com.zifang.z.script.engine.RecorderEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 录制与回放 API。
 * <p>
 * API 基础路径: /api/mock-platform/recordings
 * 所属模块: z-script-web
 * 鉴权: 暂未接入统一鉴权
 * <p>
 * 主要端点:
 * <ul>
 *   <li>POST   /start                       — 开启录制会话</li>
 *   <li>POST   /stop                        — 停止录制</li>
 *   <li>GET    /list                        — 列出所有录制会话</li>
 *   <li>GET    /byCode?recordingCode={code}  — 查看录制详情</li>
 *   <li>GET    /requests?recordingCode={code} — 查看录制中所有请求</li>
 *   <li>POST   /export?recordingCode={code}   — 录制转换为 Mock 端点草稿（不落库）</li>
 *   <li>POST   /import?recordingCode={code}   — 录制转换为 Mock 并入库</li>
 *   <li>POST   /playback?recordingCode={code} — 回放到目标服务</li>
 *   <li>POST   /compare?recordingCode={code}  — 录制与目标服务响应对比</li>
 *   <li>DELETE /?recordingCode={code}        — 删除录制及关联 Mock 端点</li>
 *   <li>GET    /mocks?recordingCode={code}    — 查询录制关联的 Mock 端点</li>
 * </ul>
 */
@Tag(name = "Mock-录制与回放")
@RestController
@RequestMapping("/api/mock-platform/recordings")
public class MockRecordingController {

    @Autowired
    private RecorderEngine recorder;
    @Autowired
    private PlaybackEngine playback;
    @Autowired
    private MockEndpointMapper endpointMapper;

    /**
     * 构造有序 Map 的辅助方法，按入参顺序保留键值对。
     *
     * @param kvs 变长参数，按 key1, value1, key2, value2 ... 排列
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 按插入顺序排列的 LinkedHashMap
     */
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> mapOf(Object... kvs) {
        Map<K, V> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            m.put((K) kvs[i], (V) kvs[i + 1]);
        }
        return m;
    }

    /**
     * 开启录制
     * POST /api/mock-platform/recordings/start
     * body: { recordingCode, recordingName, targetUrl }
     */
    @Operation(summary = "开启录制")
    @PostMapping("/start")
    @BizApi(summary = "开启录制", description = "开启录制 POST /api/mock-platform/recordings/start body: { recordingCode, recordingName, targetUrl }", tags = { "z-script" })
    public Object startRecording(@RequestBody @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String code = body.get("recordingCode");
        String name = body.get("recordingName");
        String target = body.get("targetUrl");
        if (code == null || code.isEmpty()) {
            return mapOf("success", false, "message", "recordingCode required");
        }
        MockRecording rec = recorder.startRecording(code, name, target);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("recordingCode", rec.getRecordingCode());
        resp.put("status", rec.getRecordStatus());
        resp.put("targetUrl", rec.getTargetUrl());
        resp.put("message", "Recording started. Any request to /api/mock/" + code + "/** will be captured.");
        return resp;
    }

    /**
     * 停止录制
     */
    @Operation(summary = "停止录制")
    @PostMapping("/stop")
    @BizApi(summary = "停止录制", tags = { "z-script" })
    public Object stopRecording(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode) {
        MockRecording rec = recorder.stopRecording(recordingCode);
        if (rec == null) {
            return mapOf("success", false, "message", "Recording not found");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("recordingCode", rec.getRecordingCode());
        resp.put("status", rec.getRecordStatus());
        resp.put("totalRequests", rec.getTotalRequests());
        return resp;
    }

    /**
     * 列出所有录制会话
     */
    @Operation(summary = "录制会话列表")
    @GetMapping("/list")
    @BizApi(summary = "列出所有录制会话", tags = { "z-script" })
    public Object listRecordings(@RequestParam(required = false) @BizParam(name = "recordStatus", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordStatus) {
        QueryWrapper<MockRecording> qw = new QueryWrapper<>();
        if (recordStatus != null && !recordStatus.isEmpty()) {
            qw.eq("record_status", recordStatus);
        }

        qw.orderByDesc("id");
        return recorder.listRecordings();
    }

    /**
     * 获取录制详情
     */
    @Operation(summary = "录制详情")
    @GetMapping("/byCode")
    @BizApi(summary = "获取录制详情", tags = { "z-script" })
    public Object getRecording(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode) {
        return recorder.listRecordings().stream()
                .filter(r -> recordingCode.equals(r.getRecordingCode()))
                .findFirst().orElse(null);
    }

    /**
     * 获取录制中所有请求
     */
    @Operation(summary = "录制请求列表")
    @GetMapping("/requests")
    @BizApi(summary = "获取录制中所有请求", tags = { "z-script" })
    public Object getRecordingRequests(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode) {
        return recorder.getRecordingRequests(recordingCode);
    }

    /**
     * 录制 → Mock endpoint 草稿 (不直接入库)
     * POST /api/mock-platform/recordings/{code}/export
     * body: { projectCode? }
     */
    @Operation(summary = "导出录制为Mock端点草稿")
    @PostMapping("/export")
    @BizApi(summary = "录制 → Mock endpoint 草稿 (不直接入库)", description = "录制 → Mock endpoint 草稿 (不直接入库) POST /api/mock-platform/recordings/{code}/export body: { projectCode? }", tags = { "z-script" })
    public Object exportAsMock(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode,
                               @RequestBody(required = false)
                               @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String projectCode = body == null ? null : body.get("projectCode");
        List<MockEndpoint> drafts = recorder.exportAsMockEndpoints(recordingCode, projectCode);
        return mapOf(
                "success", true,
                "drafts", drafts,
                "totalUnique", drafts.size());
    }

    /**
     * 把录制转换为 Mock 并入库
     * POST /api/mock-platform/recordings/{code}/import
     * body: { envCode?, projectCode? }
     */
    @Operation(summary = "导入录制为Mock端点")
    @PostMapping("/import")
    @BizApi(summary = "把录制转换为 Mock 并入库", description = "把录制转换为 Mock 并入库 POST /api/mock-platform/recordings/{code}/import body: { envCode?, projectCode? }", tags = { "z-script" })
    public Object importAsMock(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode,
                               @RequestBody(required = false)
                               @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String envCode = body == null ? null : body.get("envCode");
        String projectCode = body == null ? null : body.get("projectCode");
        return playback.playbackToMock(recordingCode, envCode, projectCode);
    }

    /**
     * 回放录制到目标服务（验证录制有效）
     * POST /api/mock-platform/recordings/{code}/playback
     * body: { targetUrl }
     */
    @Operation(summary = "回放录制到目标服务")
    @PostMapping("/playback")
    @BizApi(summary = "回放录制到目标服务（验证录制有效）", description = "回放录制到目标服务（验证录制有效） POST /api/mock-platform/recordings/{code}/playback body: { targetUrl }", tags = { "z-script" })
    public Object playback(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode,
                           @RequestBody @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String target = body.get("targetUrl");
        if (target == null || target.isEmpty()) {
            return mapOf("success", false, "message", "targetUrl required");
        }
        return playback.playbackAsScript(recordingCode, target);
    }

    /**
     * 回放并对比响应（基线对比）
     */
    @Operation(summary = "回放并对比响应(基线对比)")
    @PostMapping("/compare")
    @BizApi(summary = "回放并对比响应（基线对比）", tags = { "z-script" })
    public Object compare(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode,
                          @RequestBody @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String target = body.get("targetUrl");
        if (target == null || target.isEmpty()) {
            return mapOf("success", false, "message", "targetUrl required");
        }
        return playback.playbackCompare(recordingCode, target);
    }

    /**
     * 删除录制（级联删除请求详情）
     */
    @Operation(summary = "删除录制及关联Mock端点")
    @DeleteMapping
    @BizApi(summary = "删除录制（级联删除请求详情）", tags = { "z-script" })
    public Object deleteRecording(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode) {
        boolean ok = recorder.deleteRecording(recordingCode);
        // 同时清理关联的 Mock endpoints
        int removed = endpointMapper.delete(new QueryWrapper<MockEndpoint>().eq("source_recording", recordingCode));
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", ok);
        resp.put("removedMockEndpoints", removed);
        return resp;
    }

    /**
     * 查询某录制关联的 Mock endpoints
     */
    @Operation(summary = "查询录制关联的Mock端点")
    @GetMapping("/mocks")
    @BizApi(summary = "查询某录制关联的 Mock endpoints", tags = { "z-script" })
    public Object listMocksByRecording(@RequestParam @BizParam(name = "recordingCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String recordingCode) {
        return endpointMapper.selectList(new QueryWrapper<MockEndpoint>().eq("source_recording", recordingCode));
    }
}



