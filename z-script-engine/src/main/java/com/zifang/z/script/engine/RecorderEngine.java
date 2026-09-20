package com.zifang.z.script.engine;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.json.JsonUtil;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 录制引擎 (Recorder) - 真实/Mock 流量录制器
 * <p>
 * 核心能力:
 * 1. startRecording() - 创建录制会话（IDLE → RECORDING）
 * 2. captureRequest()  - 捕获每一个请求/响应快照并入库
 * 3. stopRecording()   - 结束会话（RECORDING → STOPPED）
 * 4. exportAsMock()    - 将录制数据转换为 Mock endpoints（录制→Mock 双向桥）
 * <p>
 * 工作模式:
 * - 把 MockEnvironment.envType=REAL 时的代理流量记录下来
 * - 客户端发出 /api/mock/{env}/xxx，系统把请求转发到真实服务，同时落库
 * - 用户停止录制后，可以把整个录制一键转成 Mock endpoints 用于离线测试
 */
@Component
public class RecorderEngine {

    private static final Logger log = LogManager.getLogger(RecorderEngine.class);

    @Autowired
    private MockRecordingMapper recordingMapper;
    @Autowired
    private MockRecordingRequestMapper requestMapper;
    @Autowired
    private MockEndpointMapper endpointMapper;

    /**
     * 启动一次新的录制会话
     *
     * @param recordingCode 唯一编码
     * @param recordingName 名称
     * @param targetUrl     被代理的真实服务地址
     * @return 录制会话实体
     */
    public MockRecording startRecording(String recordingCode, String recordingName, String targetUrl) {
        MockRecording existing = recordingMapper.selectOne(
                new QueryWrapper<MockRecording>().eq("recording_code", recordingCode));
        if (existing != null) {
            log.warn("Recording already exists: {}", recordingCode);
            existing.setRecordStatus("RECORDING");
            existing.setStartTime(new Date());
            existing.setEndTime(null);
            recordingMapper.updateById(existing);
            // 清空旧请求
            requestMapper.deleteByRecordingCode(recordingCode);
            return existing;
        }

        MockRecording rec = new MockRecording();
        rec.setRecordingCode(recordingCode);
        rec.setRecordingName(recordingName);
        rec.setTargetUrl(targetUrl);
        rec.setRecordStatus("RECORDING");
        rec.setTotalRequests(0L);
        rec.setStartTime(new Date());
        rec.setTenantCode("default");
        rec.setDeleted(0);
        recordingMapper.insert(rec);
        log.info("Recording started: {} -> {}", recordingCode, targetUrl);
        return rec;
    }

    /**
     * 捕获一次请求/响应到当前录制会话
     *
     * @param recordingCode   录制编码
     * @param method          HTTP 方法
     * @param url             请求 URL
     * @param requestHeaders  请求头 (Map)
     * @param requestBody     请求体
     * @param responseStatus  响应状态码
     * @param responseHeaders 响应头 (Map)
     * @param responseBody    响应体
     * @param responseTime    响应耗时 (ms)
     * @param matchedEndpoint 匹配到的 Mock 编码 (用于回放时反查)
     */
    public void captureRequest(String recordingCode,
                               String method,
                               String url,
                               Map<String, String> requestHeaders,
                               String requestBody,
                               int responseStatus,
                               Map<String, String> responseHeaders,
                               String responseBody,
                               long responseTime,
                               String matchedEndpoint) {
        MockRecording rec = recordingMapper.selectOne(
                new QueryWrapper<MockRecording>().eq("recording_code", recordingCode));
        if (rec == null || !"RECORDING".equals(rec.getRecordStatus())) {
            return;
        }

        // 计算 sequence = 当前总数 + 1
        Long count = requestMapper.countByRecordingCode(recordingCode);
        int sequence = count == null ? 1 : count.intValue() + 1;

        MockRecordingRequest req = new MockRecordingRequest();
        req.setRecordingCode(recordingCode);
        req.setSequence(sequence);
        req.setRequestMethod(method);
        req.setRequestUrl(url);
        req.setRequestHeaders(requestHeaders == null ? null : JsonUtil.toJson(requestHeaders));
        req.setRequestBody(requestBody);
        req.setResponseStatus(responseStatus);
        req.setResponseHeaders(responseHeaders == null ? null : JsonUtil.toJson(responseHeaders));
        req.setResponseBody(responseBody);
        req.setResponseTime(responseTime);
        req.setMatchedEndpoint(matchedEndpoint);
        req.setCreateTime(new Date());
        requestMapper.insert(req);

        // 更新录制总数
        MockRecording update = new MockRecording();
        update.setId(rec.getId());
        update.setTotalRequests((long) sequence);
        recordingMapper.updateById(update);
    }

