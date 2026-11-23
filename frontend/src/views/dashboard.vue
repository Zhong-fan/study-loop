<template>
    <div class="page">
        <el-card v-if="loading" shadow="never">
            <el-skeleton :rows="4" animated />
        </el-card>

        <el-card v-else-if="!questions.length" shadow="never">
            <el-empty :description="pending.length > 0
                ? `有 ${pending.length} 个知识点到期，但还没有题目`
                : '今天没有需要复习的题目'">
                <el-button v-if="pending.length > 0" type="primary" @click="router.push('/ai')">去 AI 出题</el-button>
                <el-button @click="load">刷新</el-button>
            </el-empty>
        </el-card>

        <el-card v-else shadow="never" class="summary">
            <template #header>
                <div class="card-header">
                    <span>今日复习</span>
                    <el-button size="small" @click="load">刷新</el-button>
                </div>
            </template>

            <div class="count-row">
                <div class="count">
                    <b>{{ questions.length }}</b>
                    <span>道待复习</span>
                </div>
                <div class="split"></div>
                <div class="count">
                    <b>{{ pending.length }}</b>
                    <span>个知识点到期</span>
                </div>
            </div>

            <el-button class="start" type="primary" size="large" @click="router.push('/practice')">
                开始复习
            </el-button>

            <p class="hint">按 1 / 3 / 7 / 14 天的间隔安排；做完自动更新掌握度与下次复习时间。</p>
        </el-card>

        <div class="links">
            <button class="link-card" @click="router.push('/questions')">
                <span class="link-title">题库</span>
                <span class="link-sub">查看和编辑所有题目</span>
            </button>
            <button class="link-card" @click="router.push('/stats')">
                <span class="link-title">统计</span>
                <span class="link-sub">覆盖率、掌握度与盲区</span>
            </button>
            <button class="link-card" @click="router.push('/ai')">
                <span class="link-title">AI 出题</span>
                <span class="link-sub">按知识点补题</span>
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getDueQuestions } from '@/api';

const router = useRouter();
const loading = ref(true);
const questions = ref<any[]>([]);
const pending = ref<number[]>([]);

const load = async () => {
    loading.value = true;
    try {
        const data = await getDueQuestions();
        questions.value = data.questions || [];
        pending.value = data.pendingKnowledgeIds || [];
    } finally {
        loading.value = false;
    }
};

onMounted(load);
</script>

<style scoped>
.page {
    padding: 24px;
    max-width: 880px;
    margin: 0 auto;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.summary {
    border-radius: 14px;
}

.count-row {
    display: flex;
    align-items: center;
    gap: 32px;
    padding: 8px 0 24px;
}
.split {
    width: 1px;
    height: 48px;
    background: #eef2f7;
}
.count {
    display: flex;
    flex-direction: column;
    gap: 2px;
}
.count b {
    font-size: 36px;
    line-height: 1.1;
    color: #0f172a;
    font-variant-numeric: tabular-nums;
}
.count span {
    font-size: 13px;
    color: #64748b;
}

.start {
    min-width: 160px;
    height: 46px;
    border-radius: 10px;
    font-weight: 600;
}

.hint {
    margin: 16px 0 0;
    font-size: 12px;
    color: #94a3b8;
}

.links {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    margin-top: 16px;
}
.link-card {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 18px;
    text-align: left;
    background: #fff;
    border: 1px solid #eef2f7;
    border-radius: 12px;
    cursor: pointer;
    transition: border-color 0.15s ease, transform 0.15s ease;
}
.link-card:hover {
    border-color: #bfdbfe;
    transform: translateY(-1px);
}
.link-title {
    font-size: 14px;
    font-weight: 600;
    color: #0f172a;
}
.link-sub {
    font-size: 12px;
    color: #94a3b8;
}

@media (max-width: 640px) {
    .page {
        padding: 16px;
    }
    .count-row {
        gap: 20px;
    }
    .count b {
        font-size: 30px;
    }
    .links {
        grid-template-columns: 1fr;
    }
}
</style>
