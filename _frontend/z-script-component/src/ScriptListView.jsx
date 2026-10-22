import React from 'react';
import { Tag, Button, Space } from 'antd';
import { ListTable } from '@yuku123/z-frontend-common/ui';

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
            {/* 列表页走基座 ListTable（lead 008 §2 规则 2/3/4）：分页 / 过滤 / EmptyState 三件套。
                分页用基座默认的 DEFAULT_PAGINATION（原来手写的 {pageSize:10, showSizeChanger:false}
                缺 pageSizeOptions / showTotal），过滤按脚本编码 / 名称端侧过滤。 */}
            <ListTable
                cardTitle={title || undefined}
                rows={scripts}
                columns={columns}
                rowKey={(r) => r.scriptCode || r.id}
                loading={loading}
                searchKeys="scriptCode,scriptName"
                searchPlaceholder="按脚本编码 / 名称过滤"
                emptyTitle="暂无脚本"
                emptyDescription="脚本发布后会出现在这里"
            />
        </div>
    );
}