    /**
     * 结束录制
     */
    public MockRecording stopRecording(String recordingCode) {
        MockRecording rec = recordingMapper.selectOne(
                new QueryWrapper<MockRecording>().eq("recording_code", recordingCode));
        if (rec == null) {
            return null;
        }

        rec.setRecordStatus("STOPPED");
        rec.setEndTime(new Date());
        recordingMapper.updateById(rec);
        log.info("Recording stopped: {} ({} requests)", recordingCode, rec.getTotalRequests());
        return rec;
    }

    /**
     * 列出当前所有录制会话
     */
    public List<MockRecording> listRecordings() {
        return recordingMapper.selectList(
                new QueryWrapper<MockRecording>().orderByDesc("id"));
    }

    /**
     * 获取某个录制的所有请求详情
     */
    public List<MockRecordingRequest> getRecordingRequests(String recordingCode) {
        return requestMapper.selectByRecordingCode(recordingCode);
    }

    /**
     * 删除一个录制 (级联删除请求详情)
     */
    public boolean deleteRecording(String recordingCode) {
        MockRecording rec = recordingMapper.selectOne(
                new QueryWrapper<MockRecording>().eq("recording_code", recordingCode));
        if (rec == null) {
            return false;
        }

        requestMapper.deleteByRecordingCode(recordingCode);
        recordingMapper.deleteById(rec.getId());
        return true;
    }

    /**
     * 录制转换为 Mock endpoint 列表
     * - 解析每个请求，按 method+url 生成 MockEndpoint 草稿
     * - response_body 作为 response_template
     * - 用户可再编辑入库
     */
    public List<MockEndpoint> exportAsMockEndpoints(String recordingCode, String projectCode) {
        List<MockRecordingRequest> requests = requestMapper.selectByRecordingCode(recordingCode);
        java.util.List<MockEndpoint> endpoints = new java.util.ArrayList<>();

        // 去重 - 相同的 (method, url) 只保留最后一条
        java.util.Map<String, MockEndpoint> dedupe = new java.util.LinkedHashMap<>();
        AtomicInteger idx = new AtomicInteger(0);
        for (MockRecordingRequest r : requests) {
            String key = r.getRequestMethod() + " " + r.getRequestUrl();
            MockEndpoint ep = new MockEndpoint();
            ep.setMockCode("recording-" + recordingCode + "-" + idx.incrementAndGet());
            ep.setMockName("Recorded: " + key);
            ep.setPath(r.getRequestUrl());
            ep.setMethod(r.getRequestMethod());
            ep.setMatchUrlPattern(r.getRequestUrl());
            ep.setResponseTemplate(r.getResponseBody() == null || r.getResponseBody().isEmpty()
                    ? "{}" : r.getResponseBody());
            ep.setResponseHeaders(r.getResponseHeaders());
            ep.setStatusCode(r.getResponseStatus() == null ? 200 : r.getResponseStatus());
            ep.setProjectCode(projectCode == null ? "default" : projectCode);
            ep.setEnvCode("default");
            ep.setPriority(5);
            ep.setStatus(1);
            ep.setHitCount(0L);
            ep.setSourceRecording(recordingCode);
            dedupe.put(key, ep);
        }
        endpoints.addAll(dedupe.values());
        return endpoints;
    }
}
