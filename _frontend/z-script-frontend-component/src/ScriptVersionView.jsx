import React, { useMemo, useState } from 'react';
import { Table, Tag, Button, Space, Empty, Select, Modal, Input, InputNumber, Form, Alert } from 'antd';

// 脚本版本/灰度面板。规范 §5：组件层只接 props，不自己 fetch。
// 数据形状来自 ScriptVersionController：
//   id / scriptId / versionNo / status (DRAFT|PUBLISHED|GRAY|DEPRECATED) /
//   grayPercentage / isCurrent / changelog / createTime
const STATUS_COLOR = {
    PUBLISHED: 'success',
    GRAY: 'processing',
    DRAFT: 'default',
    DEPRECATED: 'error',
};
const STATUS_LABEL = {
    PUBLISHED: '生产',
    GRAY: '灰度',
    DRAFT: '草稿',
    DEPRECATED: '下线',
};

const formatTime = (t) => {
    if (!t) return '-';
    // 后端序列化 Date 默认是 ISO 字符串，这里只取到分钟
    const d = new Date(t);
    if (Number.isNaN(d.getTime())) return String(t);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

export default function ScriptVersionView({
    scripts = [],
    selectedScriptId,
    versions = [],
    loading = false,
    onSelectScript,
    onSetCanary,
    onPromote,
    onOffline,
    onPublish,
    title = '脚本版本 / 灰度',
}) {
    // 选择脚本
    const scriptOptions = useMemo(
        () =>
            scripts.map((s) => ({
                value: s.id ?? s.scriptCode,
                label: `${s.scriptCode}（${s.scriptName ?? ''}）`,
                payload: s,
            })),
        [scripts]
    );

    // 行内灰度权重输入（按 versionId 局部态）
    const [canaryDraft, setCanaryDraft] = useState({});

    // 发版 modal
    const [pubOpen, setPubOpen] = useState(false);
    const [pubForm] = Form.useForm();

    const selectedScript = useMemo(
        () => scripts.find((s) => (s.id ?? s.scriptCode) === selectedScriptId) || null,
        [scripts, selectedScriptId]
    );

    const openPublish = () => {
        if (!selectedScript) return;
        pubForm.resetFields();
        pubForm.setFieldsValue({
            scriptCode: selectedScript.scriptCode,
            dslType: selectedScript.dslType || 'EL',
            // 发版脚本时让上一次内容作默认值，方便迭代
            dslContent: selectedScript.dslContent || '',
            versionNo: '',
            changeLog: '',
            outputMapping: '',
        });
        setPubOpen(true);
    };

    const submitPublish = async () => {
        try {
            const v = await pubForm.validateFields();
            await onPublish?.({
                scriptId: selectedScript.id,
                scriptCode: selectedScript.scriptCode,
                versionNo: v.versionNo,
                dslType: v.dslType,
                dslContent: v.dslContent,
                outputMapping: v.outputMapping || undefined,
                changeLog: v.changeLog || undefined,
            });
            setPubOpen(false);
        } catch (e) {
            // antd 的 validateFields 失败会抛，无需再处理
            if (e && e.errorFields) return;
            throw e;
        }
    };

    const columns = [
        {
            title: '版本',
            dataIndex: 'versionNo',
            key: 'versionNo',
            width: 80,
            render: (v) => `v${v}`,
        },
        {
            title: 'DSL',
            dataIndex: 'dslType',
            key: 'dslType',
            width: 80,
            render: (v) => (v ? <Tag>{v}</Tag> : <span style={{ color: '#bbb' }}>-</span>),
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 100,
            render: (v) => <Tag color={STATUS_COLOR[v] || 'default'}>{STATUS_LABEL[v] || v}</Tag>,
        },
        {
            title: '灰度',
            dataIndex: 'grayPercentage',
            key: 'grayPercentage',
            width: 100,
            render: (v, row) =>
                row.status === 'GRAY' ? <Tag color="processing">{v ?? 0}%</Tag> : <span style={{ color: '#bbb' }}>-</span>,
        },
        {
            title: '当前',
            dataIndex: 'isCurrent',
            key: 'isCurrent',
            width: 80,
            render: (v) =>
                Number(v) === 1 ? <Tag color="success">线上</Tag> : <span style={{ color: '#bbb' }}>-</span>,
        },
        {
            title: '变更说明',
            dataIndex: 'changelog',
            key: 'changelog',
            ellipsis: true,
            render: (v) => v || <span style={{ color: '#bbb' }}>-</span>,
        },
        {
            title: '发布时间',
            dataIndex: 'publishedAt',
            key: 'publishedAt',
            width: 150,
            render: (v, r) => formatTime(v || r.createTime),
        },
        {
            title: '操作',
            key: 'actions',
            width: 280,
            render: (_, row) => {
                const draft = canaryDraft[row.id];
                return (
                    <Space size="small" wrap>
                        <InputNumber
                            size="small"
                            min={0}
                            max={100}
                            step={10}
                            value={draft ?? row.grayPercentage ?? 0}
                            onChange={(v) => setCanaryDraft((d) => ({ ...d, [row.id]: v ?? 0 }))}
                            style={{ width: 80 }}
                            addonAfter="%"
                        />
                        <Button
                            size="small"
                            type="link"
                            onClick={() => onSetCanary?.(row.id, draft ?? row.grayPercentage ?? 0)}
                        >
                            灰度
                        </Button>
                        <Button
                            size="small"
                            type="link"
                            disabled={row.status === 'PUBLISHED' && Number(row.isCurrent) === 1}
                            onClick={() => onPromote?.(row.id)}
                        >
                            晋升
                        </Button>
                        <Button
                            size="small"
                            type="link"
                            danger
                            disabled={row.status === 'DEPRECATED'}
                            onClick={() => onOffline?.(row.id)}
                        >
                            下线
                        </Button>
                    </Space>
                );
            },
        },
    ];

    return (
        <div data-component="z-script-version-list">
            {title && <h3 style={{ marginTop: 0 }}>{title}</h3>}

            <Space style={{ marginBottom: 16 }} wrap>
                <Select
                    style={{ minWidth: 280 }}
                    placeholder="选一个脚本查看版本"
                    value={selectedScriptId}
                    onChange={(v) => onSelectScript?.(v)}
                    options={scriptOptions}
                    showSearch
                    optionFilterProp="label"
                    notFoundContent="暂无脚本（先去「脚本」栏创建）"
                />
                <Button type="primary" disabled={!selectedScript} onClick={openPublish}>
                    发布新版本
                </Button>
            </Space>

            {!selectedScript && (
                <Alert
                    type="info"
                    showIcon
                    style={{ marginBottom: 16 }}
                    message="版本/灰度是按脚本隔离的，先在上方选一个脚本。"
                />
            )}

            <Table
                rowKey={(r) => r.id}
                columns={columns}
                dataSource={versions}
                loading={loading}
                pagination={{ pageSize: 10, showSizeChanger: false }}
                locale={{ emptyText: <Empty description={selectedScript ? '该脚本暂无版本' : '请先选脚本'} /> }}
            />

            <Modal
                title={`发布新版本 — ${selectedScript?.scriptCode || ''}`}
                open={pubOpen}
                onOk={submitPublish}
                onCancel={() => setPubOpen(false)}
                okText="发布"
                cancelText="取消"
                destroyOnClose
                width={680}
            >
                <Form form={pubForm} layout="vertical">
                    <Form.Item name="scriptCode" label="脚本编码">
                        <Input disabled />
                    </Form.Item>
                    <Space style={{ width: '100%' }} size={12}>
                        <Form.Item
                            name="versionNo"
                            label="版本号"
                            rules={[{ required: true, message: '必填，例如 v1.1.0' }]}
                            style={{ flex: 1, minWidth: 160 }}
                        >
                            <Input placeholder="v1.1.0" />
                        </Form.Item>
                        <Form.Item
                            name="dslType"
                            label="DSL"
                            rules={[{ required: true }]}
                            style={{ width: 120 }}
                        >
                            <Select
                                options={[
                                    { value: 'EL', label: 'EL' },
                                    { value: 'GROOVY', label: 'GROOVY' },
                                    { value: 'LUA', label: 'LUA' },
                                    { value: 'SQL', label: 'SQL' },
                                    { value: 'MOCK', label: 'MOCK' },
                                    { value: 'API_BRIDGE', label: 'API_BRIDGE' },
                                ]}
                            />
                        </Form.Item>
                    </Space>
                    <Form.Item
                        name="dslContent"
                        label="脚本内容"
                        rules={[{ required: true, message: '必填' }]}
                    >
                        <Input.TextArea rows={6} placeholder="DSL 源码" />
                    </Form.Item>
                    <Form.Item name="outputMapping" label="输出映射（可选）">
                        <Input placeholder="例如 ${data.text}" />
                    </Form.Item>
                    <Form.Item name="changeLog" label="变更说明（可选）">
                        <Input placeholder="本次发布变更点" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
}