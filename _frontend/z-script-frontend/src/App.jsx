import React, { useCallback, useEffect, useState } from 'react';
import { Layout, Tabs, Typography, Button, Space, Input, message, Alert, Modal, Select, Radio } from 'antd';
import { ScriptListView, MockEndpointListView, AppListView, ScriptVersionView, RecordingView, ScenarioView } from '@yuku123/z-script-frontend-component';

const { Header, Content } = Layout;

// 所有请求都带 base（/script/），生产同源、dev 由 vite proxy 转发到 8086
const api = (p) => `${import.meta.env.BASE_URL}api/${p}`;

// z-script 只有 app + AK 一种认证方式（ApiKeyAuthInterceptor 默认拒绝 /api/**），
// 控制台自己不例外：AK 存 localStorage，每次请求带 X-Api-Key。
const AK_STORAGE_KEY = 'z-script-console-api-key';

// 后端统一响应可能是裸数组 / {data} / {rows}，此处一次兜底，避免前端猜结构
const rowsOf = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (!payload || typeof payload !== 'object') return [];
  if (Array.isArray(payload.data)) return payload.data;
  if (Array.isArray(payload.rows)) return payload.rows;
  if (payload.data && Array.isArray(payload.data.list)) return payload.data.list;
  return [];
};

export default function App() {
  const [apiKey, setApiKey] = useState(() => localStorage.getItem(AK_STORAGE_KEY) || '');
  const [appName, setAppName] = useState('console');
  const [scripts, setScripts] = useState([]);
  const [endpoints, setEndpoints] = useState([]);
  const [apps, setApps] = useState([]);
  // 「版本/灰度」面板的独立状态：选中的脚本 + 版本列表
  const [selectedScriptId, setSelectedScriptId] = useState(null);
  const [versions, setVersions] = useState([]);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  // 「配置脚本」弹窗状态：null=关闭；编辑的是应用的 scope + allowed_scripts
  const [editApp, setEditApp] = useState(null);
  const [editScope, setEditScope] = useState('SPECIFIC');
  const [editScripts, setEditScripts] = useState([]);

  const headers = useCallback(
    () => ({ 'Content-Type': 'application/json', ...(apiKey ? { 'X-Api-Key': apiKey } : {}) }),
    [apiKey]
  );

  const applyApiKey = (value) => {
    setApiKey(value || '');
    if (value) localStorage.setItem(AK_STORAGE_KEY, value);
    else localStorage.removeItem(AK_STORAGE_KEY);
  };

  const request = useCallback(
    async (path, init = {}) => {
      const res = await fetch(api(path), { ...init, headers: { ...headers(), ...init.headers } });
      if (res.status === 401) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.errorCode || 'MISSING_API_KEY');
      }
      return res;
    },
    [headers]
  );

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const grab = async (path) => {
        const res = await request(path);
        if (!res.ok) throw new Error(`${path} → HTTP ${res.status}`);
        return rowsOf(await res.json());
      };
      const [s, m, a] = await Promise.all([
        grab('script/list'),
        grab('mock-platform/endpoints/list'),
        grab('script/app/list'),
      ]);
      setScripts(s);
      setEndpoints(m);
      setApps(a);
    } catch (e) {
      setError(String(e.message || e));
    } finally {
      setLoading(false);
    }
  }, [request]);

  useEffect(() => {
    load();
  }, [load]);

  const issueKey = async () => {
    const res = await fetch(api('script/api-key'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ appName, scope: 'ALL', description: 'z-script console' }),
    });
    const body = await res.json().catch(() => ({}));
    const data = body.data || {};
    if (!res.ok || !data.apiKey) {
      message.error(`签发失败: ${body.message || res.status}`);
      return;
    }
    // secret 后端只在创建时明文返回一次，控制台不做存储（凭证不落 localStorage）
    message.success(`已为 ${data.appName} 签发 AK，secret 仅显示一次: ${data.plainSecret}`);
    applyApiKey(data.apiKey);
    // 不再手动 load()：apiKey 变化会让下面的 useEffect 用新 AK 重新拉一次，
    // 手动再调会拿到旧闭包里的空 AK，产生一个后到的 401 假错误。
  };

  // scriptCode 是后端的 @RequestParam，必须走 query string；
  // body 是脚本自己的入参 Map（ScriptController#run 的 @RequestBody）。
  const runScript = async (row) => {
    const res = await request(`script/run?scriptCode=${encodeURIComponent(row.scriptCode)}`, {
      method: 'POST',
      body: '{}',
    });
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](
      ok
        ? `已执行 ${row.scriptCode}（${body.durationMs ?? 0}ms）${body.data === undefined ? '' : ` → ${JSON.stringify(body.data)}`}`
        : `执行失败: ${body.errorMessage || body.message || res.status}`
    );
  };

  const publishScript = async (row) => {
    const next = Number(row.status) === 1 ? 'unpublish' : 'publish';
    const res = await request(`script/${next}?scriptCode=${encodeURIComponent(row.scriptCode)}`, {
      method: 'POST',
    });
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](
      ok ? `${row.scriptCode} ${next} ok${body.httpPath ? ` → ${body.httpPath}` : ''}` : `${next} 失败: ${body.message || res.status}`
    );
    if (ok) load();
  };

  // 应用启停：禁用后该应用名下所有 Key 一并 403（APP_DISABLED）
  const toggleApp = async (row, next) => {
    const res = await request(`script/app?appCode=${encodeURIComponent(row.appCode)}`, {
      method: 'PUT',
      body: JSON.stringify({ status: next ? 1 : 0 }),
    });
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](`${row.appCode} ${next ? '启用' : '禁用'}${ok ? ' ok' : ` 失败: ${body.message || res.status}`}`);
    if (ok) load();
  };

  const openEditScripts = (row) => {
    let parsed = [];
    try {
      parsed = JSON.parse(row.allowedScripts || '[]');
    } catch (e) {
      parsed = [];
    }
    setEditApp(row);
    setEditScope(row.scope === 'ALL' ? 'ALL' : 'SPECIFIC');
    setEditScripts(Array.isArray(parsed) ? parsed : []);
  };

  const saveAppScripts = async () => {
    const res = await request(`script/app/scripts?appCode=${encodeURIComponent(editApp.appCode)}`, {
      method: 'POST',
      body: JSON.stringify({ scope: editScope, scripts: editScripts }),
    });
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](`${editApp.appCode} 脚本列表${ok ? '已保存' : `保存失败: ${body.message || res.status}`}`);
    if (ok) {
      setEditApp(null);
      load();
    }
  };

  // 版本/灰度面板：拉到选中脚本的版本列表。script/version/list/{scriptId} 是 GET，
  // scriptId 取自 /api/script/list 响应里的 id 字段（DO 上是 Long 主键）。
  const loadVersions = async (scriptId) => {
    if (!scriptId) {
      setVersions([]);
      return;
    }
    setVersionsLoading(true);
    try {
      const res = await request(`script/version/list/${encodeURIComponent(scriptId)}`);
      const body = await res.json().catch(() => ({}));
      setVersions(Array.isArray(body.data) ? body.data : []);
    } catch (e) {
      setVersions([]);
      message.error(`版本列表加载失败: ${e.message || e}`);
    } finally {
      setVersionsLoading(false);
    }
  };

  // 发版：全部字段在 Controller 侧是 @RequestParam，所以走 query string + POST，
  // 与现有 script/publish / script/run 同一套路。
  const publishVersion = async (payload) => {
    const qs = new URLSearchParams();
    Object.entries(payload).forEach(([k, v]) => {
      if (v !== undefined && v !== null) qs.append(k, v);
    });
    const res = await request(`script/version/publish?${qs.toString()}`, { method: 'POST' });
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](`发版${ok ? '成功' : `失败: ${body.message || res.status}`}`);
    if (ok && selectedScriptId) await loadVersions(selectedScriptId);
  };

  // 灰度权重：canaryWeight 0~100，>0 = 启用 GRAY，=0 = 降级为 DEPRECATED（见 service）
  const setCanaryVersion = async (versionId, weight) => {
    const res = await request(
      `script/version/canary/${encodeURIComponent(versionId)}?canaryWeight=${encodeURIComponent(weight)}`,
      { method: 'POST' }
    );
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](`灰度 ${weight}%${ok ? ' 已设置' : `失败: ${body.message || res.status}`}`);
    if (ok && selectedScriptId) await loadVersions(selectedScriptId);
  };

  const promoteVersion = async (versionId) => {
    const res = await request(`script/version/promote/${encodeURIComponent(versionId)}`, { method: 'POST' });
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](`晋升${ok ? '成功' : `失败: ${body.message || res.status}`}`);
    if (ok && selectedScriptId) await loadVersions(selectedScriptId);
  };

  const offlineVersion = async (versionId) => {
    const res = await request(`script/version/offline/${encodeURIComponent(versionId)}`, { method: 'POST' });
    const body = await res.json().catch(() => ({}));
    const ok = res.ok && body.success !== false;
    message[ok ? 'success' : 'error'](`下线${ok ? '成功' : `失败: ${body.message || res.status}`}`);
    if (ok && selectedScriptId) await loadVersions(selectedScriptId);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Typography.Title level={4} style={{ color: '#fff', margin: 0 }}>
          Z-Script 脚本平台 / Mock 平台
        </Typography.Title>
        <Space style={{ marginLeft: 'auto' }} size={8}>
          <Input
            size="small"
            style={{ width: 200 }}
            value={appName}
            onChange={(e) => setAppName(e.target.value)}
            placeholder="app 名"
            addonBefore="app"
          />
          <Input.Password
            size="small"
            style={{ width: 280 }}
            value={apiKey}
            onChange={(e) => applyApiKey(e.target.value)}
            placeholder="AK（X-Api-Key，zsk_live_...）"
          />
          <Button size="small" onClick={issueKey}>
            签发 AK
          </Button>
          <Button onClick={load} loading={loading}>
            刷新
          </Button>
        </Space>
      </Header>
      <Content style={{ padding: 24, background: '#f5f5f5' }}>
        {error && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message={`加载失败：${error}`}
            description={
              error === 'MISSING_API_KEY'
                ? '填一个 app 名后点「签发 AK」，或把已有 AK 粘贴到右上角输入框。z-script 全站只认 app + AK。'
                : '请确认 z-script-admin 已启动、MySQL 中已执行 _doc/002_deploy/init.sql。'
            }
          />
        )}
        <Tabs
          defaultActiveKey="scripts"
          items={[
            {
              key: 'apps',
              label: `应用 (${apps.length})`,
              children: (
                <AppListView apps={apps} loading={loading} onEditScripts={openEditScripts} onToggle={toggleApp} />
              ),
            },
            {
              key: 'scripts',
              label: `脚本 (${scripts.length})`,
              children: (
                <ScriptListView
                  scripts={scripts}
                  loading={loading}
                  onRun={runScript}
                  onPublish={publishScript}
                />
              ),
            },
            {
              key: 'mocks',
              label: `Mock 端点 (${endpoints.length})`,
              children: <MockEndpointListView endpoints={endpoints} loading={loading} />,
            },
            {
              key: 'versions',
              label: `版本/灰度 (${versions.length})`,
              children: (
                <ScriptVersionView
                  scripts={scripts}
                  selectedScriptId={selectedScriptId}
                  versions={versions}
                  loading={versionsLoading}
                  onSelectScript={(id) => {
                    setSelectedScriptId(id);
                    loadVersions(id);
                  }}
                  onPublish={publishVersion}
                  onSetCanary={setCanaryVersion}
                  onPromote={promoteVersion}
                  onOffline={offlineVersion}
                />
              ),
            },
            {
              key: 'recordings',
              label: '录制/回放',
              children: <RecordingView request={request} />,
            },
            {
              key: 'scenarios',
              label: '场景/状态机',
              children: <ScenarioView request={request} />,
            },
          ]}
        />
        <Modal
          title={`配置脚本 — ${editApp?.appCode || ''}`}
          open={!!editApp}
          onOk={saveAppScripts}
          onCancel={() => setEditApp(null)}
          okText="保存"
          cancelText="取消"
          destroyOnClose
        >
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Radio.Group value={editScope} onChange={(e) => setEditScope(e.target.value)}>
              <Radio.Button value="ALL">全部脚本</Radio.Button>
              <Radio.Button value="SPECIFIC">指定列表</Radio.Button>
            </Radio.Group>
            <Select
              mode="multiple"
              style={{ width: '100%' }}
              placeholder={editScope === 'ALL' ? 'ALL 模式下无需选择' : '选择该应用可调用的脚本'}
              disabled={editScope === 'ALL'}
              value={editScripts}
              onChange={setEditScripts}
              options={scripts.map((s) => ({ value: s.scriptCode, label: `${s.scriptCode}（${s.dslType}）` }))}
            />
          </Space>
        </Modal>
      </Content>
    </Layout>
  );
}
