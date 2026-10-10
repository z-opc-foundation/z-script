import { CodeOutlined, HomeOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import ConsolePage from './ConsolePage'

/** 菜单 + 路由清单（lead 008 §10/§14/§16）。整个 AK 控制台是有状态单页（共享 apiKey/脚本选中态），故只占一个菜单。 */
export const menuItems = [
    { key: '/z-script/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-script/console', label: '脚本控制台', icon: <CodeOutlined /> },
]

export const routes = [
    { path: '/z-script/home', Component: HomePage },
    { path: '/z-script/console', Component: ConsolePage },
]
