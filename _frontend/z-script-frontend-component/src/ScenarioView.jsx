import React, { useState, useEffect } from 'react';
import {
    Table, Tag, Button, Space, Empty, Modal, Input, Form, InputNumber, Tabs, Card, Descriptions, Alert, message,
} from 'antd';

// Mock 场景状态机面板
const STATUS_LABEL = {
    1: '启用',
    0: '禁用',
};

export default function ScenarioView({ request, title = '场景 / 状态机' }) {
    const [scenarios, setScenarios] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedScenario, setSelectedScenario] = useState(null);
    const [instances, setInstances] = useState([]);
    const [instancesLoading, setInstancesLoading] = useState(false);
    const [cases, setCases] = useState([]);
    const [casesLoading, setCasesLoading] = useState(false);

    const [scenarioForm] = Form.useForm();

    useEffect(() => {
        loadList();
    }, []);

    const loadList = async () => {
        setLoading(true);
        try {
            const res = await request('/api/mock-platform/scenarios/list');
            const body = await res.json();
            setScenarios(Array.isArray(body?.data) ? body.data : []);
        } catch (e) {
            message.error('加载场景列表失败: ' + e.message);
        } finally {
            setLoading(false);
        }
    };

    const loadInstances = async (scenarioCode) => {
        if (!scenarioCode) {
            setInstances([]);
            return;
        }
        setInstancesLoading(true);
        try {
            const res = await request(`/api/mock-platform/scenarios/instances?scenarioCode=${encodeURIComponent(scenarioCode)}`);
            const body = await res.json();
            setInstances(Array.isArray(body?.data) ? body.data : []);
        } catch (e) {
            message.error('加载实例列表失败: ' + e.message);
        } finally {
            setInstancesLoading(false);
        }
    };

    const loadCases = async (scenarioCode) => {
        if (!scenarioCode) {
            setCases([]);
            return;
        }
        setCasesLoading(true);
        try {
            const res = await request(`/api/mock-platform/cases/list?caseGroup=${encodeURIComponent(scenarioCode)}`);
            const body = await res.json();
            setCases(Array.isArray(body?.data) ? body.data : []);
        } catch (e) {
            message.error('加载用例列表失败: ' + e.message);
        } finally {
            setCasesLoading(false);
        }
    };

    const handleCreate = async (values) => {
        try {
            const res = await request('/api/mock-platform/scenarios', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });
            const body = await res.json();
            if (body?.success) {
                message.success('创建场景成功');
                scenarioForm.resetFields();
                Modal.destroyAll();
                loadList();
            } else {
                message.error(body?.message || '创建失败');
            }
        } catch (e) {
            message.error('创建失败: ' + e.message);
        }
    };

    const handleUpdate = async (scenarioCode, values) => {
        try {
            const res = await request(`/api/mock-platform/scenarios?scenarioCode=${encodeURIComponent(scenarioCode)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });
            const body = await res.json();
            if (body?.success) {
                message.success('更新场景成功');
                Modal.destroyAll();
                loadList();
            } else {
                message.error(body?.message || '更新失败');
            }
        } catch (e) {
            message.error('更新失败: ' + e.message);
        }
    };

    const handleDelete = async (scenarioCode) => {
        try {
            const res = await request(`/api/mock-platform/scenarios?scenarioCode=${encodeURIComponent(scenarioCode)}`, {
                method: 'DELETE',
            });
            const body = await res.json();
            if (body?.success) {
                message.success('删除场景成功');
                loadList();
                if (selectedScenario === scenarioCode) {
                    setSelectedScenario(null);
                    setInstances([]);
                    setCases([]);
                }
            } else {
                message.error(body?.message || '删除失败');
            }
        } catch (e) {
            message.error('删除失败: ' + e.message);
        }
    };

    const handleReset = async (scenarioCode) => {
        try {
            const res = await request(`/api/mock-platform/scenarios/reset?scenarioCode=${encodeURIComponent(scenarioCode)}`, {
                method: 'POST',
            });
            const body = await res.json();
            if (body?.success) {
                message.success('重置场景状态成功');
                if (selectedScenario === scenarioCode) {
                    loadInstances(scenarioCode);
                }
            } else {
                message.error(body?.message || '重置失败');
            }
        } catch (e) {
            message.error('重置失败: ' + e.message);
        }
    };

    const handleSelectScenario = async (scenarioCode) => {
        setSelectedScenario(scenarioCode);
        loadInstances(scenarioCode);
        loadCases(scenarioCode);
    };

    const scenarioColumns = [
        {
            title: '场景编码',
            dataIndex: 'scenarioCode',
            key: 'scenarioCode',
            width: 180,
            render: (v) => <Tag>{v}</Tag>,
        },
        {
            title: '名称',
            dataIndex: 'scenarioName',
            key: 'scenarioName',
            ellipsis: true,
        },
        {
            title: '初始状态',
            dataIndex: 'initialState',
            key: 'initialState',
            width: 100,
            render: (v) => <Tag color="processing">{v}</Tag>,
        },
        {
            title: '描述',
            dataIndex: 'description',
            key: 'description',
            ellipsis: true,
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 160,
            render: (v) => {
                if (!v) return '-';
                const d = new Date(v);
                const pad = (n) => String(n).padStart(2, '0');
                return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
            },
        },
        {
            title: '操作',
            key: 'actions',
            width: 240,
            render: (_, row) => (
                <Space size="small" wrap>
                    <Button size="small" type="link" onClick={() => handleSelectScenario(row.scenarioCode)}>
                        查看状态
                    </Button>
                    <Button
                        size="small"
                        type="link"
                        onClick={() => {
                            Modal.confirm({
                                title: '确认删除场景',
                                content: `确定删除场景 ${row.scenarioCode} 及其所有关联数据吗？`,
                                onOk: () => handleDelete(row.scenarioCode),
                            });
                        }}
                    >
                        删除
                    </Button>
                </Space>
            ),
        },
    ];

    const instanceColumns = [
        {
            title: '实例 Key',
            dataIndex: 'instanceKey',
            key: 'instanceKey',
            width: 150,
            render: (v) => <Tag>{v}</Tag>,
        },
        {
            title: '当前状态',
            dataIndex: 'currentState',
            key: 'currentState',
            width: 120,
            render: (v) => <Tag color="processing">{v}</Tag>,
        },
        {
            title: '触发次数',
            dataIndex: 'triggerCount',
            key: 'triggerCount',
            width: 100,
        },
        {
            title: '最后触发',
            dataIndex: 'lastTriggerTime',
            key: 'lastTriggerTime',
            width: 160,
            render: (v) => {
                if (!v) return '-';
                const d = new Date(v);
                const pad = (n) => String(n).padStart(2, '0');
                return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
            },
        },
    ];

    const caseColumns = [
        {
            title: '用例编码',
            dataIndex: 'caseCode',
            key: 'caseCode',
            width: 150,
            render: (v) => <Tag>{v}</Tag>,
        },
        {
            title: '分组',
            dataIndex: 'caseGroup',
            key: 'caseGroup',
            width: 120,
        },
        {
            title: '名称',
            dataIndex: 'testCaseName',
            key: 'testCaseName',
            ellipsis: true,
        },
        {
            title: '环境',
            dataIndex: 'envCode',
            key: 'envCode',
            width: 100,
        },
        {
            title: '优先级',
            dataIndex: 'priority',
            key: 'priority',
            width: 80,
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 80,
            render: (v) => <Tag color={v === 1 ? 'success' : 'default'}>{STATUS_LABEL[v] || v}</Tag>,
        },
    ];

    return (
        <div data-component="z-script-scenario">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}

            <Space style={{ marginBottom: 16 }}>
                <Button
                    type="primary"
                    onClick={() => {
                        Modal.destroyAll();
                        Modal.open({
                            title: '创建场景',
                            width: 500,
                            footer: null,
                            content: (
                                <Form form={scenarioForm} layout="vertical" onFinish={handleCreate}>
                                    <Form.Item
                                        name="scenarioCode"
                                        label="场景编码"
                                        rules={[{ required: true, message: '必填，仅允许字母数字下划线' }]}
                                    >
                                        <Input placeholder="例如 scenario_login" />
                                    </Form.Item>
                                    <Form.Item
                                        name="scenarioName"
                                        label="场景名称"
                                        rules={[{ required: true, message: '必填' }]}
                                    >
                                        <Input placeholder="例如 用户登录场景" />
                                    </Form.Item>
                                    <Form.Item
                                        name="initialState"
                                        label="初始状态"
                                        initialValue="started"
                                        rules={[{ required: true }]}
                                    >
                                        <Input disabled />
                                    </Form.Item>
                                    <Form.Item name="description" label="描述">
                                        <Input.TextArea rows={3} placeholder="场景描述（可选）" />
                                    </Form.Item>
                                    <Form.Item>
                                        <Button type="primary" htmlType="submit" block>
                                            创建
                                        </Button>
                                    </Form.Item>
                                </Form>
                            ),
                        });
                    }}
                >
                    创建场景
                </Button>
            </Space>

            <Table
                rowKey={(r) => r.scenarioCode}
                columns={scenarioColumns}
                dataSource={scenarios}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: false }}
                locale={{ emptyText: <Empty description="暂无场景（先创建一个场景）" /> }}
            />

            {selectedScenario && (
                <Card
                    title={`场景 "${selectedScenario}" 的状态与用例`}
                    style={{ marginTop: 16 }}
                    extra={
                        <Space>
                            <Button
                                type="primary"
                                danger
                                onClick={() => {
                                    Modal.confirm({
                                        title: '确认重置场景状态',
                                        content: `确定重置场景 ${selectedScenario} 的所有实例状态吗？`,
                                        onOk: () => handleReset(selectedScenario),
                                    });
                                }}
                            >
                                重置状态
                            </Button>
                            <Button
                                danger
                                onClick={() => {
                                    Modal.confirm({
                                        title: '确认删除场景',
                                        content: `确定删除场景 ${selectedScenario} 及其所有关联数据吗？`,
                                        onOk: () => handleDelete(selectedScenario),
                                    });
                                }}
                            >
                                删除场景
                            </Button>
                        </Space>
                    }
                >
                    {instances.length === 0 && (
                        <Alert
                            type="info"
                            showIcon
                            style={{ marginBottom: 16 }}
                            message="该场景暂无运行实例。调用场景 API 会自动创建实例。"
                        />
                    )}

                    <Tabs
                        defaultActiveKey="instances"
                        items={[
                            {
                                key: 'instances',
                                label: `实例列表 (${instances.length})`,
                                children: (
                                    <Table
                                        rowKey={(r) => r.instanceKey}
                                        columns={instanceColumns}
                                        dataSource={instances}
                                        loading={instancesLoading}
                                        pagination={{ pageSize: 10, showSizeChanger: false }}
                                        locale={{ emptyText: <Empty description="暂无实例" /> }}
                                    />
                                ),
                            },
                            {
                                key: 'cases',
                                label: `关联用例 (${cases.length})`,
                                children: (
                                    <Table
                                        rowKey={(r) => r.caseCode}
                                        columns={caseColumns}
                                        dataSource={cases}
                                        loading={casesLoading}
                                        pagination={{ pageSize: 10, showSizeChanger: false }}
                                        locale={{ emptyText: <Empty description="暂无关联用例" /> }}
                                    />
                                ),
                            },
                        ]}
                    />
                </Card>
            )}
        </div>
    );
}
