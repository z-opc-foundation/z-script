import React from 'react';
import { Table, Tag, Button, Space, Empty } from 'antd';

// 应用列表：应用是权限模型的中心（AK 挂应用、脚本列表挂应用）。
// 规范 §5：接 props 不 fetch；scope/脚本列表的编辑动作全部由父层通过回调决定。
const parseScripts = (json) => {
    try {
        const v = JSON.parse(json || '[]');
        return Array.isArray(v) ? v : [];
    } catch (e) {
        return [];
    }
};

export default function AppListView({
    apps = [],
    loading = false,
    onEditScripts,
    onToggle,
    title = '应用',
}) {
    const columns = [
        { title: '应用编码', dataIndex: 'appCode', key: 'appCode', width: 160 },
        { title: '名称', dataIndex: 'appName', key: 'appName' },
        {
            title: '权限范围',
            dataIndex: 'scope',
            key: 'scope',
            width: 110,
            render: (v) =>
                v === 'ALL' ? <Tag color="green">ALL</Tag> : <Tag color="orange">{v || 'SPECIFIC'}</Tag>,
        },
        {
            title: '可访问脚本',
            key: 'scripts',
            ellipsis: true,
            render: (_, r) =>
                r.scope === 'ALL' ? '全部脚本' : parseScripts(r.allowedScripts).join(', ') || '（未配置）',
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 90,
            render: (v) => (Number(v) === 1 ? <Tag color="success">启用</Tag> : <Tag>禁用</Tag>),
        },
        {
            title: '操作',
            key: 'actions',
            width: 170,
            render: (_, row) => (
                <Space size="small">
                    {onEditScripts && (
                        <Button size="small" type="link" onClick={() => onEditScripts(row)}>
                            配置脚本
                        </Button>
                    )}
                    {onToggle && (
                        <Button size="small" type="link" onClick={() => onToggle(row, Number(row.status) !== 1)}>
                            {Number(row.status) === 1 ? '禁用' : '启用'}
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div data-component="z-script-app-list">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}
            <Table
                rowKey={(r) => r.appCode || r.id}
                columns={columns}
                dataSource={apps}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: false }}
                locale={{ emptyText: <Empty description="暂无应用（签发 AK 时会自动创建）" /> }}
            />
        </div>
    );
}
