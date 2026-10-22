import React from 'react';
import { Tag, Switch } from 'antd';
import { ListTable } from '@yuku123/z-frontend-common/ui';

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
            {/* 列表页走基座 ListTable（lead 008 §2 规则 2/3/4）：分页 / 过滤 / EmptyState 三件套。
                分页用基座默认的 DEFAULT_PAGINATION（原来手写的 {pageSize:10, showSizeChanger:false}
                缺 pageSizeOptions / showTotal），过滤按 Mock 编码 / 名称 / Path 端侧过滤。 */}
            <ListTable
                cardTitle={title || undefined}
                rows={endpoints}
                columns={columns}
                rowKey={(r) => r.mockCode || r.id}
                loading={loading}
                searchKeys="mockCode,mockName,path"
                searchPlaceholder="按 Mock 编码 / 名称 / Path 过滤"
                emptyTitle="暂无 Mock 端点"
                emptyDescription="录制回放后可一键导入为 Mock 端点"
            />
        </div>
    );
}
