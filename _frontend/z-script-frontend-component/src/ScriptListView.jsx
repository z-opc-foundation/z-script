import React from 'react';
import { Table, Tag, Button, Space, Empty } from 'antd';

// 规范 §5：组件层只接 props，不自己 fetch —— 数据源由父项目决定（admin / 其它实例 / mock）。
const DSL_COLOR = { GROOVY: 'purple', JS: 'blue', LUA: 'red', EL: 'green', SQL: 'orange' };

export default function ScriptListView({ scripts = [], loading = false, onRun, onPublish, title = '脚本列表' }) {
    const columns = [
        { title: '脚本编码', dataIndex: 'scriptCode', key: 'scriptCode', width: 180 },
        { title: '名称', dataIndex: 'scriptName', key: 'scriptName' },
        {
            title: 'DSL',
            dataIndex: 'dslType',
            key: 'dslType',
            width: 90,
            render: (v) => <Tag color={DSL_COLOR[v] || 'default'}>{v}</Tag>,
        },
        {
            title: '暴露方式',
            dataIndex: 'exposeAs',
            key: 'exposeAs',
            width: 140,
            // httpPath 在取消发布后仍会留在库里（MyBatis-Plus updateById 不写 null），
            // 所以只有真的按 HTTP/BOTH 暴露时才跟着显示，避免出现「NONE /demo/hello」。
            render: (v, r) =>
                v ? `${v}${r.httpPath && /HTTP|BOTH/.test(v) ? ` ${r.httpPath}` : ''}` : '-',
        },
        { title: '版本', dataIndex: 'version', key: 'version', width: 90 },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 90,
            render: (v) => (Number(v) === 1 ? <Tag color="success">已上线</Tag> : <Tag>草稿</Tag>),
        },
        {
            title: '操作',
            key: 'actions',
            width: 160,
            render: (_, row) => (
                <Space size="small">
                    {onRun && (
                        <Button size="small" type="link" onClick={() => onRun(row)}>
                            执行
                        </Button>
                    )}
                    {onPublish && (
                        <Button size="small" type="link" onClick={() => onPublish(row)}>
                            {Number(row.status) === 1 ? '下线' : '发布'}
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div data-component="z-script-script-list">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}
            <Table
                rowKey={(r) => r.scriptCode || r.id}
                columns={columns}
                dataSource={scripts}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: false }}
                locale={{ emptyText: <Empty description="暂无脚本" /> }}
            />
        </div>
    );
}
