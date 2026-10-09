import React from 'react';
import { Table, Tag, Switch, Empty } from 'antd';

// Mock 端点列表：path + method + 匹配规则 + 响应模板摘要。
// 规范 §5：接 props 不 fetch。
const METHOD_COLOR = { GET: 'blue', POST: 'green', PUT: 'orange', DELETE: 'red', PATCH: 'purple' };

export default function MockEndpointListView({ endpoints = [], loading = false, onToggle, title = 'Mock 端点' }) {
    const columns = [
        { title: '编码', dataIndex: 'mockCode', key: 'mockCode', width: 160 },
        { title: '名称', dataIndex: 'mockName', key: 'mockName' },
        {
            title: 'Method',
            dataIndex: 'method',
            key: 'method',
            width: 100,
            render: (v) => <Tag color={METHOD_COLOR[v] || 'default'}>{v}</Tag>,
        },
        { title: 'Path', dataIndex: 'path', key: 'path', ellipsis: true },
        { title: '环境', dataIndex: 'envCode', key: 'envCode', width: 120 },
        { title: '状态码', dataIndex: 'statusCode', key: 'statusCode', width: 90 },
        { title: '延迟(ms)', dataIndex: 'delayMs', key: 'delayMs', width: 100 },
        {
            title: '启用',
            key: 'toggle',
            width: 80,
            render: (_, row) => (
                <Switch
                    size="small"
                    checked={Number(row.status ?? row.enabled ?? 1) === 1}
                    onChange={(checked) => onToggle && onToggle(row, checked)}
                />
            ),
        },
    ];

    return (
        <div data-component="z-script-mock-endpoint-list">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}
            <Table
                rowKey={(r) => r.mockCode || r.id}
                columns={columns}
                dataSource={endpoints}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: false }}
                locale={{ emptyText: <Empty description="暂无 Mock 端点" /> }}
            />
        </div>
    );
}
