<template>
    <div class="wrapper">
        <v-header />
        <v-sidebar />
        <div class="content-box" :class="{ 'content-collapse': sidebar.collapse }">
            <v-tabs></v-tabs>
            <div class="content">
                <!-- 不做 transition/keep-alive：切换动画在部分环境下会卡在 leave 阶段，
                     导致地址已变但旧页面不卸载，表现为"导航点不动"。 -->
                <router-view />
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { onMounted } from 'vue';
import { useSidebarStore } from '@/store/sidebar';
import vHeader from '@/components/header.vue';
import vSidebar from '@/components/sidebar.vue';
import vTabs from '@/components/tabs.vue';

const sidebar = useSidebarStore();

// 路由懒加载在开发模式下首次访问才编译，点击菜单会等好几秒。
// 进入主界面后空闲预取，首次点击即可直接命中缓存。
const prefetch = [
    () => import('../views/courses.vue'),
    () => import('../views/questions.vue'),
    () => import('../views/quiz.vue'),
    () => import('../views/ai.vue'),
    () => import('../views/stats.vue'),
    () => import('../views/predict.vue'),
    () => import('../views/exams.vue'),
    () => import('../views/admin.vue'),
    () => import('../views/practice.vue')
];

onMounted(() => {
    const warm = () => prefetch.forEach((load, i) => setTimeout(() => load().catch(() => {}), i * 120));
    if ('requestIdleCallback' in window) {
        (window as any).requestIdleCallback(warm, { timeout: 3000 });
    } else {
        setTimeout(warm, 500);
    }
});
</script>

<style>
.wrapper {
    height: 100vh;
    overflow: hidden;
}
.content-box {
    position: absolute;
    left: var(--sl-sidebar-w);
    right: 0;
    top: var(--sl-header-h);
    bottom: 0;
    -webkit-transition: left 0.25s ease;
    transition: left 0.25s ease;
    background: var(--sl-bg);
    overflow: hidden;
}

.content {
    width: auto;
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
}

.content::-webkit-scrollbar {
    width: 0;
}

.content-collapse {
    left: var(--sl-sidebar-collapsed);
}
</style>
