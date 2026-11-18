import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import VueSetupExtend from 'vite-plugin-vue-setup-extend';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default defineConfig({
    base: '/',
    plugins: [
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
        proxy: {
            // 开发时把 /api 转发到网关，避免跨域
            '/api': {
                target: 'http://127.0.0.1:10080',
                changeOrigin: true
            }
        }
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

