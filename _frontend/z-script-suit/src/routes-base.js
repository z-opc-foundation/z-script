/**
 * 带 base 前缀的跳转助手（与 z-schedule 同解法）。
 *
 * <Route path> 由 react-router 自己拼 basename，所以路由表里的 `/z-script/*` 不用改。
 * 但 `window.location.href` / `window.location.replace` 是整页跳转，完全绕过 router，
 * 不自己拼前缀就少一层 base：应用挂在 context-path `/script` 下，写死的 `/z-script/login`
 * 会打到 `/z-script/login` ⇒ 404 白屏。nginx 根路径部署时 base 是 `/`，那时看不出来 ——
 * 又是只在一种部署形态下炸。
 */
const RAW = import.meta.env.BASE_URL || '/'

export const BASE = RAW !== '/' ? RAW.replace(/\/$/, '') : ''

export const at = (path) => `${BASE}${path}`
