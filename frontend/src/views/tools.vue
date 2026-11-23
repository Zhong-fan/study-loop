<template>
    <div class="tools">
        <div class="head">
            <h1>资料与工具</h1>
            <p>上传资料、维护题库、出题与查看完整统计都在这里；日常复习只需要「概览」和「做题」。</p>
        </div>

        <template v-for="group in groups" :key="group.title">
            <div class="group-title">{{ group.title }}</div>
            <div class="grid">
                <button
                    v-for="item in group.items"
                    :key="item.path"
                    type="button"
                    class="tool-card"
                    @click="router.push(item.path)"
                >
                    <span class="tool-icon">
                        <el-icon><component :is="item.icon" /></el-icon>
                    </span>
                    <span class="tool-text">
                        <b>{{ item.title }}</b>
                        <span>{{ item.desc }}</span>
                    </span>
                    <el-icon class="arrow"><ArrowRight /></el-icon>
                </button>
            </div>
        </template>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight } from '@element-plus/icons-vue';

const router = useRouter();

const stored = JSON.parse(localStorage.getItem('user') || '{}');
const isAdmin = stored.role === 'ADMIN';

const groups = computed(() => {
    const list = [
        {
            title: '资料与题目',
            items: [
                { path: '/courses', title: '课程与资料', desc: '上传 PDF / 图片，解析并抽取知识点', icon: 'Notebook' },
                { path: '/questions', title: '题库', desc: '查看、编辑和删除题目', icon: 'EditPen' },
                { path: '/exams', title: '真题', desc: '导入历年真题，统计考点分布', icon: 'Files' }
            ]
        },
        {
            title: '练习与预测',
            items: [
                { path: '/quiz', title: '自测', desc: '按课程生成一份试卷，限时作答', icon: 'Tickets' },
                { path: '/ai', title: 'AI 出题', desc: '按知识点生成题目草稿并入库', icon: 'MagicStick' },
                { path: '/predict', title: '预测卷', desc: '按权重和真题频率生成预测卷', icon: 'Aim' }
            ]
        },
        {
            title: '数据',
            items: [{ path: '/stats', title: '统计', desc: '覆盖率、掌握度、薄弱知识点与趋势', icon: 'PieChart' }]
        }
    ];

    if (isAdmin) {
        list.push({
            title: '系统',
            items: [
                { path: '/admin', title: '用户管理', desc: '账号状态与 AI 配置', icon: 'User' }
            ]
        });
    }
    return list;
});
</script>

<style scoped>
.tools {
    max-width: 900px;
    margin: 0 auto;
    padding: 28px 24px 56px;
}
.head h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: #0f172a;
}
.head p {
    margin: 8px 0 28px;
    font-size: 13px;
    line-height: 1.7;
    color: #64748b;
}

.group-title {
    margin: 22px 0 10px;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.4px;
    color: #94a3b8;
}

.grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
}
.tool-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px;
    text-align: left;
    background: #fff;
    border: 1px solid #eef2f7;
    border-radius: 12px;
    cursor: pointer;
    transition: border-color 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease;
}
.tool-card:hover {
    border-color: #bfdbfe;
    transform: translateY(-1px);
    box-shadow: 0 8px 24px -18px rgba(15, 23, 42, 0.35);
}
.tool-icon {
    flex: none;
    width: 38px;
    height: 38px;
    display: grid;
    place-items: center;
    font-size: 18px;
    color: #2563eb;
    background: #eff6ff;
    border-radius: 10px;
}
.tool-text {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
}
.tool-text b {
    font-size: 14px;
    color: #0f172a;
}
.tool-text span {
    font-size: 12px;
    color: #94a3b8;
}
.arrow {
    margin-left: auto;
    color: #cbd5e1;
}

@media (max-width: 900px) {
    .grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}
@media (max-width: 640px) {
    .tools {
        padding: 18px 14px 40px;
    }
    .grid {
        grid-template-columns: 1fr;
    }
}
</style>
