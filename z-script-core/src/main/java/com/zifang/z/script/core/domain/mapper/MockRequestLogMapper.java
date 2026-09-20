package com.zifang.z.script.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.script.core.domain.entity.MockRequestLog;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface MockRequestLogMapper extends BaseMapper<MockRequestLog> {

    /**
     * 路径查询 - 最近的 N 条
     */
    @Select("SELECT * FROM z_mock_request_log WHERE request_path = #{path} ORDER BY id DESC LIMIT #{limit}")
    List<MockRequestLog> selectByPath(String path, Integer limit);

    /**
     * Mock 编码查询
     */
    @Select("SELECT * FROM z_mock_request_log WHERE mock_code = #{mockCode} ORDER BY id DESC LIMIT #{limit}")
    List<MockRequestLog> selectByMockCode(String mockCode, Integer limit);

    /**
     * 统计：未匹配请求数
     */
    @Select("SELECT COUNT(*) FROM z_mock_request_log WHERE matched = 0")
    Long countUnmatched();

    /**
     * 统计：响应时间分布（按 mockCode）
     */
    @Select("SELECT mock_code, COUNT(*) AS cnt, AVG(duration_ms) AS avg_ms, MAX(duration_ms) AS max_ms " +
            "FROM z_mock_request_log WHERE mock_code IS NOT NULL " +
            "GROUP BY mock_code ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> statsByMockCode(Integer limit);
}
