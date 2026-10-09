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
            entry: path.resolve(__dirname, 'src/index.jsx'),
            formats: ['es'],
            fileName: () => 'index.js',
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
