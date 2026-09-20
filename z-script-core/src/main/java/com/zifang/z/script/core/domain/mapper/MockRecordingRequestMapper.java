package com.zifang.z.script.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.script.core.domain.entity.MockRecordingRequest;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MockRecordingRequestMapper extends BaseMapper<MockRecordingRequest> {

    /**
     * 按录制编码查询所有录制请求，按 sequence 升序
     */
    @Select("SELECT * FROM z_mock_recording_request WHERE recording_code = #{recordingCode} ORDER BY sequence ASC")
    List<MockRecordingRequest> selectByRecordingCode(String recordingCode);

    /**
     * 统计指定录制的请求总数
     */
    @Select("SELECT COUNT(*) FROM z_mock_recording_request WHERE recording_code = #{recordingCode}")
    Long countByRecordingCode(String recordingCode);

    /**
     * 删除指定录制的所有请求
     */
    @Select("DELETE FROM z_mock_recording_request WHERE recording_code = #{recordingCode}")
    void deleteByRecordingCode(String recordingCode);
}
