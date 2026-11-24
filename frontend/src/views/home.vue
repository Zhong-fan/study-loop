<template>
    <div class="app-shell">
        <v-header />

        <main class="app-main">
            <router-view v-slot="{ Component, route }">
                <transition name="page" mode="out-in">
                    <component :is="Component" :key="route.path" />
                </transition>
            </router-view>
        </main>

        <footer class="app-footer">
            <span>
                版式参考
                <a href="https://overreacted.io" target="_blank" rel="noreferrer">overreacted.io</a>
                ·
                <a href="https://maggieappleton.com" target="_blank" rel="noreferrer">maggieappleton.com</a>
            </span>
            <span class="mono">{{ year }} · 学习手记</span>
        </footer>
    </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import vHeader from '@/components/header.vue';

const route = useRoute();
const year = new Date().getFullYear();

// 移动端键盘/输入框滚动后，切页会沿用旧位置；路由稳定后再兜底滚到顶部
onMounted(() => {
    window.scrollTo(0, 0);
    setTimeout(() => window.scrollTo(0, 0), 60);
});

watch(
    () => route.path,
    async () => {
        await nextTick();
        window.scrollTo(0, 0);
        setTimeout(() => window.scrollTo(0, 0), 60);
    }
);
</script>

<style scoped>
.app-shell {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    background: var(--sl-bg);
}

.app-main {
    flex: 1;
    width: 100%;
    max-width: var(--sl-content-w);
    margin: 0 auto;
    padding: 0 40px 80px;
}

.app-footer a {
    color: var(--sl-text-secondary);
    border-bottom: 1px solid var(--sl-border);
}
.app-footer a:hover {
    color: var(--sl-primary);
    border-color: var(--sl-primary);
}

.app-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    max-width: var(--sl-content-w);
    margin: 0 auto;
    padding: 22px 40px 34px;
    font-size: 12px;
    color: var(--sl-muted);
    border-top: 1px solid var(--sl-border);
}

@media (max-width: 780px) {
    .app-main {
        padding: 0 18px 56px;
    }
    .app-footer {
        padding: 18px 18px 28px;
    }
}
</style>
