<template>
    <div class="mx-auto max-w-6xl">
        <h1 class="mb-4 text-2xl font-semibold tracking-tight text-ink-strong">统计</h1>

        <div v-if="loading" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div v-for="i in 4" :key="i" class="h-24 animate-pulse rounded-xl border border-line bg-surface"></div>
        </div>

        <template v-else>
            <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <div v-for="item in statCards" :key="item.label" class="rounded-xl border border-line bg-surface p-4">
                    <div class="flex items-center gap-2 text-xs text-ink-muted">
                        <span class="size-1.5 rounded-full" :style="{ background: item.color }"></span>
                        <span>{{ item.label }}</span>
                    </div>
                    <div class="mt-3 text-2xl font-semibold text-ink-strong">
                        {{ item.value }}<span class="ml-1 text-xs font-normal text-ink-muted">{{ item.unit }}</span>
                    </div>
                </div>
            </div>

            <div class="mt-4 grid gap-4 lg:grid-cols-12">
                <section class="rounded-xl border border-line bg-surface p-4 lg:col-span-5">
                    <div class="mb-3 flex items-center justify-between gap-2">
                        <h2 class="text-sm font-medium text-ink">知识点覆盖率</h2>
                        <el-select v-model="courseId" placeholder="选择课程" size="small" style="width: 170px" @change="loadCoverage">
                            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                        </el-select>
                    </div>
                    <div class="flex items-center gap-4">
                        <div class="min-w-0 flex-1">
                            <base-chart :option="coverageOption" height="200px" />
                        </div>
                        <div class="w-36 shrink-0 space-y-2 text-xs">
                            <div class="flex items-center gap-2">
                                <span class="size-2 rounded-full bg-[#4c6ef5]"></span><span class="flex-1">已掌握</span><b>{{ coverage.mastered }}</b>
                            </div>
                            <div class="flex items-center gap-2">
                                <span class="size-2 rounded-full bg-[#dbe4ff]"></span><span class="flex-1">未掌握</span>
                                <b>{{ Math.max(coverage.totalKnowledge - coverage.mastered, 0) }}</b>
                            </div>
                            <div class="flex items-center gap-2">
                                <span class="size-2 rounded-full bg-[#12b886]"></span><span class="flex-1">练过</span><b>{{ coverage.practiced }}</b>
                            </div>
                            <div class="flex items-center gap-2">
                                <span class="size-2 rounded-full bg-[#f59f00]"></span><span class="flex-1">总数</span><b>{{ coverage.totalKnowledge }}</b>
                            </div>
                        </div>
                    </div>
                    <p class="mt-3 text-xs text-ink-muted">练过至少 2 次、正确率 80% 以上才算掌握。</p>
                </section>

                <section class="rounded-xl border border-line bg-surface p-4 lg:col-span-7">
                    <h2 class="mb-3 text-sm font-medium text-ink">最近 30 天趋势</h2>
                    <base-chart :option="trendOption" height="240px" />
                </section>
            </div>

            <section class="mt-4 rounded-xl border border-line bg-surface p-4">
                <div class="mb-3 flex items-center justify-between">
                    <h2 class="text-sm font-medium text-ink">还不熟的知识点</h2>
                    <span v-if="weakKnowledge.length" class="text-xs text-warn">{{ weakKnowledge.length }} 个</span>
                </div>
                <div v-if="weakKnowledge.length === 0" class="py-10 text-center text-sm text-ink-muted">
                    暂时没有不熟的知识点。
                </div>
                <base-chart v-else :option="weakOption" :height="weakHeight" />
            </section>

            <section v-if="transferList.length" class="mt-4 rounded-xl border border-line bg-surface p-4">
                <div class="mb-3 flex items-center justify-between">
                    <h2 class="text-sm font-medium text-ink">记住了，但不会用</h2>
                    <span class="text-xs text-warn">{{ transferList.length }} 个</span>
                </div>
                <div class="space-y-2">
                    <div v-for="item in transferList" :key="item.knowledgeId" class="flex flex-wrap items-center gap-3 text-sm">
                        <b class="text-ink">#{{ item.knowledgeId }}</b>
                        <span class="text-xs text-ink-muted">
                            单点 {{ percent(item.surfaceAccuracy) }} · 综合 {{ percent(item.deepAccuracy) }}
                        </span>
                        <span class="rounded-full bg-warn-soft px-2 py-0.5 text-xs text-warn">换个问法就不会</span>
                    </div>
                </div>
                <p class="mt-3 text-xs text-ink-muted">这类知识点不会拉长复习间隔，下次优先给综合题。</p>
            </section>
        </template>
    </div>
</template>


<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getCoverage, getCourses, getOverview, getTransfer, getTrend, getWeakKnowledge } from '@/api';
import BaseChart from '@/components/base-chart.vue';

const loading = ref(true);

const overview = ref<any>({
    totalQuestions: 0,
    dueToday: 0,
    masteredQuestions: 0,
    streakDays: 0,
    reviewedToday: 0,
    totalReviews: 0
});
const coverage = ref<any>({ totalKnowledge: 0, practiced: 0, mastered: 0, coverage: 0 });
const courses = ref<any[]>([]);
const courseId = ref<number | null>(null);
const weakKnowledge = ref<any[]>([]);
const transferList = ref<any[]>([]);
const trend = ref<any[]>([]);

const percent = (value: number | null | undefined) =>
    value === null || value === undefined ? '—' : `${Math.round(value * 100)}%`;

