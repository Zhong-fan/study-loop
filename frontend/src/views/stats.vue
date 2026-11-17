<template>
    <div class="page">
        <el-skeleton v-if="loading" :rows="8" animated />

        <template v-else>
            <!-- 顶部四个关键指标 -->
            <div class="stat-grid">
                <el-card v-for="item in statCards" :key="item.label" class="stat-card" shadow="never">
                    <div class="stat-accent" :style="{ background: item.color }"></div>
                    <div class="stat-body">
                        <div class="num">{{ item.value }}</div>
                        <div class="label">{{ item.label }}</div>
                    </div>
                </el-card>
            </div>

            <div class="chart-grid">
                <!-- 覆盖率 -->
                <el-card shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span>知识点覆盖率</span>
                            <el-select
                                v-model="courseId"
                                placeholder="选择课程"
                                size="small"
                                style="width: 180px"
                                @change="loadCoverage"
                            >
                                <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                            </el-select>
                        </div>
                    </template>

                    <div class="coverage">
                        <base-chart :option="coverageOption" height="220px" />
                        <div class="coverage-legend">
                            <div class="legend-row">
                                <span class="dot" style="background: #2563eb"></span>
                                <span>已掌握</span><b>{{ coverage.mastered }}</b>
                            </div>
                            <div class="legend-row">
                                <span class="dot" style="background: #e5e7eb"></span>
                                <span>未掌握</span>
                                <b>{{ Math.max(coverage.totalKnowledge - coverage.mastered, 0) }}</b>
                            </div>
                            <div class="legend-row">
                                <span class="dot" style="background: #10b981"></span>
                                <span>练过</span><b>{{ coverage.practiced }}</b>
                            </div>
                            <div class="legend-row">
                                <span class="dot" style="background: #f59e0b"></span>
                                <span>知识点总数</span><b>{{ coverage.totalKnowledge }}</b>
                            </div>
                        </div>
                    </div>

                    <div class="hint">掌握 = 正确率 ≥ 80% 且至少练过 2 次；只练一次全对不算。</div>
                </el-card>

                <!-- 复习趋势 -->
                <el-card shadow="never">
                    <template #header>
                        <div class="card-header"><span>最近 30 天趋势</span></div>
                    </template>
                    <base-chart :option="trendOption" height="260px" />
                </el-card>
            </div>

            <!-- 盲区 -->
            <el-card class="block" shadow="never">
                <template #header>
                    <div class="card-header">
                        <span>盲区清单（正确率低于 60%）</span>
                        <el-tag v-if="weakKnowledge.length" type="warning" size="small">
                            {{ weakKnowledge.length }} 个知识点
                        </el-tag>
                    </div>
                </template>

                <el-empty v-if="weakKnowledge.length === 0" description="暂时没有盲区，继续保持" :image-size="80" />
                <base-chart v-else :option="weakOption" :height="weakHeight" />
            </el-card>
        </template>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getCoverage, getCourses, getOverview, getTrend, getWeakKnowledge } from '@/api';
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
const trend = ref<any[]>([]);

const statCards = computed(() => [
    { label: '题目总数', value: overview.value.totalQuestions, color: '#2563eb' },
    { label: '今日待复习知识点', value: overview.value.dueToday, color: '#f59e0b' },
    { label: '已掌握知识点', value: overview.value.masteredQuestions, color: '#10b981' },
    { label: '连续复习天数', value: overview.value.streakDays, color: '#8b5cf6' }
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
                itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
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
                    { value: mastered, name: '已掌握', itemStyle: { color: '#2563eb' } },
                    { value: rest, name: '未掌握', itemStyle: { color: '#e5e7eb' } }
                ]
            }
        ]
    };
});

const trendOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    legend: { data: ['复习次数', '正确率'], right: 0, top: 0, textStyle: { fontSize: 12 } },
    grid: { left: 8, right: 8, top: 40, bottom: 8, containLabel: true },
    xAxis: {
        type: 'category',
        data: trend.value.map((t) => t.statDate?.slice(5) ?? ''),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    yAxis: [
        {
            type: 'value',
            name: '次数',
            nameTextStyle: { color: '#9ca3af', fontSize: 11 },
            splitLine: { lineStyle: { color: '#f3f4f6' } },
            axisLabel: { color: '#9ca3af', fontSize: 11 }
        },
        {
            type: 'value',
            name: '正确率',
            min: 0,
            max: 1,
            axisLabel: {
                formatter: (v: number) => `${Math.round(v * 100)}%`,
                color: '#9ca3af',
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
            itemStyle: { color: '#bfdbfe', borderRadius: [4, 4, 0, 0] },
            data: trend.value.map((t) => t.reviewCount)
        },
        {
            name: '正确率',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { width: 2, color: '#2563eb' },
            itemStyle: { color: '#2563eb' },
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
                color: '#9ca3af',
                fontSize: 11
            },
            splitLine: { lineStyle: { color: '#f3f4f6' } }
        },
        yAxis: {
            type: 'category',
            data: rows.map((r) => `#${r.knowledgeId}`),
            axisLine: { show: false },
            axisTick: { show: false },
            axisLabel: { color: '#4b5563', fontSize: 12 }
        },
        series: [
            {
                type: 'bar',
                barMaxWidth: 16,
                itemStyle: { color: '#fca5a5', borderRadius: [0, 4, 4, 0] },
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
        const [o, t, w, data] = await Promise.all([
            getOverview(),
            getTrend(30),
            getWeakKnowledge(),
            getCourses({ page: 1, size: 100 })
        ]);
        overview.value = o;
        trend.value = t;
        weakKnowledge.value = w;
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

<style scoped>
.stat-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: var(--sl-s4);
}
.stat-card {
    position: relative;
    overflow: hidden;
}
.stat-card :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: var(--sl-s4);
    padding: var(--sl-s5);
}
.stat-accent {
    width: 4px;
    height: 44px;
    border-radius: 4px;
}
.chart-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr);
    gap: var(--sl-s4);
    margin-top: var(--sl-s4);
}
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.coverage {
    display: flex;
    align-items: center;
    gap: var(--sl-s4);
}
.coverage > :first-child {
    flex: 1;
    min-width: 0;
}
.coverage-legend {
    width: 140px;
    display: flex;
    flex-direction: column;
    gap: var(--sl-s2);
}
.legend-row {
    display: flex;
    align-items: center;
    gap: var(--sl-s2);
    font-size: var(--sl-fs-sm);
    color: var(--sl-text-secondary);
}
.legend-row b {
    margin-left: auto;
    color: var(--sl-text);
    font-variant-numeric: tabular-nums;
}
.dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}
.block {
    margin-top: var(--sl-s4);
}
.hint {
    margin-top: var(--sl-s3);
}

@media (max-width: 1100px) {
    .stat-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .chart-grid {
        grid-template-columns: minmax(0, 1fr);
    }
}
</style>
