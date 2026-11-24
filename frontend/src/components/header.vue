<template>
    <header class="masthead">
        <router-link to="/dashboard" class="brand">
            <span class="brand-mark">CMMR</span>
            <span class="brand-sub">学习手记</span>
        </router-link>

        <nav class="mast-nav">
            <router-link
                to="/dashboard"
                class="nav-item link-underline"
                :class="{ active: route.path === '/dashboard' }"
            >
                概览
            </router-link>
            <router-link
                to="/practice"
                class="nav-item link-underline"
                :class="{ active: route.path === '/practice' }"
            >
                做题
            </router-link>
        </nav>

        <div class="mast-tools">
            <button type="button" class="tool-link" title="资料与工具" @click="router.push('/tools')">
                <el-icon><Tools /></el-icon>
                <span>资料与工具</span>
            </button>

            <el-popover placement="bottom-end" :width="340" trigger="click" @show="loadNotifications">
                <template #reference>
                    <button type="button" class="tool-link icon-only" title="消息">
                        <el-icon><Bell /></el-icon>
                        <span v-if="unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
                    </button>
                </template>

                <div class="notice-head">
                    <span class="mono">消息</span>
                    <button type="button" class="text-btn" @click="readAll">全部已读</button>
                </div>
                <p v-if="notices.length === 0" class="notice-empty">暂无消息</p>
                <div v-for="item in notices" :key="item.id" class="notice-item" :class="{ unread: !item.readAt }">
                    <div class="notice-title">{{ item.title }}</div>
                    <div class="notice-content">{{ item.content }}</div>
                </div>
                <button
                    v-if="notices.length < noticeTotal"
                    type="button"
                    class="text-btn more"
                    :disabled="noticeLoading"
                    @click="loadMoreNotices"
                >
                    加载更多（{{ notices.length }}/{{ noticeTotal }}）
                </button>
            </el-popover>

            <el-dropdown trigger="click" @command="handleCommand">
                <button type="button" class="user-chip">
                    <span>{{ username }}</span>
                    <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="dashboard">概览</el-dropdown-item>
                        <el-dropdown-item command="tools">资料与工具</el-dropdown-item>
                        <el-dropdown-item divided command="loginout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </header>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowDown, Bell, Tools } from '@element-plus/icons-vue';
import { getNotifications, markNotificationsRead } from '@/api';

const router = useRouter();
const route = useRoute();

const cached = JSON.parse(localStorage.getItem('user') || '{}');
const username: string = cached.nickname || cached.username || '未登录';

const notices = ref<any[]>([]);
const unread = ref(0);
const noticePage = ref(1);
const noticeTotal = ref(0);
const noticeLoading = ref(false);

const loadNotifications = async (append = false) => {
    noticeLoading.value = true;
    const target = append ? noticePage.value + 1 : 1;
    try {
        const data = await getNotifications(target, 10);
        notices.value = append ? [...notices.value, ...(data.items || [])] : data.items || [];
        unread.value = data.unread || 0;
        noticeTotal.value = data.total || 0;
        noticePage.value = target;
    } catch (e) {
        // 后端没起来时不打断页面
    } finally {
        noticeLoading.value = false;
    }
};

const loadMoreNotices = async () => {
    await loadNotifications(true);
};

const readAll = async () => {
    await markNotificationsRead();
    await loadNotifications();
};

const handleCommand = (command: string) => {
    if (command === 'loginout') {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        router.push('/login');
    } else if (command === 'dashboard') {
        router.push('/dashboard');
    } else if (command === 'tools') {
        router.push('/tools');
    }
};

onMounted(async () => {
    try {
        await loadNotifications();
        noticePage.value = 1;
    } catch (e) {
        // 忽略
    }
});
</script>

<style scoped>
.masthead {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    align-items: center;
    gap: 40px;
    height: var(--sl-header-h);
    padding: 0 40px;
    background: rgba(246, 242, 233, 0.86);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid var(--sl-border);
}

.brand {
    display: flex;
    align-items: baseline;
    gap: 10px;
    text-decoration: none;
}
.brand-mark {
    font-family: var(--sl-font-display);
    font-size: 24px;
    letter-spacing: -0.02em;
    color: var(--sl-text);
}
.brand-sub {
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.18em;
    text-transform: uppercase;
    color: var(--sl-muted);
}

.mast-nav {
    display: flex;
    align-items: center;
    gap: 28px;
}
.nav-item {
    padding: 4px 0;
    font-family: var(--sl-font-mono);
    font-size: 11px;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: var(--sl-text-secondary);
    text-decoration: none;
    transition: color 0.18s ease;
}
.nav-item:hover,
.nav-item.active {
    color: var(--sl-primary);
}

.mast-tools {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-left: auto;
}
.tool-link {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0;
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.1em;
    color: var(--sl-muted);
    background: none;
    border: 0;
    cursor: pointer;
    transition: color 0.18s ease;
}
.tool-link:hover {
    color: var(--sl-primary);
}
.tool-link.icon-only {
    font-size: 16px;
}
.badge {
    position: absolute;
    top: -6px;
    right: -8px;
    min-width: 15px;
    height: 15px;
    padding: 0 3px;
    font-family: var(--sl-font-mono);
    font-size: 9px;
    line-height: 15px;
    text-align: center;
    color: #fff;
    background: var(--sl-primary);
    border-radius: 999px;
}

.user-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 0;
    font-family: var(--sl-font-body);
    font-size: 13px;
    color: var(--sl-text);
    background: none;
    border: 0;
    cursor: pointer;
}
.user-chip:hover {
    color: var(--sl-primary);
}

.notice-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 8px;
    margin-bottom: 4px;
    border-bottom: 1px solid var(--sl-border);
}
.text-btn {
    padding: 0;
    font-family: var(--sl-font-body);
    font-size: 12px;
    color: var(--sl-primary);
    background: none;
    border: 0;
    cursor: pointer;
}
.text-btn.more {
    margin-top: 10px;
    color: var(--sl-muted);
}
.notice-empty {
    margin: 16px 0;
    font-family: var(--sl-font-display);
    font-size: 15px;
    color: var(--sl-muted);
    text-align: center;
}
.notice-item {
    padding: 10px 0;
    border-bottom: 1px solid var(--sl-border);
}
.notice-item.unread .notice-title::before {
    content: '';
    display: inline-block;
    width: 5px;
    height: 5px;
    margin-right: 7px;
    vertical-align: middle;
    background: var(--sl-primary);
    border-radius: 50%;
}
.notice-title {
    font-size: 13px;
    font-weight: 500;
    color: var(--sl-text);
}
.notice-content {
    margin-top: 3px;
    font-size: 12px;
    line-height: 1.6;
    color: var(--sl-muted);
}

@media (max-width: 780px) {
    .masthead {
        gap: 20px;
        padding: 0 18px;
    }
    .brand-sub,
    .tool-link span:not(.badge) {
        display: none;
    }
    .mast-nav {
        gap: 18px;
    }
}
</style>
