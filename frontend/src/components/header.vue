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
                <!-- 今日待复习提醒：数字来自统计接口，点一下进复习页 -->
                <div class="btn-icon" @click="router.push('/dashboard')">
                    <el-tooltip effect="dark" :content="`今日待复习 ${dueToday} 道`" placement="bottom">
                        <i class="el-icon-lx-notice"></i>
                    </el-tooltip>
                    <span class="badge" v-if="dueToday > 0">{{ dueToday > 99 ? '99+' : dueToday }}</span>
                </div>

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
import { getOverview } from '@/api';
import imgurl from '../assets/img/img.jpg';

const router = useRouter();
const sidebar = useSidebarStore();

// 登录时写入的用户信息
const cached = JSON.parse(localStorage.getItem('user') || '{}');
const username: string = cached.nickname || cached.username || '未登录';

const dueToday = ref(0);

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
    if (document.body.clientWidth < 1500) {
        collapseChage();
    }
    try {
        const overview = await getOverview();
        dueToday.value = overview.dueToday;
    } catch (e) {
        // 后端没起来时顶栏不显示数字即可，不打断页面
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
    height: 70px;
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
    height: 70px;
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
</style>
