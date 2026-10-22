import { CodeOutlined, HomeOutlined, ReadOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import ConsolePage from './ConsolePage'
// 组件文档页在 component 包（DOC.md 与薄包装都归它），suit 只负责挂菜单与路由。
import { ComponentDocPage } from '@yuku123/z-script-component/pages'

/** 菜单 + 路由清单（lead 008 §10/§14/§16）。整个 AK 控制台是有状态单页（共享 apiKey/脚本选中态），故只占一个菜单。 */
export const menuItems = [
    { key: '/z-script/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-script/console', label: '脚本控制台', icon: <CodeOutlined /> },
    { key: '/z-script/docs', label: '组件文档', icon: <ReadOutlined /> },
]

export const routes = [
    { path: '/z-script/home', Component: HomePage },
    { path: '/z-script/console', Component: ConsolePage },
    { path: '/z-script/docs', Component: ComponentDocPage },
]