const statCards = computed(() => [
    {
        label: '题库里的题目',
        value: overview.value.totalQuestions,
        unit: ' 道',
        color: '#4c6ef5',
        icon: 'Document'
    },
    {
        label: '今天该复习',
        value: overview.value.dueToday,
        unit: ' 个知识点',
        color: '#f59f00',
        icon: 'AlarmClock'
    },
    {
        label: '已经掌握',
        value: overview.value.masteredQuestions,
        unit: ' 个知识点',
        color: '#12b886',
        icon: 'CircleCheck'
    },
    {
        label: '连续复习',
        value: overview.value.streakDays,
        unit: ' 天',
        color: '#6b7280',
        icon: 'Calendar'
    }
]);

const coverageOption = computed(() => {
    const total = coverage.value.totalKnowledge;
    const mastered = coverage.value.mastered;
    const rest = Math.max(total - mastered, 0);

    return {
        tooltip: { trigger: 'item', formatter: '{b}: {c}' },
        series: [
            {
                type: 'pie',
                radius: ['62%', '82%'],
                avoidLabelOverlap: false,
                itemStyle: { borderRadius: 6, borderColor: '#ffffff', borderWidth: 2 },
                label: {
                    show: true,
                    position: 'center',
                    formatter: total === 0 ? '暂无数据' : `${Math.round((mastered / total) * 100)}%`,
                    fontSize: 22,
                    fontWeight: 700,
                    color: '#1f2937'
                },
                emphasis: { label: { show: true } },
                data: [
                    { value: mastered, name: '已掌握', itemStyle: { color: '#4c6ef5' } },
                    { value: rest, name: '未掌握', itemStyle: { color: '#dbe4ff' } }
                ]
            }
        ]
    };
});

const trendOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    legend: { data: ['复习次数', '正确率'], left: 0, top: 0, itemWidth: 12, textStyle: { fontSize: 12 } },
    grid: { left: 8, right: 16, top: 40, bottom: 8, containLabel: true },
    xAxis: {
        type: 'category',
        data: trend.value.map((t) => t.statDate?.slice(5) ?? ''),
        axisLine: { lineStyle: { color: '#dbe4ff' } },
        axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    yAxis: [
        {
            type: 'value',
            splitLine: { lineStyle: { color: '#eef0f4' } },
            axisLabel: { color: '#6b7280', fontSize: 11 }
        },
        {
            type: 'value',
            min: 0,
            max: 1,
            axisLabel: {
                formatter: (v: number) => `${Math.round(v * 100)}%`,
                color: '#6b7280',
                fontSize: 11
            },
            splitLine: { show: false }
        }
    ],
    series: [
        {
            name: '复习次数',
            type: 'bar',
            barMaxWidth: 18,
            itemStyle: { color: '#dbe4ff', borderRadius: [4, 4, 0, 0] },
            data: trend.value.map((t) => t.reviewCount)
        },
        {
            name: '正确率',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { width: 2, color: '#4c6ef5' },
            itemStyle: { color: '#4c6ef5' },
            data: trend.value.map((t) => t.accuracy ?? 0)
        }
    ]
}));

const weakOption = computed(() => {
    const rows = [...weakKnowledge.value].slice(0, 8).reverse();
    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: (params: any[]) => {
                const p = params[0];
                return `知识点 #${p.name}<br/>正确率 ${Math.round(p.value * 100)}%`;
            }
        },
        grid: { left: 8, right: 40, top: 8, bottom: 8, containLabel: true },
        xAxis: {
            type: 'value',
            min: 0,
            max: 1,
            axisLabel: {
                formatter: (v: number) => `${Math.round(v * 100)}%`,
                color: '#6b7280',
                fontSize: 11
            },
            splitLine: { lineStyle: { color: '#eef0f4' } }
        },
        yAxis: {
            type: 'category',
            data: rows.map((r) => `#${r.knowledgeId}`),
            axisLine: { show: false },
            axisTick: { show: false },
            axisLabel: { color: '#374151', fontSize: 12 }
        },
        series: [
            {
                type: 'bar',
                barMaxWidth: 16,
                showBackground: true,
                backgroundStyle: { color: '#eef0f4', borderRadius: [0, 4, 4, 0] },
                itemStyle: { color: '#ffa8a8', borderRadius: [0, 4, 4, 0] },
                label: {
                    show: true,
                    position: 'right',
                    formatter: (p: any) => `${Math.round(p.value * 100)}%`,
                    color: '#6b7280',
                    fontSize: 11
                },
                data: rows.map((r) => r.accuracy)
            }
        ]
    };
});

const weakHeight = computed(() => `${Math.max(weakKnowledge.value.length, 3) * 36 + 40}px`);

const loadCoverage = async () => {
    if (!courseId.value) {
        return;
    }
    coverage.value = await getCoverage(courseId.value);
};

onMounted(async () => {
    try {
        const [o, t, w, tr, data] = await Promise.all([
            getOverview(),
            getTrend(30),
            getWeakKnowledge(),
            getTransfer(),
            getCourses({ page: 1, size: 100 })
        ]);
        overview.value = o;
        trend.value = t;
        weakKnowledge.value = w;
        transferList.value = tr;
        courses.value = data.records;

        if (courses.value.length > 0) {
            courseId.value = courses.value[0].id;
            await loadCoverage();
        }
    } finally {
        loading.value = false;
    }
});
</script>
