import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Table, Tag, Button, Space, Empty, Modal, Input, Form, message, Drawer, Tabs, Popconfirm, Select, Tooltip, Typography, Alert,
} from 'antd';
import { PlayCircleOutlined, ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons';

// Mock 录制与回放面板。
// request 契约与 SPA 一致：request('mock-platform/...')，路径相对 /api/，401 时抛错。
// 数据形状对齐 MockRecordingController：list/requests 返回裸数组；
// playback 返回 [{recordingCode,target,total,pass,fail,results}]；compare 返回 {success,total,match,diff,diffs}。

const fmtTime = (t) => {
    if (!t) return '-';
    const d = new Date(t);
    if (Number.isNaN(d.getTime())) return String(t);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const prettyJson = (s) => {
    if (!s) return '（空）';
    try {
        return JSON.stringify(JSON.parse(s), null, 2);
    } catch (e) {
        return String(s);
    }
};

const StatusTag = ({ status }) =>
    status === 'RECORDING' ? (
        <Tag color="processing" icon={<span style={{ display: 'inline-block', width: 6, height: 6, borderRadius: '50%', background: '#1677ff', marginRight: 4 }} />}>
            录制中
        </Tag>
    ) : status === 'STOPPED' ? (
        <Tag>已停止</Tag>
    ) : (
        <Tag color="default">{status || '未知'}</Tag>
    );

export default function RecordingView({ request, title = '录制 / 回放' }) {
    const [recordings, setRecordings] = useState([]);
    const [loading, setLoading] = useState(false);
    const [statusFilter, setStatusFilter] = useState(null);

    // 详情抽屉
    const [active, setActive] = useState(null); // 选中的 MockRecording 行
    const [requests, setRequests] = useState([]);
    const [requestsLoading, setRequestsLoading] = useState(false);

    // 开启录制 modal
    const [startOpen, setStartOpen] = useState(false);
    const [startForm] = Form.useForm();

    // 回放 / 对比
    const [targetUrl, setTargetUrl] = useState('');
    const [playResult, setPlayResult] = useState(null);
    const [playLoading, setPlayLoading] = useState(false);
    const [cmpResult, setCmpResult] = useState(null);
    const [cmpLoading, setCmpLoading] = useState(false);
    const [importLoading, setImportLoading] = useState(false);

    const loadList = useCallback(async () => {
        setLoading(true);
        try {
            const res = await request('mock-platform/recordings/list');
            const body = await res.json();
            setRecordings(Array.isArray(body) ? body : (Array.isArray(body?.data) ? body.data : []));
        } catch (e) {
            message.error(`加载录制列表失败: ${e.message || e}`);
        } finally {
            setLoading(false);
        }
    }, [request]);

    useEffect(() => {
        loadList();
    }, [loadList]);

    const loadRequests = useCallback(async (recordingCode) => {
        setRequestsLoading(true);
        try {
            const res = await request(`mock-platform/recordings/requests?recordingCode=${encodeURIComponent(recordingCode)}`);
            const body = await res.json();
            setRequests(Array.isArray(body) ? body : (Array.isArray(body?.data) ? body.data : []));
        } catch (e) {
            message.error(`加载请求列表失败: ${e.message || e}`);
            setRequests([]);
        } finally {
            setRequestsLoading(false);
        }
    }, [request]);

    const openDetail = (row) => {
        setActive(row);
        setPlayResult(null);
        setCmpResult(null);
        setTargetUrl(row.targetUrl || '');
        loadRequests(row.recordingCode);
    };

    const doStart = async (values) => {
        try {
            const res = await request('mock-platform/recordings/start', {
                method: 'POST',
                body: JSON.stringify(values),
            });
            const body = await res.json().catch(() => ({}));
            if (body?.success) {
                message.success(`录制 ${values.recordingCode} 已开启`);
                setStartOpen(false);
                startForm.resetFields();
                loadList();
                Modal.info({
                    title: '采集入口',
                    width: 560,
                    content: (
                        <div>
                            <p style={{ margin: '8px 0' }}>把被测流量改发到这个前缀（<code>**</code> 后的路径会转发到目标并录制）：</p>
                            <Typography.Paragraph copyable code style={{ fontSize: 14, wordBreak: 'break-all' }}>
                                /api/mock/{values.recordingCode}/...
                            </Typography.Paragraph>
                            <p style={{ color: '#888', margin: 0 }}>目标服务: {values.targetUrl}</p>
                        </div>
                    ),
                });
            } else {
                message.error(body?.message || '开启录制失败');
            }
        } catch (e) {
            message.error(`开启录制失败: ${e.message || e}`);
        }
    };

    const doStop = async (row) => {
        try {
            const res = await request(`mock-platform/recordings/stop?recordingCode=${encodeURIComponent(row.recordingCode)}`, {
                method: 'POST',
            });
            const body = await res.json().catch(() => ({}));
            if (body?.success) {
                message.success(`已停止，共录到 ${body.totalRequests ?? '-'} 个请求`);
                loadList();
                if (active?.recordingCode === row.recordingCode) loadRequests(row.recordingCode);
            } else {
                message.error(body?.message || '停止录制失败');
            }
        } catch (e) {
            message.error(`停止录制失败: ${e.message || e}`);
        }
    };

    const doDelete = async (row) => {
        try {
            const res = await request(`mock-platform/recordings?recordingCode=${encodeURIComponent(row.recordingCode)}`, {
                method: 'DELETE',
            });
            const body = await res.json().catch(() => ({}));
            if (body?.success) {
                message.success(`已删除（连带清理 ${body.removedMockEndpoints ?? 0} 个关联 Mock 端点）`);
                if (active?.recordingCode === row.recordingCode) setActive(null);
                loadList();
            } else {
                message.error(body?.message || '删除失败');
            }
        } catch (e) {
            message.error(`删除失败: ${e.message || e}`);
        }
    };

    const doPlayback = async () => {
        if (!targetUrl) {
            message.warning('先填目标服务地址');
            return;
        }
        setPlayLoading(true);
        setPlayResult(null);
        try {
            const res = await request(`mock-platform/recordings/playback?recordingCode=${encodeURIComponent(active.recordingCode)}`, {
                method: 'POST',
                body: JSON.stringify({ targetUrl }),
            });
            const body = await res.json().catch(() => null);
            if (Array.isArray(body) && body.length) {
                setPlayResult(body[0]);
            } else if (body && !body.success) {
                message.error(body.message || '回放失败');
            } else {
                setPlayResult(body);
            }
        } catch (e) {
            message.error(`回放失败: ${e.message || e}`);
        } finally {
            setPlayLoading(false);
        }
    };

    const doCompare = async () => {
        if (!targetUrl) {
            message.warning('先填目标服务地址');
            return;
        }
        setCmpLoading(true);
        setCmpResult(null);
        try {
            const res = await request(`mock-platform/recordings/compare?recordingCode=${encodeURIComponent(active.recordingCode)}`, {
                method: 'POST',
                body: JSON.stringify({ targetUrl }),
            });
            const body = await res.json().catch(() => null);
            if (body?.success) {
                setCmpResult(body);
            } else {
                message.error(body?.message || '对比失败');
            }
        } catch (e) {
            message.error(`对比失败: ${e.message || e}`);
        } finally {
            setCmpLoading(false);
        }
    };

    const doImportMock = async () => {
        setImportLoading(true);
        try {
            const res = await request(`mock-platform/recordings/import?recordingCode=${encodeURIComponent(active.recordingCode)}`, {
                method: 'POST',
                body: JSON.stringify({ envCode: 'default' }),
            });
            const body = await res.json().catch(() => ({}));
            if (body?.success) {
                message.success(`已导入 ${body.imported ?? body.count ?? 0} 个 Mock 端点（在「Mock 端点」栏查看）`);
            } else {
                message.error(body?.message || '导入失败');
            }
        } catch (e) {
            message.error(`导入失败: ${e.message || e}`);
        } finally {
            setImportLoading(false);
        }
    };

    const filtered = useMemo(
        () => (statusFilter ? recordings.filter((r) => r.recordStatus === statusFilter) : recordings),
        [recordings, statusFilter]
    );

    const columns = [
        {
            title: '录制编码',
            dataIndex: 'recordingCode',
            key: 'recordingCode',
            width: 200,
            render: (v) => (
                <Typography.Text copyable={{ text: v }} style={{ fontFamily: 'monospace' }}>
                    {v}
                </Typography.Text>
            ),
        },
        { title: '名称', dataIndex: 'recordingName', key: 'recordingName', ellipsis: true },
        {
            title: '状态',
            dataIndex: 'recordStatus',
            key: 'recordStatus',
            width: 110,
            filters: [
                { text: '录制中', value: 'RECORDING' },
                { text: '已停止', value: 'STOPPED' },
            ],
            filteredValue: statusFilter ? [statusFilter] : null,
            render: (v) => <StatusTag status={v} />,
        },
        {
            title: '目标服务',
            dataIndex: 'targetUrl',
            key: 'targetUrl',
            ellipsis: true,
            width: 220,
            render: (v) => v || <span style={{ color: '#bbb' }}>-</span>,
        },
        {
            title: '请求数',
            dataIndex: 'totalRequests',
            key: 'totalRequests',
            width: 90,
            sorter: (a, b) => (a.totalRequests ?? 0) - (b.totalRequests ?? 0),
            render: (v) => v ?? 0,
        },
        {
            title: '开始 / 结束',
            key: 'time',
            width: 170,
            render: (_, r) => (
                <Tooltip title={`${fmtTime(r.startTime)} → ${fmtTime(r.endTime) || '进行中'}`}>
                    <span style={{ fontSize: 12, color: '#666' }}>{fmtTime(r.startTime)}</span>
                </Tooltip>
            ),
        },
        {
            title: '操作',
            key: 'actions',
            width: 220,
            render: (_, row) => (
                <Space size="small">
                    <Button size="small" type="link" onClick={() => openDetail(row)}>
                        详情
                    </Button>
                    {row.recordStatus === 'RECORDING' ? (
                        <Popconfirm title="停止后将不能再采集" onConfirm={() => doStop(row)} okText="停止" cancelText="取消">
                            <Button size="small" type="link" danger>
                                停止
                            </Button>
                        </Popconfirm>
                    ) : (
                        <Button size="small" type="link" onClick={() => openDetail(row)}>
                            回放
                        </Button>
                    )}
                    <Popconfirm title={`删除录制及关联 Mock 端点？`} onConfirm={() => doDelete(row)} okText="删除" cancelText="取消">
                        <Button size="small" type="link" danger>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    const requestColumns = [
        { title: '#', dataIndex: 'sequence', key: 'sequence', width: 50 },
        {
            title: '方法',
            dataIndex: 'requestMethod',
            key: 'requestMethod',
            width: 90,
            render: (v) => <Tag color={v === 'GET' ? 'blue' : v === 'POST' ? 'orange' : 'default'}>{v}</Tag>,
        },
        { title: 'URL', dataIndex: 'requestUrl', key: 'requestUrl', ellipsis: true },
        {
            title: '响应码',
            dataIndex: 'responseStatus',
            key: 'responseStatus',
            width: 90,
            render: (v) => (
                <Tag color={v >= 200 && v < 300 ? 'success' : v ? 'error' : 'default'}>{v ?? '-'}</Tag>
            ),
        },
        {
            title: '耗时',
            dataIndex: 'responseTime',
            key: 'responseTime',
            width: 90,
            render: (v) => (v != null ? `${v} ms` : '-'),
        },
        { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 160, render: fmtTime },
    ];

    const playResultColumns = [
        { title: '#', dataIndex: 'sequence', key: 'sequence', width: 50 },
        { title: '方法', dataIndex: 'method', key: 'method', width: 80 },
        { title: 'URL', dataIndex: 'url', key: 'url', ellipsis: true },
        {
            title: '结果',
            dataIndex: 'status',
            key: 'status',
            width: 90,
            render: (v) => <Tag color={v === 'PASS' ? 'success' : 'error'}>{v}</Tag>,
        },
        { title: '实际状态', dataIndex: 'actualStatus', key: 'actualStatus', width: 90, render: (v) => v ?? '-' },
        { title: '耗时', dataIndex: 'durationMs', key: 'durationMs', width: 90, render: (v) => (v != null ? `${v} ms` : '-') },
        {
            title: '错误',
            dataIndex: 'error',
            key: 'error',
            ellipsis: true,
            render: (v) => (v ? <Tooltip title={v}><span style={{ color: '#cf1322' }}>{v}</span></Tooltip> : '-'),
        },
    ];

    const diffColumns = [
        { title: '方法', dataIndex: 'method', key: 'method', width: 80, render: (v) => v ?? '-' },
        { title: 'URL', dataIndex: 'url', key: 'url', ellipsis: true },
        { title: '期望状态', dataIndex: 'expectedStatus', key: 'expectedStatus', width: 90, render: (v) => v ?? '-' },
        { title: '实际状态', dataIndex: 'actualStatus', key: 'actualStatus', width: 90, render: (v) => v ?? '-' },
        {
            title: '差异',
            key: 'diff',
            render: (_, r) =>
                r.error ? (
                    <span style={{ color: '#cf1322' }}>{r.error}</span>
                ) : (
                    <span>
                        body 长度 期望 {r.expectedBodyLength ?? '-'} / 实际 {r.actualBodyLength ?? '-'}
                    </span>
                ),
        },
    ];

    return (
        <div data-component="z-script-recording">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}

            <Space style={{ marginBottom: 16 }} wrap>
                <Button type="primary" icon={<ThunderboltOutlined />} onClick={() => setStartOpen(true)}>
                    开启录制
                </Button>
                <Button icon={<ReloadOutlined />} onClick={loadList} loading={loading}>
                    刷新
                </Button>
            </Space>

            <Table
                rowKey={(r) => r.recordingCode}
                columns={columns}
                dataSource={filtered}
                loading={loading}
                onChange={(_, filters) => setStatusFilter(filters?.recordStatus?.[0] ?? null)}
                pagination={{ pageSize: 10, showSizeChanger: false, hideOnSinglePage: true }}
                locale={{ emptyText: <Empty description="暂无录制会话 — 点「开启录制」开始" /> }}
                onRow={(row) => ({ onClick: () => openDetail(row), style: { cursor: 'pointer' } })}
            />

            <Drawer
                title={active ? `录制详情 — ${active.recordingCode}` : ''}
                width={900}
                open={!!active}
                onClose={() => setActive(null)}
                destroyOnClose
                extra={
                    active?.recordStatus === 'RECORDING' ? (
                        <Button danger onClick={() => doStop(active)}>
                            停止录制
                        </Button>
                    ) : null
                }
            >
                {active && (
                    <Tabs
                        defaultActiveKey="requests"
                        items={[
                            {
                                key: 'requests',
                                label: `请求列表 (${requests.length})`,
                                children: (
                                    <Table
                                        rowKey={(r) => r.id ?? r.sequence}
                                        size="small"
                                        columns={requestColumns}
                                        dataSource={requests}
                                        loading={requestsLoading}
                                        pagination={{ pageSize: 15, showSizeChanger: false, hideOnSinglePage: true }}
                                        locale={{ emptyText: <Empty description={`尚无请求 — 把流量发到 /api/mock/${active.recordingCode}/... 即会被采集`} /> }}
                                        expandable={{
                                            expandedRowRender: (r) => (
                                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                                                    <div>
                                                        <b>请求 Body</b>
                                                        <pre style={{ maxHeight: 200, overflow: 'auto', background: '#fafafa', padding: 8, fontSize: 12 }}>
                                                            {prettyJson(r.requestBody)}
                                                        </pre>
                                                    </div>
                                                    <div>
                                                        <b>响应 Body</b>
                                                        <pre style={{ maxHeight: 200, overflow: 'auto', background: '#fafafa', padding: 8, fontSize: 12 }}>
                                                            {prettyJson(r.responseBody)}
                                                        </pre>
                                                    </div>
                                                </div>
                                            ),
                                        }}
                                    />
                                ),
                            },
                            {
                                key: 'playback',
                                label: '回放到目标',
                                children: (
                                    <Space direction="vertical" style={{ width: '100%' }} size={12}>
                                        <Space.Compact style={{ width: '100%' }}>
                                            <Input
                                                placeholder="目标服务地址，例如 http://localhost:9000"
                                                value={targetUrl}
                                                onChange={(e) => setTargetUrl(e.target.value)}
                                                onPressEnter={doPlayback}
                                            />
                                            <Button type="primary" icon={<PlayCircleOutlined />} loading={playLoading} onClick={doPlayback}>
                                                回放
                                            </Button>
                                        </Space.Compact>
                                        {playResult ? (
                                            <>
                                                <Alert
                                                    type={playResult.fail > 0 ? 'warning' : 'success'}
                                                    showIcon
                                                    message={`共 ${playResult.total ?? 0} 个请求：${playResult.pass ?? 0} 通过 / ${playResult.fail ?? 0} 失败`}
                                                />
                                                <Table
                                                    rowKey={(r, i) => i}
                                                    size="small"
                                                    columns={playResultColumns}
                                                    dataSource={playResult.results || []}
                                                    pagination={false}
                                                    scroll={{ y: 360 }}
                                                />
                                            </>
                                        ) : (
                                            <Empty description="填目标地址后点回放，逐条重放录制的请求" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                                        )}
                                    </Space>
                                ),
                            },
                            {
                                key: 'compare',
                                label: '基线对比',
                                children: (
                                    <Space direction="vertical" style={{ width: '100%' }} size={12}>
                                        <Space.Compact style={{ width: '100%' }}>
                                            <Input
                                                placeholder="基线服务地址"
                                                value={targetUrl}
                                                onChange={(e) => setTargetUrl(e.target.value)}
                                                onPressEnter={doCompare}
                                            />
                                            <Button type="primary" loading={cmpLoading} onClick={doCompare}>
                                                对比
                                            </Button>
                                        </Space.Compact>
                                        {cmpResult ? (
                                            <>
                                                <Alert
                                                    type={cmpResult.diff > 0 ? 'warning' : 'success'}
                                                    showIcon
                                                    message={`共 ${cmpResult.total ?? 0} 个请求：${cmpResult.match ?? 0} 一致 / ${cmpResult.diff ?? 0} 不一致`}
                                                />
                                                {cmpResult.diffs?.length > 0 && (
                                                    <Table rowKey={(r, i) => i} size="small" columns={diffColumns} dataSource={cmpResult.diffs} pagination={false} />
                                                )}
                                            </>
                                        ) : (
                                            <Empty description="对比录制基线与目标服务的响应差异" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                                        )}
                                    </Space>
                                ),
                            },
                            {
                                key: 'mock',
                                label: '转成 Mock',
                                children: (
                                    <Space direction="vertical" style={{ width: '100%' }} size={12}>
                                        <Alert
                                            type="info"
                                            showIcon
                                            message="把录制的响应固化为 Mock 端点（default 环境），之后无需目标服务即可回放同样的响应。"
                                        />
                                        <Popconfirm title="确认导入为 Mock 端点？" onConfirm={doImportMock} okText="导入" cancelText="取消">
                                            <Button type="primary" loading={importLoading}>
                                                导入为 Mock 端点
                                            </Button>
                                        </Popconfirm>
                                    </Space>
                                ),
                            },
                        ]}
                    />
                )}
            </Drawer>

            <Modal
                title="开启录制"
                open={startOpen}
                onOk={() => startForm.submit()}
                onCancel={() => setStartOpen(false)}
                okText="开启"
                cancelText="取消"
                destroyOnClose
                width={520}
            >
                <Form form={startForm} layout="vertical" onFinish={doStart}>
                    <Form.Item
                        name="recordingCode"
                        label="录制编码"
                        rules={[
                            { required: true, message: '必填' },
                            { pattern: /^[A-Za-z0-9_-]+$/, message: '仅允许字母/数字/下划线/中划线' },
                        ]}
                        extra="采集入口将是 /api/mock/{编码}/..."
                    >
                        <Input placeholder="例如 rec_login_flow" />
                    </Form.Item>
                    <Form.Item name="recordingName" label="录制名称" rules={[{ required: true, message: '必填' }]}>
                        <Input placeholder="例如 登录流程录制" />
                    </Form.Item>
                    <Form.Item
                        name="targetUrl"
                        label="目标服务地址"
                        rules={[
                            { required: true, message: '必填' },
                            { pattern: /^https?:\/\//, message: '需以 http:// 或 https:// 开头' },
                        ]}
                    >
                        <Input placeholder="http://localhost:9000" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
}
