import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Table, Tag, Button, Space, Empty, Modal, Input, Form, message, Drawer, Popconfirm, Alert, Tooltip, Typography, Descriptions, Badge,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';

// Mock 场景状态机面板。
// request 契约与 SPA 一致：request('mock-platform/...')，路径相对 /api/。
// 数据形状对齐 MockPlatformController：scenarios/list、cases/list、endpoints/list 返回裸数组；
// scenarios/instances 返回 {instanceKey: currentState} 裸 map。

const fmtTime = (t) => {
    if (!t) return '-';
    const d = new Date(t);
    if (Number.isNaN(d.getTime())) return String(t);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const stateColor = (s) => {
    if (!s) return 'default';
    const lowered = String(s).toLowerCase();
    if (/(finish|done|complete|success|end)/.test(lowered)) return 'success';
    if (/(fail|error|cancel|reject)/.test(lowered)) return 'error';
    if (/(start|running|pending|processing)/.test(lowered)) return 'processing';
    return 'purple';
};

export default function ScenarioView({ request, title = '场景 / 状态机' }) {
    const [scenarios, setScenarios] = useState([]);
    const [loading, setLoading] = useState(false);

    // 受控的创建/编辑 Modal（此前用 Modal.open —— antd v5 没有这个 API，一点就崩）
    const [formOpen, setFormOpen] = useState(false);
    const [editing, setEditing] = useState(null); // null=新建，否则为被编辑的场景行
    const [form] = Form.useForm();

    // 详情抽屉
    const [active, setActive] = useState(null);
    const [instances, setInstances] = useState({});
    const [instancesLoading, setInstancesLoading] = useState(false);
    const [endpoints, setEndpoints] = useState([]);
    const [cases, setCases] = useState([]);

    const loadList = useCallback(async () => {
        setLoading(true);
        try {
            const res = await request('mock-platform/scenarios/list');
            const body = await res.json();
            setScenarios(Array.isArray(body) ? body : (Array.isArray(body?.data) ? body.data : []));
        } catch (e) {
            message.error(`加载场景列表失败: ${e.message || e}`);
        } finally {
            setLoading(false);
        }
    }, [request]);

    useEffect(() => {
        loadList();
    }, [loadList]);

    const loadDetail = useCallback(
        async (scenarioCode) => {
            setInstancesLoading(true);
            try {
                const [instRes, epRes, caseRes] = await Promise.all([
                    request(`mock-platform/scenarios/instances?scenarioCode=${encodeURIComponent(scenarioCode)}`),
                    request(`mock-platform/endpoints/list?scenarioCode=${encodeURIComponent(scenarioCode)}`),
                    request(`mock-platform/cases/list?caseGroup=${encodeURIComponent(scenarioCode)}`),
                ]);
                const inst = await instRes.json().catch(() => ({}));
                setInstances(inst && typeof inst === 'object' && !Array.isArray(inst) ? inst : {});
                const eps = await epRes.json().catch(() => []);
                setEndpoints(Array.isArray(eps) ? eps : []);
                const cs = await caseRes.json().catch(() => []);
                setCases(Array.isArray(cs) ? cs : []);
            } catch (e) {
                message.error(`加载场景详情失败: ${e.message || e}`);
            } finally {
                setInstancesLoading(false);
            }
        },
        [request]
    );

    const openDetail = (row) => {
        setActive(row);
        loadDetail(row.scenarioCode);
    };

    const openCreate = () => {
        setEditing(null);
        form.resetFields();
        form.setFieldsValue({ initialState: 'started' });
        setFormOpen(true);
    };

    const openEdit = (row) => {
        setEditing(row);
        form.setFieldsValue({
            scenarioCode: row.scenarioCode,
            scenarioName: row.scenarioName,
            initialState: row.initialState || 'started',
            description: row.description,
        });
        setFormOpen(true);
    };

    const submitForm = async (values) => {
        const isEdit = !!editing;
        try {
            const res = await request(
                isEdit ? `mock-platform/scenarios?scenarioCode=${encodeURIComponent(editing.scenarioCode)}` : 'mock-platform/scenarios',
                { method: isEdit ? 'PUT' : 'POST', body: JSON.stringify(values) }
            );
            const body = await res.json().catch(() => ({}));
            if (body?.success) {
                message.success(isEdit ? '场景已更新' : `场景 ${values.scenarioCode} 已创建`);
                setFormOpen(false);
                loadList();
            } else {
                message.error(body?.message || (isEdit ? '更新失败' : '创建失败'));
            }
        } catch (e) {
            message.error(`${isEdit ? '更新' : '创建'}失败: ${e.message || e}`);
        }
    };

    const doDelete = async (row) => {
        try {
            const res = await request(`mock-platform/scenarios?scenarioCode=${encodeURIComponent(row.scenarioCode)}`, {
                method: 'DELETE',
            });
            const body = await res.json().catch(() => ({}));
            if (body?.success) {
                message.success('已删除场景');
                if (active?.scenarioCode === row.scenarioCode) setActive(null);
                loadList();
            } else {
                message.error(body?.message || '删除失败');
            }
        } catch (e) {
            message.error(`删除失败: ${e.message || e}`);
        }
    };

    const doReset = async (instanceKey) => {
        const key = instanceKey ?? '';
        try {
            const res = await request(
                `mock-platform/scenarios/reset?scenarioCode=${encodeURIComponent(active.scenarioCode)}${key ? `&instanceKey=${encodeURIComponent(key)}` : ''}`,
                { method: 'POST' }
            );
            const body = await res.json().catch(() => ({}));
            if (body?.success) {
                message.success(key ? `实例 ${key} 已回到初始态` : '全部实例已重置');
                loadDetail(active.scenarioCode);
            } else {
                message.error(body?.message || '重置失败');
            }
        } catch (e) {
            message.error(`重置失败: ${e.message || e}`);
        }
    };

    const instanceRows = useMemo(
        () => Object.entries(instances).map(([k, v]) => ({ instanceKey: k, currentState: v })),
        [instances]
    );

    const columns = [
        {
            title: '场景编码',
            dataIndex: 'scenarioCode',
            key: 'scenarioCode',
            width: 200,
            render: (v) => (
                <Typography.Text copyable={{ text: v }} style={{ fontFamily: 'monospace' }}>
                    {v}
                </Typography.Text>
            ),
        },
        { title: '名称', dataIndex: 'scenarioName', key: 'scenarioName', ellipsis: true },
        {
            title: '初始状态',
            dataIndex: 'initialState',
            key: 'initialState',
            width: 120,
            render: (v) => <Tag color={stateColor(v)}>{v || '-'}</Tag>,
        },
        {
            title: '描述',
            dataIndex: 'description',
            key: 'description',
            ellipsis: true,
            render: (v) => v || <span style={{ color: '#bbb' }}>-</span>,
        },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, render: fmtTime },
        {
            title: '操作',
            key: 'actions',
            width: 220,
            render: (_, row) => (
                <Space size="small">
                    <Button size="small" type="link" onClick={() => openDetail(row)}>
                        状态机
                    </Button>
                    <Button size="small" type="link" onClick={() => openEdit(row)}>
                        编辑
                    </Button>
                    <Popconfirm title="删除场景？（不会删除关联端点）" onConfirm={() => doDelete(row)} okText="删除" cancelText="取消">
                        <Button size="small" type="link" danger>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    const instanceColumns = [
        { title: '实例 Key', dataIndex: 'instanceKey', key: 'instanceKey', render: (v) => <Tag>{v}</Tag> },
        {
            title: '当前状态',
            dataIndex: 'currentState',
            key: 'currentState',
            width: 140,
            render: (v) => <Tag color={stateColor(v)}>{v}</Tag>,
        },
        {
            title: '操作',
            key: 'ops',
            width: 90,
            render: (_, row) => (
                <Popconfirm title="重置该实例到初始状态？" onConfirm={() => doReset(row.instanceKey)} okText="重置" cancelText="取消">
                    <Button size="small" type="link">
                        重置
                    </Button>
                </Popconfirm>
            ),
        },
    ];

    const endpointColumns = [
        { title: 'Mock 编码', dataIndex: 'mockCode', key: 'mockCode', width: 180, ellipsis: true },
        {
            title: '方法',
            dataIndex: 'method',
            key: 'method',
            width: 80,
            render: (v) => <Tag color={v === 'GET' ? 'blue' : 'orange'}>{v}</Tag>,
        },
        { title: '路径', dataIndex: 'path', key: 'path', ellipsis: true },
        {
            title: '状态流转',
            key: 'transition',
            width: 160,
            render: (_, r) =>
                r.requiredState || r.newState ? (
                    <span style={{ fontSize: 12 }}>
                        <Tag color={stateColor(r.requiredState)}>{r.requiredState || '任意'}</Tag>→
                        <Tag color={stateColor(r.newState)}>{r.newState || '不变'}</Tag>
                    </span>
                ) : (
                    <span style={{ color: '#bbb' }}>-</span>
                ),
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 80,
            render: (v) => (Number(v) === 1 ? <Badge status="success" text="启用" /> : <Badge status="default" text="停用" />),
        },
        { title: '命中', dataIndex: 'hitCount', key: 'hitCount', width: 70, render: (v) => v ?? 0 },
    ];

    const caseColumns = [
        { title: '用例编码', dataIndex: 'caseCode', key: 'caseCode', width: 160, ellipsis: true },
        { title: '名称', dataIndex: 'caseName', key: 'caseName', ellipsis: true, render: (v) => v || '-' },
        { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 80,
            render: (v) => (Number(v) === 1 ? <Badge status="success" text="启用" /> : <Badge status="default" text="停用" />),
        },
        {
            title: '通过 / 执行',
            key: 'pass',
            width: 110,
            render: (_, r) => {
                const run = r.runCount ?? 0;
                const pass = r.passCount ?? 0;
                if (!run) return <span style={{ color: '#bbb' }}>未执行</span>;
                return (
                    <Tooltip title={`失败 ${r.failCount ?? 0} 次`}>
                        <span style={{ color: pass === run ? '#389e0d' : '#d46b08' }}>
                            {pass} / {run}
                        </span>
                    </Tooltip>
                );
            },
        },
    ];

    return (
        <div data-component="z-script-scenario">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}

            <Space style={{ marginBottom: 16 }}>
                <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                    新建场景
                </Button>
                <Button icon={<ReloadOutlined />} onClick={loadList} loading={loading}>
                    刷新
                </Button>
            </Space>

            <Table
                rowKey={(r) => r.scenarioCode}
                columns={columns}
                dataSource={scenarios}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: false, hideOnSinglePage: true }}
                locale={{ emptyText: <Empty description="暂无场景 — 点「新建场景」开始" /> }}
                onRow={(row) => ({ onClick: () => openDetail(row), style: { cursor: 'pointer' } })}
            />

            <Modal
                title={editing ? `编辑场景 — ${editing.scenarioCode}` : '新建场景'}
                open={formOpen}
                onOk={() => form.submit()}
                onCancel={() => setFormOpen(false)}
                okText={editing ? '保存' : '创建'}
                cancelText="取消"
                destroyOnClose
                width={520}
            >
                <Form form={form} layout="vertical" onFinish={submitForm}>
                    <Form.Item
                        name="scenarioCode"
                        label="场景编码"
                        rules={[
                            { required: true, message: '必填' },
                            { pattern: /^[A-Za-z0-9_-]+$/, message: '仅允许字母/数字/下划线/中划线' },
                        ]}
                        extra={editing ? '编码是定位键，编辑时不可改' : undefined}
                    >
                        <Input placeholder="例如 order_flow" disabled={!!editing} />
                    </Form.Item>
                    <Form.Item name="scenarioName" label="场景名称" rules={[{ required: true, message: '必填' }]}>
                        <Input placeholder="例如 下单流程" />
                    </Form.Item>
                    <Form.Item
                        name="initialState"
                        label="初始状态"
                        rules={[{ required: true, message: '必填' }]}
                        extra="重置实例时回到这个状态"
                    >
                        <Input placeholder="started" />
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} placeholder="这个状态机模拟什么（可选）" maxLength={200} showCount />
                    </Form.Item>
                </Form>
            </Modal>

            <Drawer
                title={active ? `场景状态机 — ${active.scenarioName || active.scenarioCode}` : ''}
                width={860}
                open={!!active}
                onClose={() => setActive(null)}
                destroyOnClose
                extra={
                    <Popconfirm title="重置全部实例到初始状态？" onConfirm={() => doReset(null)} okText="重置" cancelText="取消">
                        <Button danger>全部重置</Button>
                    </Popconfirm>
                }
            >
                {active && (
                    <>
                        <Descriptions size="small" column={2} style={{ marginBottom: 16 }} bordered>
                            <Descriptions.Item label="场景编码">{active.scenarioCode}</Descriptions.Item>
                            <Descriptions.Item label="初始状态">
                                <Tag color={stateColor(active.initialState)}>{active.initialState || '-'}</Tag>
                            </Descriptions.Item>
                            <Descriptions.Item label="描述" span={2}>
                                {active.description || '-'}
                            </Descriptions.Item>
                        </Descriptions>

                        <Alert
                            type="info"
                            showIcon
                            style={{ marginBottom: 16 }}
                            message="实例由调用方带 X-Scenario-Instance 等请求头驱动；没有实例时先去触发一次 Mock 调用。"
                        />

                        <Table
                            title={() => <b>运行实例 ({instanceRows.length})</b>}
                            rowKey={(r) => r.instanceKey}
                            size="small"
                            columns={instanceColumns}
                            dataSource={instanceRows}
                            loading={instancesLoading}
                            pagination={false}
                            locale={{ emptyText: <Empty description="暂无运行实例" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                        />

                        <Table
                            style={{ marginTop: 24 }}
                            title={() => (
                                <Space>
                                    <b>关联 Mock 端点 ({endpoints.length})</b>
                                    <Tooltip title="scenarioCode 关联的端点，状态机进入某状态后这些端点的响应会随之变化">
                                        <span style={{ color: '#999' }}>ⓘ</span>
                                    </Tooltip>
                                </Space>
                            )}
                            rowKey={(r) => r.mockCode || r.id}
                            size="small"
                            columns={endpointColumns}
                            dataSource={endpoints}
                            pagination={false}
                            locale={{ emptyText: <Empty description="暂无关联端点" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                        />

                        <Table
                            style={{ marginTop: 24 }}
                            title={() => <b>关联用例 ({cases.length})</b>}
                            rowKey={(r) => r.caseCode || r.id}
                            size="small"
                            columns={caseColumns}
                            dataSource={cases}
                            pagination={false}
                            locale={{ emptyText: <Empty description="暂无关联用例" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                        />
                    </>
                )}
            </Drawer>
        </div>
    );
}
