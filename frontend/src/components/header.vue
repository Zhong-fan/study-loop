<template>
    <div class="header">
        <div class="header-left">
            <img class="logo" src="../assets/img/logo.svg" alt="" />
            <div class="web-title">CMMR 学习助手</div>
            <div class="collapse-btn" @click="collapseChage">
                <el-icon v-if="sidebar.collapse"><Expand /></el-icon>
                <el-icon v-else><Fold /></el-icon>
            </div>
        </div>

        <div class="header-right">
            <div class="header-user-con">
                <!-- 站内信：数字是未读数，点开看列表 -->
                <el-popover placement="bottom" :width="330" trigger="click" @show="loadNotifications">
                    <template #reference>
                        <div class="btn-icon">
                            <i class="el-icon-lx-notice"></i>
                            <span class="badge" v-if="unread > 0">{{ unread > 99 ? '99+' : unread }}</span>
                        </div>
                    </template>

                    <div class="notice-head">
                        <span>消息</span>
                        <el-button link type="primary" size="small" @click="readAll">全部已读</el-button>
                    </div>
                    <el-empty v-if="notices.length === 0" description="暂无消息" :image-size="50" />
                    <el-button
                        v-if="notices.length < noticeTotal"
                        class="notice-more"
                        link
                        type="primary"
                        size="small"
                        :loading="noticeLoading"
                        @click="loadMoreNotices"
                    >
                        加载更多（{{ notices.length }}/{{ noticeTotal }}）
                    </el-button>
                    <div
                        v-for="item in notices"
                        :key="item.id"
                        class="notice-item"
                        :class="{ unread: !item.readAt }"
                    >
                        <div class="notice-title">{{ item.title }}</div>
                        <div class="notice-content">{{ item.content }}</div>
                    </div>
                </el-popover>

                <el-avatar class="user-avator" :size="30" :src="imgurl" />

                <el-dropdown class="user-name" trigger="click" @command="handleCommand">
                    <span class="el-dropdown-link">
                        {{ username }}
                        <el-icon class="el-icon--right"><arrow-down /></el-icon>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="dashboard">今日复习</el-dropdown-item>
                            <el-dropdown-item divided command="loginout">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useSidebarStore } from '../store/sidebar';
import { getNotifications, markNotificationsRead } from '@/api';
import imgurl from '../assets/img/img.jpg';

const router = useRouter();
const sidebar = useSidebarStore();

// 登录时写入的用户信息
const cached = JSON.parse(localStorage.getItem('user') || '{}');
const username: string = cached.nickname || cached.username || '未登录';

const notices = ref<any[]>([]);
const unread = ref(0);
const noticePage = ref(1);
const noticeTotal = ref(0);
const noticeLoading = ref(false);

const loadNotifications = async (append = false) => {
    noticeLoading.value = true;
    try {
        const data = await getNotifications(append ? noticePage.value : 1, 10);
        notices.value = append ? [...notices.value, ...(data.items || [])] : (data.items || []);
        unread.value = data.unread || 0;
        noticeTotal.value = data.total || 0;
    } catch (e) {
        // 后端没起来时不显示消息，不打断页面
    } finally {
        noticeLoading.value = false;
    }
};

const loadMoreNotices = async () => {
    noticePage.value += 1;
    await loadNotifications(true);
};

const readAll = async () => {
    await markNotificationsRead();
    await loadNotifications();
};

const collapseChage = () => {
    sidebar.handleCollapse();
};

const handleCommand = (command: string) => {
    if (command === 'loginout') {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        router.push('/login');
    } else if (command === 'dashboard') {
        router.push('/dashboard');
    }
};

onMounted(async () => {
    if (document.body.clientWidth < 1180) {
        collapseChage();
    }
    try {
        await loadNotifications();
        noticePage.value = 1;
    } catch (e) {
        // 忽略：顶栏不因为消息接口失败而报错
    }
});
</script>

<style scoped>
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-sizing: border-box;
    width: 100%;
    height: var(--sl-header-h);
    color: var(--header-text-color);
    background-color: var(--header-bg-color);
    border-bottom: 1px solid #ddd;
}
.header-left {
    display: flex;
    align-items: center;
    padding-left: 20px;
    height: 100%;
}
.logo {
    width: 35px;
}
.web-title {
    margin: 0 40px 0 10px;
    font-size: 22px;
}
.collapse-btn {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    padding: 0 10px;
    cursor: pointer;
    opacity: 0.8;
    font-size: 22px;
}
.collapse-btn:hover {
    opacity: 1;
}
.header-right {
    float: right;
    padding-right: 50px;
}
.header-user-con {
    display: flex;
    height: var(--sl-header-h);
    align-items: center;
}
.btn-icon {
    position: relative;
    width: 30px;
    height: 30px;
    text-align: center;
    cursor: pointer;
    display: flex;
    align-items: center;
    color: var(--header-text-color);
    margin: 0 8px;
    font-size: 20px;
}
.badge {
    position: absolute;
    right: -6px;
    top: -4px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    background: #f56c6c;
    color: #fff;
    font-size: 11px;
    line-height: 16px;
}
.user-avator {
    margin: 0 10px 0 20px;
}
.el-dropdown-link {
    color: var(--header-text-color);
    cursor: pointer;
    display: flex;
    align-items: center;
}
.notice-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 6px;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 6px;
    font-weight: 600;
}
.notice-item {
    padding: 6px 0;
    border-bottom: 1px dashed #f0f0f0;
}
.notice-item.unread .notice-title::before {
    content: '';
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #f56c6c;
    margin-right: 6px;
    vertical-align: middle;
}
.notice-title {
    font-size: 13px;
    color: #303133;
}
.notice-content {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
}
</style>
