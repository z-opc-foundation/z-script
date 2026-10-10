/**
 * z-script-component 路由清单（lead 005 §8.6 #3：./pages 命名导出 routes，非空数组）
 *
 * 占位说明：z-script 的页面代码现仍住在 z-z-script-suit/src/ 下（manifest + page 文件），
 * 本 manifest 现阶段只列骨架路由供主壳的 domainRoutes 探测；正式消费请走 suit。
 * 下次重构把 page 文件搬入 component 后，Component 字段直接换成同模块 import 即可。
 */
import { ScriptListView } from '.';
import { ScriptVersionView } from '.';
import { ScenarioView } from '.';
import { RecordingView } from '.';
import { AppListView } from '.';
import { MockEndpointListView } from '.';

export const appMeta = { title: 'z-script 控制台', short: 'z-script' }

export const routes = [
    { path: '/z-script/script', title: '脚本列表', order: 1, Component: ScriptListView },
    { path: '/z-script/script/version', title: '版本', order: 2, Component: ScriptVersionView },
    { path: '/z-script/script/scenario', title: '场景', order: 3, Component: ScenarioView },
    { path: '/z-script/script/recording', title: '录制', order: 4, Component: RecordingView },
    { path: '/z-script/script/app', title: '应用', order: 5, Component: AppListView },
    { path: '/z-script/script/mock', title: 'Mock 端点', order: 6, Component: MockEndpointListView },
]
