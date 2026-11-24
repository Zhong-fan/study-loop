import { createApp } from 'vue';
import { createPinia } from 'pinia';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import router from './router';
import 'element-plus/dist/index.css';
import './assets/css/tokens.css';
import './assets/css/grid.css';
import './assets/css/motion.css';
import './assets/css/editorial.css';

const app = createApp(App);
app.use(createPinia());
app.use(router);

// 注册 element-plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

app.mount('#app');
