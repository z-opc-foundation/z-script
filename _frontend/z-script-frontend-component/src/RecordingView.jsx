import React, { useState, useEffect, useMemo } from 'react';
import { Table, Tag, Button, Space, Empty, Modal, Input, Form, message, Card, Tabs } from 'antd';

// Mock 录制与回放面板
const STATUS_LABEL = {
    STARTED: '录制中',
    STOPPED: '已停止',
    EXPORTED: '已导出',
    IMPORTED: '已导入',
};

export default function RecordingView({ request, title = '录制 / 回放' }) {
    const [recordings, setRecordings] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedRecording, setSelectedRecording] = useState(null);
    const [requests, setRequests] = useState([]);
    const [requestsLoading, setRequestsLoading] = useState(false);

    const [startOpen, setStartOpen] = useState(false);
    const [startForm] = Form.useForm();
    const [playbackForm] = Form.useForm();
    const [compareForm] = Form.useForm();

    useEffect(() => {
        loadList();
    }, []);

    const loadList = async () => {
        setLoading(true);
        try {
            const res = await request('/api/mock-platform/recordings/list');
            const body = await res.json();
            setRecordings(Array.isArray(body?.data) ? body.data : []);
        } catch (e) {
            message.error('加载录制列表失败: ' + e.message);
        } finally {
            setLoading(false);
        }
    };

    const loadRequests = async (recordingCode) => {
        if (!recordingCode) {
            setRequests([]);
            return;
        }
        setRequestsLoading(true);
        try {
            const res = await request(`/api/mock-platform/recordings/requests?recordingCode=${encodeURIComponent(recordingCode)}`);
            const body = await res.json();
            setRequests(Array.isArray(body?.data) ? body.data : []);
        } catch (e) {
            message.error('加载请求列表失败: ' + e.message);
        } finally {
            setRequestsLoading(false);
        }
    };

    const handleStart = async (values) => {
        try {
            const res = await request('/api/mock-platform/recordings/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });
            const body = await res.json();
            if (body?.success) {
                message.success('开启录制成功');
                setStartOpen(false);
                startForm.resetFields();
                loadList();
            } else {
                message.error(body?.message || '开启录制失败');
            }
        } catch (e) {
            message.error('开启录制失败: ' + e.message);
        }
    };

    const handleStop = async (recordingCode) => {
        try {
            const res = await request(`/api/mock-platform/recordings/stop?recordingCode=${encodeURIComponent(recordingCode)}`, {
                method: 'POST',
            });
            const body = await res.json();
            if (body?.success) {
                message.success('停止录制成功');
                loadList();
                if (selectedRecording === recordingCode) {
                    loadRequests(recordingCode);
                }
            } else {
                message.error(body?.message || '停止录制失败');
            }
        } catch (e) {
            message.error('停止录制失败: ' + e.message);
        }
    };

    const handleDelete = async (recordingCode) => {
        try {
            const res = await request(`/api/mock-platform/recordings?recordingCode=${encodeURIComponent(recordingCode)}`, {
                method: 'DELETE',
            });
            const body = await res.json();
            if (body?.success) {
                message.success('删除成功');
                loadList();
                if (selectedRecording === recordingCode) {
                    setSelectedRecording(null);
                    setRequests([]);
                }
            } else {
                message.error(body?.message || '删除失败');
            }
        } catch (e) {
            message.error('删除失败: ' + e.message);
        }
    };

    const handlePlayback = async (values) => {
        if (!selectedRecording) return;
        try {
            const res = await request(`/api/mock-platform/recordings/playback?recordingCode=${encodeURIComponent(selectedRecording)}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });
            const body = await res.json();
            Modal.success({
                title: '回放结果',
                content: (
                    <div style={{ maxHeight: 400, overflow: 'auto' }}>
                        <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>
                            {JSON.stringify(body, null, 2)}
                        </pre>
                    </div>
                ),
                width: 800,
            });
        } catch (e) {
            message.error('回放失败: ' + e.message);
        }
    };

    const handleCompare = async (values) => {
        if (!selectedRecording) return;
        try {
            const res = await request(`/api/mock-platform/recordings/compare?recordingCode=${encodeURIComponent(selectedRecording)}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });
            const body = await res.json();
            Modal.success({
                title: '对比结果',
                content: (
                    <div style={{ maxHeight: 400, overflow: 'auto' }}>
                        <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>
                            {JSON.stringify(body, null, 2)}
                        </pre>
                    </div>
                ),
                width: 800,
            });
        } catch (e) {
            message.error('对比失败: ' + e.message);
        }
    };

    const handleSelectRecording = async (recordingCode) => {
        setSelectedRecording(recordingCode);
        loadRequests(recordingCode);
    };

    const columns = [
        {
            title: '录制编码',
            dataIndex: 'recordingCode',
            key: 'recordingCode',
            width: 180,
            render: (v) => <Tag>{v}</Tag>,
        },
        {
            title: '名称',
            dataIndex: 'recordingName',
            key: 'recordingName',
            ellipsis: true,
        },
        {
            title: '状态',
            dataIndex: 'recordStatus',
            key: 'recordStatus',
            width: 100,
            render: (v) => <Tag color={v === 'STARTED' ? 'processing' : 'default'}>{STATUS_LABEL[v] || v}</Tag>,
        },
        {
            title: '目标地址',
            dataIndex: 'targetUrl',
            key: 'targetUrl',
            ellipsis: true,
            width: 200,
        },
        {
            title: '总请求数',
            dataIndex: 'totalRequests',
            key: 'totalRequests',
            width: 100,
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
            width: 200,
            render: (_, row) => (
                <Space size="small" wrap>
                    <Button size="small" type="link" onClick={() => handleSelectRecording(row.recordingCode)}>
                        查看请求
                    </Button>
                    {row.recordStatus === 'STARTED' ? (
                        <Button size="small" danger onClick={() => handleStop(row.recordingCode)}>
                            停止
                        </Button>
                    ) : (
                        <Button size="small" disabled>
                            停止
                        </Button>
                    )}
                    <Button size="small" danger onClick={() => handleDelete(row.recordingCode)}>
                        删除
                    </Button>
                </Space>
            ),
        },
    ];

    const requestColumns = [
        {
            title: '序号',
            dataIndex: 'id',
            key: 'id',
            width: 60,
        },
        {
            title: '方法',
            dataIndex: 'httpMethod',
            key: 'httpMethod',
            width: 80,
            render: (v) => <Tag color={v === 'POST' ? 'error' : 'default'}>{v}</Tag>,
        },
        {
            title: '路径',
            dataIndex: 'httpPath',
            key: 'httpPath',
            ellipsis: true,
        },
        {
            title: '响应状态',
            dataIndex: 'responseStatus',
            key: 'responseStatus',
            width: 100,
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
    ];

    return (
        <div data-component="z-script-recording">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}

            <Space style={{ marginBottom: 16 }}>
                <Button type="primary" onClick={() => setStartOpen(true)}>
                    开启录制
                </Button>
            </Space>

            <Table
                rowKey={(r) => r.recordingCode}
                columns={columns}
                dataSource={recordings}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: false }}
                locale={{ emptyText: <Empty description="暂无录制会话（先开启一个录制）" /> }}
            />

            {selectedRecording && (
                <Card
                    title={`录制 "${selectedRecording}" 的请求列表`}
                    style={{ marginTop: 16 }}
                    extra={
                        <Space>
                            <Button
                                type="primary"
                                onClick={() => {
                                    Modal.confirm({
                                        title: '确认删除录制',
                                        content: `确定删除录制 ${selectedRecording} 及其所有请求吗？`,
                                        onOk: () => handleDelete(selectedRecording),
                                    });
                                }}
                            >
                                删除录制
                            </Button>
                        </Space>
                    }
                >
                    <Tabs
                        defaultActiveKey="requests"
                        items={[
                            {
                                key: 'requests',
                                label: '请求列表',
                                children: (
                                    <Table
                                        rowKey={(r) => r.id}
                                        columns={requestColumns}
                                        dataSource={requests}
                                        loading={requestsLoading}
                                        pagination={{ pageSize: 15, showSizeChanger: false }}
                                        locale={{ emptyText: <Empty description="该录制暂无请求" /> }}
                                    />
                                ),
                            },
                            {
                                key: 'playback',
                                label: '回放',
                                children: (
                                    <Form form={playbackForm} layout="vertical" onFinish={handlePlayback}>
                                        <Form.Item
                                            name="targetUrl"
                                            label="目标服务地址"
                                            rules={[{ required: true, message: '必填，例如 http://localhost:8080' }]}
                                        >
                                            <Input placeholder="http://target-service" />
                                        </Form.Item>
                                        <Form.Item>
                                            <Button type="primary" htmlType="submit">
                                                开始回放
                                            </Button>
                                        </Form.Item>
                                    </Form>
                                ),
                            },
                            {
                                key: 'compare',
                                label: '对比',
                                children: (
                                    <Form form={compareForm} layout="vertical" onFinish={handleCompare}>
                                        <Form.Item
                                            name="targetUrl"
                                            label="基线服务地址"
                                            rules={[{ required: true, message: '必填' }]}
                                        >
                                            <Input placeholder="http://baseline-service" />
                                        </Form.Item>
                                        <Form.Item>
                                            <Button type="primary" htmlType="submit">
                                                开始对比
                                            </Button>
                                        </Form.Item>
                                    </Form>
                                ),
                            },
                        ]}
                    />
                </Card>
            )}

            <Modal
                title="开启录制"
                open={startOpen}
                onOk={() => {
                    startForm.submit();
                }}
                onCancel={() => setStartOpen(false)}
                okText="开启"
                cancelText="取消"
                width={500}
            >
                <Form form={startForm} layout="vertical" onFinish={handleStart}>
                    <Form.Item
                        name="recordingCode"
                        label="录制编码"
                        rules={[{ required: true, message: '必填，仅允许字母数字下划线' }]}
                    >
                        <Input placeholder="例如 recording_001" />
                    </Form.Item>
                    <Form.Item
                        name="recordingName"
                        label="录制名称"
                        rules={[{ required: true, message: '必填' }]}
                    >
                        <Input placeholder="例如 用户登录流程录制" />
                    </Form.Item>
                    <Form.Item
                        name="targetUrl"
                        label="目标服务地址"
                        rules={[{ required: true, message: '必填' }]}
                    >
                        <Input placeholder="http://target-service/api" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
}
