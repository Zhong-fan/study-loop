<template>
    <div class="app-ui flex h-screen overflow-hidden bg-canvas font-sans text-ink">
        <!-- 桌面侧栏 -->
        <aside class="hidden w-[200px] shrink-0 flex-col border-r border-line bg-surface-2 md:flex">
            <div class="flex items-center gap-2.5 px-4 pb-3 pt-5">
                <div class="grid size-9 shrink-0 place-items-center rounded-lg bg-brand shadow-sm">
                    <BookOpenIcon class="size-5 text-white" />
                </div>
                <div class="leading-tight">
                    <div class="text-lg font-semibold tracking-tight text-brand">CMMR</div>
                    <div class="text-[10px] font-medium tracking-[0.2em] text-ink-subtle">STUDY</div>
                </div>
            </div>

            <nav class="flex flex-col gap-1 px-3 py-2">
                <RouterLink
                    v-for="item in NAV_ITEMS"
                    :key="item.to"
                    :to="item.to"
                    class="flex items-center gap-2.5 rounded-md px-3 py-2 no-underline outline-none transition-colors focus-visible:ring-2 focus-visible:ring-focus"
                    :class="isActive(item.to) ? 'bg-brand-soft font-medium text-ink-blue' : 'text-ink hover:bg-surface'"
                >
                    <component
                        :is="item.icon"
                        class="size-4 shrink-0"
                        :class="isActive(item.to) ? 'text-ink-blue' : 'text-ink-muted'"
                    />
                    <span>{{ item.label }}</span>
                </RouterLink>
            </nav>

            <div class="mt-auto border-t border-line-soft px-3 py-3">
                <div class="flex items-center gap-2.5">
                    <div class="grid size-8 shrink-0 place-items-center rounded-full bg-brand-soft text-xs font-semibold text-ink-blue">
                        {{ initials }}
                    </div>
                    <div class="min-w-0 flex-1">
                        <div class="truncate text-sm font-medium text-ink-strong">{{ displayName }}</div>
                        <div class="truncate text-xs text-ink-muted">学生</div>
                    </div>
                    <button
                        type="button"
                        class="rounded-sm p-1 text-ink-muted outline-none hover:text-danger focus-visible:ring-2 focus-visible:ring-focus"
                        title="退出登录"
                        @click="logout"
                    >
                        <LogOutIcon class="size-4" />
                    </button>
                </div>
            </div>
        </aside>

        <!-- 主列 -->
        <div class="flex min-w-0 flex-1 flex-col">
            <header class="flex h-14 shrink-0 items-center justify-between border-b border-line bg-surface px-4 md:px-5">
                <div class="flex items-center gap-2.5">
                    <span class="text-base font-semibold tracking-tight text-brand md:hidden">CMMR</span>
                    <span class="hidden text-sm font-medium text-ink md:block">{{ pageTitle }}</span>
                </div>
                <div class="flex items-center gap-2">
                    <RouterLink
                        to="/tools"
                        class="rounded-md px-2 py-1 text-xs text-ink-muted no-underline hover:bg-surface-2 hover:text-ink"
                    >
                        资料与工具
                    </RouterLink>
                    <div class="grid size-7 place-items-center rounded-full bg-brand-soft text-[11px] font-semibold text-ink-blue md:hidden">
                        {{ initials }}
                    </div>
                </div>
            </header>

            <main class="min-h-0 flex-1 overflow-y-auto p-4 pb-24 md:p-5 md:pb-5">
                <router-view />
            </main>
        </div>

        <!-- 移动端底部导航 -->
        <nav class="fixed inset-x-0 bottom-0 z-20 flex border-t border-line bg-surface/95 backdrop-blur md:hidden">
            <RouterLink
                v-for="item in NAV_ITEMS"
                :key="item.to"
                :to="item.to"
                class="flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] no-underline"
                :class="isActive(item.to) ? 'text-ink-blue' : 'text-ink-muted'"
            >
                <component :is="item.icon" class="size-[18px]" />
                <span>{{ item.label }}</span>
            </RouterLink>
        </nav>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, type Component } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
    ArchiveIcon,
    BarChart3Icon,
    BookOpenIcon,
    LayoutDashboardIcon,
    LogOutIcon,
    TriangleAlertIcon,
    ZapIcon
} from '@lucide/vue';

const route = useRoute();
const router = useRouter();

const user = computed(() => {
    try {
        return JSON.parse(localStorage.getItem('user') || '{}');
    } catch {
        return {};
    }
});
const displayName = computed(() => user.value.nickname || user.value.username || '未登录');
const initials = computed(() => (displayName.value || 'C').slice(0, 1).toUpperCase());
const pageTitle = computed(() => (route.meta.title as string) || '');

const NAV_ITEMS: { to: string; label: string; icon: Component }[] = [
    { to: '/dashboard', label: '概览', icon: LayoutDashboardIcon },
    { to: '/practice', label: '做题', icon: ZapIcon },
    { to: '/questions', label: '题库', icon: ArchiveIcon },
    { to: '/wrong-book', label: '错题', icon: TriangleAlertIcon },
    { to: '/stats', label: '统计', icon: BarChart3Icon }
];

function isActive(to: string) {
    return route.path === to || route.path.startsWith(to + '/');
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    router.push('/login');
}

onMounted(() => {
    window.scrollTo(0, 0);
});
</script>
