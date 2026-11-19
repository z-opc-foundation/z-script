package com.zifang.z.script.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.meta.api.annotation.BizMethod;
import com.zifang.z.script.core.domain.entity.MockEndpoint;
import com.zifang.z.script.core.domain.mapper.MockEndpointMapper;
import com.zifang.z.script.core.domain.service.MockEndpointService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockEndpointServiceImpl extends ServiceImpl<MockEndpointMapper, MockEndpoint> implements MockEndpointService {

    @Override
    @BizMethod(name = "getByMockCode", module = "z-script", description = "", operation = BizMethod.Operation.QUERY)
    public MockEndpoint getByMockCode(String mockCode) {
        return getOne(new LambdaQueryWrapper<MockEndpoint>()
                .eq(MockEndpoint::getMockCode, mockCode));
    }

    @Override
    @BizMethod(name = "getByPathAndMethod", module = "z-script", description = "", operation = BizMethod.Operation.QUERY)
    public MockEndpoint getByPathAndMethod(String path, String method) {
        return getOne(new LambdaQueryWrapper<MockEndpoint>()
                .eq(MockEndpoint::getPath, path)
                .eq(MockEndpoint::getMethod, method)
                .eq(MockEndpoint::getStatus, 1));
    }

    @Override
    @BizMethod(name = "listActive", module = "z-script", description = "", operation = BizMethod.Operation.QUERY)
    public List<MockEndpoint> listActive() {
        return list(new LambdaQueryWrapper<MockEndpoint>()
                .eq(MockEndpoint::getStatus, 1)
                .orderByDesc(MockEndpoint::getCreateTime));
    }

    @Override
    @BizMethod(name = "saveMockEndpoint", module = "z-script", description = "", operation = BizMethod.Operation.CREATE)
    public boolean saveMockEndpoint(MockEndpoint endpoint) {
        return save(endpoint);
    }

    @Override
    @BizMethod(name = "updateMockEndpoint", module = "z-script", description = "", operation = BizMethod.Operation.UPDATE)
    public boolean updateMockEndpoint(MockEndpoint endpoint) {
        return updateById(endpoint);
    }

    @Override
    @BizMethod(name = "deleteByMockCode", module = "z-script", description = "", operation = BizMethod.Operation.DELETE)
    public boolean deleteByMockCode(String mockCode) {
        MockEndpoint endpoint = getByMockCode(mockCode);
        if (endpoint == null) {
            return false;
        }

        return removeById(endpoint.getId());
    }

    @Override
    @BizMethod(name = "incrementHitCount", module = "z-script", description = "", operation = BizMethod.Operation.ACTION)
    public boolean incrementHitCount(String mockCode) {
        return update(new LambdaUpdateWrapper<MockEndpoint>()
                .eq(MockEndpoint::getMockCode, mockCode)
                .setSql("hit_count = hit_count + 1"));
    }
}



