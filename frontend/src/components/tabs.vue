<template>
    <div class="tabs-container">
        <el-tabs v-model="activePath" class="tabs" type="card" closable @tab-click="clickTabls" @tab-remove="closeTabs">
            <el-tab-pane
                v-for="item in tabs.list"
                :key="item.path"
                :label="item.title"
                :name="item.path"
                @click="setTags(item)"
            ></el-tab-pane>
        </el-tabs>
        <div class="Tabs-close-box">
            <el-dropdown @command="handleTags">
                <el-button size="small" type="primary" plain>
                    标签选项
                    <el-icon class="el-icon--right">
                        <arrow-down />
                    </el-icon>
                </el-button>
                <template #dropdown>
                    <el-dropdown-menu size="small">
                        <el-dropdown-item command="other">关闭其他</el-dropdown-item>
                        <el-dropdown-item command="current">关闭当前</el-dropdown-item>
                        <el-dropdown-item command="all">关闭所有</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useTabsStore } from '../store/tabs';
import { onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const activePath = ref(route.fullPath);
const tabs = useTabsStore();
// 设置标签
const setTags = (route: any) => {
    const isExist = tabs.list.some((item) => {
        return item.path === route.fullPath;
    });
    if (!isExist) {
        tabs.setTabsItem({
            name: route.name,
            title: route.meta.title,
            path: route.fullPath,
        });
    }
};
setTags(route);
onBeforeRouteUpdate((to) => {
    setTags(to);
});

// 关闭全部标签
const closeAll = () => {
    tabs.clearTabs();
    router.push('/');
};
// 关闭其他标签
const closeOther = () => {
    const curItem = tabs.list.filter((item) => {
        return item.path === route.fullPath;
    });
    tabs.closeTabsOther(curItem);
};
const handleTags = (command: string) => {
    switch (command) {
        case 'current':
            // 关闭当前页面的标签页
            tabs.closeCurrentTag({
                $router: router,
                $route: route,
            });
            break;
        case 'all':
            closeAll();
            break;

        case 'other':
            closeOther();
            break;
    }
};

const clickTabls = (item: any) => {
    router.push(item.props.name);
};
const closeTabs = (path: string) => {
    const index = tabs.list.findIndex((item) => item.path === path);
    tabs.delTabsItem(index);
    const item = tabs.list[index] || tabs.list[index - 1];
    router.push(item ? item.path : '/');
};

watch(
    () => route.fullPath,
    (newVal, oldVal) => {
        activePath.value = newVal;
    }
);
</script>

<style scss>
.tabs-container {
    position: relative;
    overflow: hidden;
    background: var(--sl-surface);
    border-bottom: 1px solid var(--sl-border);
    padding: 6px 120px 0 16px;
}

.tabs {
    .el-tabs__header {
        margin-bottom: 0;
        border-bottom: none;
    }

    .el-tabs__nav {
        height: 30px;
        border: none !important;
    }

    .el-tabs__item {
        height: 30px;
        line-height: 30px;
        border: none !important;
        border-radius: 6px 6px 0 0;
        color: var(--sl-text-secondary);
        font-size: var(--sl-fs-sm);
    }

    .el-tabs__item.is-active {
        color: var(--sl-primary);
        background: #eff6ff;
    }

    .el-tabs__nav-next,
    .el-tabs__nav-prev {
        line-height: 32px;
    }

    &.el-tabs {
        --el-tabs-header-height: 30px;
    }
}

.Tabs-close-box {
    position: absolute;
    right: 0;
    top: 0;
    box-sizing: border-box;
    padding-top: 5px;
    text-align: center;
    width: 110px;
    height: 36px;
    background: var(--sl-surface);
    box-shadow: -3px 0 12px 3px rgba(0, 0, 0, 0.04);
    z-index: 10;
}
</style>
