package com.zifang.z.script.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.script.core.domain.entity.MockEndpoint;

import java.util.List;

public interface MockEndpointService extends IService<MockEndpoint> {

    MockEndpoint getByMockCode(String mockCode);

    MockEndpoint getByPathAndMethod(String path, String method);

    List<MockEndpoint> listActive();

    boolean saveMockEndpoint(MockEndpoint endpoint);

    boolean updateMockEndpoint(MockEndpoint endpoint);

    boolean deleteByMockCode(String mockCode);

    boolean incrementHitCount(String mockCode);
}
