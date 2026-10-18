import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// 组件层 = library mode：产出 ES module + d.ts，供应用层与跨仓（z-opc 等）消费。
// react / react-dom / antd 走 peerDependencies，必须 external，否则组件层体积膨胀且多实例冲突。
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src'),
        },
    },
    build: {
        lib: {
            // 两个 entry 对应 package.json 的 exports 两张脸：`./dist/index.js`（.）与
            // `./dist/pages.js`（./pages，lead 005 §8.2/§8.6 #3 的 routes 清单）。
            // 原来只声明了 index 一个 entry，产物里根本没有 pages.js，
            // 而 exports 还指着它 —— 装到宿主上 import '.../pages' 直接 module not found。
            entry: {
                index: path.resolve(__dirname, 'src/index.jsx'),
                pages: path.resolve(__dirname, 'src/pages.js'),
            },
            formats: ['es'],
        },
        // ⚠️ Vite 没有 build.external，external 必须挂在 rollupOptions 下，
        // 否则 antd/react 会被整包打进组件层产物（实测 1.75MB）。
        rollupOptions: {
            external: ['react', 'react-dom', 'react/jsx-runtime', 'antd'],
        },
        outDir: 'dist',
        emptyOutDir: true,
        sourcemap: false,
    },
});
