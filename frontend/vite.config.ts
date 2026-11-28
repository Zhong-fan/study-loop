import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/vite';
import vue from '@vitejs/plugin-vue';
import VueSetupExtend from 'vite-plugin-vue-setup-extend';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

const proxy = {
    // 本地联调把 /api 转发到网关，避免跨域
    '/api': {
        target: 'http://127.0.0.1:10080',
        changeOrigin: true
    }
};

export default defineConfig({
    base: '/',
    plugins: [
        tailwindcss(),
        vue(),
        VueSetupExtend(),
        AutoImport({
            resolvers: [ElementPlusResolver()]
        }),
        Components({
            resolvers: [ElementPlusResolver()]
        })
    ],
    server: {
        host: '127.0.0.1',
        port: 5173,
        proxy
    },
    preview: {
        host: '127.0.0.1',
        port: 4173,
        proxy
    },
    resolve: {
        alias: {
            '@': '/src',
            '~': '/src/assets'
        }
    },
    define: {
        __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'true'
    }
});

