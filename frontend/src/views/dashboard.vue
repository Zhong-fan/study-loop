<template>
    <div class="mx-auto max-w-5xl">
        <div class="mb-5 flex items-end justify-between gap-4">
            <div>
                <p class="text-xs text-ink-muted">{{ today }}</p>
                <h1 class="mt-1 text-2xl font-semibold tracking-tight text-ink-strong">概览</h1>
            </div>
            <span class="text-xs text-ink-muted">待复习 {{ dueCount }} 题</span>
        </div>

        <section class="rounded-xl border border-line bg-surface p-6">
            <p class="text-sm text-ink-muted">今天</p>
            <h2 class="mt-2 text-3xl font-semibold tracking-tight text-ink-strong">
                还有 {{ dueCount }} 道题
            </h2>
            <p v-if="pendingCount" class="mt-2 text-sm text-ink-muted">{{ pendingCount }} 个知识点到期。</p>
            <div class="mt-5 flex items-center gap-3">
                <RouterLink
                    to="/practice"
                    class="inline-flex h-9 items-center rounded-md bg-brand px-4 text-sm font-medium text-white no-underline hover:opacity-90"
                >
                    开始复习
                </RouterLink>
                <RouterLink
                    to="/tools"
                    class="inline-flex h-9 items-center rounded-md border border-line px-4 text-sm text-ink no-underline hover:bg-surface-2"
                >
                    资料与工具
                </RouterLink>
            </div>
        </section>

        <section class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div class="rounded-xl border border-line bg-surface p-4">
                <p class="text-xs text-ink-muted">已掌握</p>
                <p class="mt-2 text-2xl font-semibold text-ink-strong">{{ overview.masteredQuestions }}</p>
                <p class="text-xs text-ink-subtle">个知识点</p>
            </div>
            <div class="rounded-xl border border-line bg-surface p-4">
                <p class="text-xs text-ink-muted">今天已复习</p>
                <p class="mt-2 text-2xl font-semibold text-ink-strong">{{ overview.reviewedToday }}</p>
                <p class="text-xs text-ink-subtle">道题</p>
            </div>
            <div class="rounded-xl border border-line bg-surface p-4">
                <p class="text-xs text-ink-muted">题库题量</p>
                <p class="mt-2 text-2xl font-semibold text-ink-strong">{{ overview.totalQuestions }}</p>
                <p class="text-xs text-ink-subtle">道</p>
            </div>
            <div class="rounded-xl border border-line bg-surface p-4">
                <p class="text-xs text-ink-muted">连续复习</p>
                <p class="mt-2 text-2xl font-semibold text-ink-strong">{{ overview.streakDays }}</p>
                <p class="text-xs text-ink-subtle">天</p>
            </div>
        </section>

        <section class="mt-4 rounded-xl border border-line bg-surface p-5">
            <div class="mb-3 flex items-center justify-between">
                <h2 class="text-sm font-medium text-ink">最近 14 天</h2>
                <span class="text-xs text-ink-muted">复习次数 / 正确率</span>
            </div>
            <BaseChart v-if="trend.length" :option="trendOption" height="240px" />
            <p v-else class="py-10 text-center text-sm text-ink-muted">还没有复习记录。</p>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getDueQuestions, getOverview, getTrend } from '@/api';
import BaseChart from '@/components/base-chart.vue';

const router = useRouter();
const loading = ref(true);
const questions = ref<any[]>([]);
const pending = ref<number[]>([]);
const trend = ref<any[]>([]);
const overview = ref<any>({
    totalQuestions: 0,
    dueToday: 0,
    masteredQuestions: 0,
    streakDays: 0,
    reviewedToday: 0,
    totalReviews: 0
});

const dueCount = computed(() => questions.value.length);
const pendingCount = computed(() => pending.value.length);

const today = new Date().toLocaleDateString('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
});

const trendOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    grid: { left: 4, right: 8, top: 30, bottom: 4, containLabel: true },
    xAxis: {
        type: 'category',
        data: trend.value.map((t: any) => t.statDate?.slice(5) ?? ''),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisTick: { show: false },
        axisLabel: { color: '#6b7280', fontSize: 10, fontFamily: 'ui-monospace, monospace' }
    },
    yAxis: [
        {
            type: 'value',
            splitLine: { lineStyle: { color: '#eef0f4', type: 'dotted' } },
            axisLabel: { color: '#6b7280', fontSize: 10 }
        },
        {
            type: 'value',
            min: 0,
            max: 1,
            axisLabel: { formatter: (v: number) => `${Math.round(v * 100)}%`, color: '#6b7280', fontSize: 10 },
            splitLine: { show: false }
        }
    ],
    series: [
        {
            name: '复习次数',
            type: 'bar',
            barMaxWidth: 14,
            itemStyle: { color: '#dbe4ff' },
            data: trend.value.map((t: any) => t.reviewCount)
        },
        {
            name: '正确率',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            symbol: 'circle',
            symbolSize: 5,
            lineStyle: { width: 1.5, color: '#4c6ef5' },
            itemStyle: { color: '#4c6ef5' },
            data: trend.value.map((t: any) => t.accuracy ?? 0)
        }
    ]
}));

const load = async () => {
    loading.value = true;
    try {
        const [due, stats, trendData] = await Promise.all([
            getDueQuestions(),
            getOverview(),
            getTrend(14)
        ]);
        questions.value = due.questions || [];
        pending.value = due.pendingKnowledgeIds || [];
        overview.value = stats || overview.value;
        trend.value = trendData || [];
    } finally {
        loading.value = false;
    }
};

onMounted(load);
</script>

