package com.zifang.z.script.engine;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.entity.MockRecording;
import com.zifang.z.script.core.domain.entity.MockRecordingRequest;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.core.domain.mapper.MockRecordingMapper;
import com.zifang.z.script.core.domain.mapper.MockRecordingRequestMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 回放引擎 (Playback) - 录制数据回放器
 * <p>
 * 核心能力:
 * 1. playbackAsScript()   - 把录制作为 "脚本" 顺序回放（验证录制有效）
 * 2. playbackToMock()     - 把录制转换为 Mock endpoints（一键 Mock 化）
 * 3. playbackCompare()    - 回放并对比新环境响应（基线对比）
 * <p>
 * 重构后: HTTP 发送统一委托 DynamicApiExecutor（OkHttp + SSE + 异步）。
 */
@Component
public class PlaybackEngine {

    private static final Logger log = LogManager.getLogger(PlaybackEngine.class);

    @Autowired
    private MockRecordingMapper recordingMapper;
    @Autowired
    private MockRecordingRequestMapper requestMapper;
    @Autowired
    private MockEndpointMapper endpointMapper;
    @Autowired
    private DynamicApiExecutor executor;

    /**
     * 顺序回放录制的所有请求到一个目标 URL
     * 用于验证录制是否可重放，或者在另一个环境复现流量
     */
    public List<Map<String, Object>> playbackAsScript(String recordingCode, String targetBaseUrl) {
        MockRecording rec = recordingMapper.selectOne(
                new QueryWrapper<MockRecording>().eq("recording_code", recordingCode));
        if (rec == null) {
            throw new IllegalArgumentException("Recording not found: " + recordingCode);
        }

        List<MockRecordingRequest> requests = requestMapper.selectByRecordingCode(recordingCode);
        List<Map<String, Object>> results = new ArrayList<>();
        int pass = 0, fail = 0;

        for (MockRecordingRequest req : requests) {
            Map<String, Object> result = new HashMap<>();
            result.put("sequence", req.getSequence());
            result.put("method", req.getRequestMethod());
            result.put("url", req.getRequestUrl());

            try {
                // 委托给 DynamicApiExecutor
                ApiExecutionResult r = executor.executeByMethodUrl(
                        req.getRequestMethod(),
                        targetBaseUrl + req.getRequestUrl(),
                        req.getRequestHeaders() == null ? null
                                : JsonUtil.fromJson(req.getRequestHeaders(), new TypeReference<Map<String, String>>() {
                        }),
                        req.getRequestBody());

                result.put("status", r.isSuccess() ? "PASS" : "FAIL");
                result.put("actualStatus", r.getStatus());
                result.put("durationMs", r.getDurationMs());
                result.put("responseSize", r.getBodySize());
                if (r.getError() != null) {
                    result.put("error", r.getError());
                }

                if (r.isSuccess()) {
                    pass++;
                } else {
                    fail++;
                }
            } catch (Exception e) {
                result.put("status", "FAIL");
                result.put("error", e.getMessage());
                fail++;
            }
            results.add(result);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("recordingCode", recordingCode);
        summary.put("target", targetBaseUrl);
        summary.put("total", requests.size());
        summary.put("pass", pass);
        summary.put("fail", fail);
        summary.put("results", results);
        log.info("Playback {} to {} : {} pass, {} fail", recordingCode, targetBaseUrl, pass, fail);
        return Collections.singletonList(summary);
    }

    /**
     * 把录制转换为 Mock endpoints 并入库
     *
     * @param recordingCode 录制编码
     * @param envCode       目标环境
     * @param projectCode   目标项目
     * @return 导入的 endpoint 数量
     */
    public Map<String, Object> playbackToMock(String recordingCode, String envCode, String projectCode) {
        MockRecording rec = recordingMapper.selectOne(
                new QueryWrapper<MockRecording>().eq("recording_code", recordingCode));
        if (rec == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Recording not found");
            return err;
        }

        List<MockRecordingRequest> requests = requestMapper.selectByRecordingCode(recordingCode);

        // 去重 - 相同的 (method, url) 只保留最后一条
        Map<String, MockRecordingRequest> dedupe = new LinkedHashMap<>();
        for (MockRecordingRequest r : requests) {
            String key = r.getRequestMethod() + " " + r.getRequestUrl();
            dedupe.put(key, r);
        }

        int imported = 0;
        for (MockRecordingRequest r : dedupe.values()) {
            String mockCode = "from-rec-" + recordingCode + "-" + Math.abs((r.getRequestMethod() + r.getRequestUrl()).hashCode());
            MockEndpoint existing = endpointMapper.selectOne(
                    new QueryWrapper<MockEndpoint>().eq("mock_code", mockCode));
            if (existing != null) {
                continue;
            }

            MockEndpoint ep = new MockEndpoint();
            ep.setMockCode(mockCode);
            ep.setMockName("Recording-" + recordingCode + ": " + r.getRequestMethod() + " " + r.getRequestUrl());
            ep.setEnvCode(envCode == null ? "default" : envCode);
            ep.setProjectCode(projectCode == null ? "default" : projectCode);
            ep.setPath(r.getRequestUrl());
            ep.setMethod(r.getRequestMethod());
            ep.setMatchUrlPattern(r.getRequestUrl());
            ep.setResponseTemplate(r.getResponseBody() == null || r.getResponseBody().isEmpty()
                    ? "{}" : r.getResponseBody());
            ep.setResponseHeaders(r.getResponseHeaders());
            ep.setStatusCode(r.getResponseStatus() == null ? 200 : r.getResponseStatus());
            ep.setPriority(5);
            ep.setStatus(1);
            ep.setSourceRecording(recordingCode);
            ep.setHitCount(0L);
            endpointMapper.insert(ep);
            imported++;
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("imported", imported);
        resp.put("totalUnique", dedupe.size());
        resp.put("recordingCode", recordingCode);
        return resp;
    }

    /**
     * 回放对比 - 把录制数据回放到目标服务，对比响应是否一致
     * 用于环境迁移验证、回归测试
     */
    public Map<String, Object> playbackCompare(String recordingCode, String targetBaseUrl) {
        MockRecording rec = recordingMapper.selectOne(
                new QueryWrapper<MockRecording>().eq("recording_code", recordingCode));
        if (rec == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Recording not found");
            return err;
        }
        List<MockRecordingRequest> requests = requestMapper.selectByRecordingCode(recordingCode);
        int total = 0, match = 0, diff = 0;
        List<Map<String, Object>> diffs = new ArrayList<>();

        for (MockRecordingRequest req : requests) {
            total++;
            try {
                // 委托给 DynamicApiExecutor
                ApiExecutionResult r = executor.executeByMethodUrl(
                        req.getRequestMethod(),
                        targetBaseUrl + req.getRequestUrl(),
                        req.getRequestHeaders() == null ? null
                                : JsonUtil.fromJson(req.getRequestHeaders(), new TypeReference<Map<String, String>>() {
                        }),
                        req.getRequestBody());

                int expected = req.getResponseStatus() == null ? 200 : req.getResponseStatus();
                boolean ok = (r.getStatus() == expected) && bodyEquals(r.getBody(), req.getResponseBody());
                if (ok) {
                    match++;
                } else {
                    diff++;
                    Map<String, Object> d = new HashMap<>();
                    d.put("url", req.getRequestUrl());
                    d.put("method", req.getRequestMethod());
                    d.put("expectedStatus", expected);
                    d.put("actualStatus", r.getStatus());
                    d.put("expectedBodyLength", req.getResponseBody() == null ? 0 : req.getResponseBody().length());
                    d.put("actualBodyLength", r.getBodySize());
                    diffs.add(d);
                }
            } catch (Exception e) {
                diff++;
                Map<String, Object> d = new HashMap<>();
                d.put("url", req.getRequestUrl());
                d.put("error", e.getMessage());
                diffs.add(d);
            }
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("total", total);
        resp.put("match", match);
        resp.put("diff", diff);
        resp.put("diffDetails", diffs);
        return resp;
    }

    private boolean bodyEquals(String a, String b) {
        if (a == null) {
            return b == null || b.isEmpty();
        }

        if (b == null) {
            return a.isEmpty();
        }

        // 简单对比（去除空白差异）
        return a.replaceAll("\\s+", "").equals(b.replaceAll("\\s+", ""));
    }
}
