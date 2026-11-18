<template>
    <div class="page sl-page">
        <el-skeleton v-if="loading" :rows="8" animated />

        <template v-else>
            <!-- 顶部四个关键指标 -->
            <div class="sl-grid">
                <el-card v-for="item in statCards" :key="item.label" class="stat-card sl-col-3" shadow="never">
                    <div class="stat-top" :style="{ color: item.color }">
                        <el-icon><component :is="item.icon" /></el-icon>
                        <span>{{ item.label }}</span>
                    </div>
                    <div class="stat-value">
                        {{ item.value }}<span class="unit">{{ item.unit }}</span>
                    </div>
                    <div class="stat-hint">{{ item.hint }}</div>
                </el-card>
            </div>

            <div class="sl-grid chart-grid">
                <!-- 覆盖率 -->
                <el-card class="sl-col-5" shadow="never">
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

                    <div class="hint">练过至少 2 次、正确率 80% 以上，才算掌握；只做过一次全对不算。</div>
                </el-card>

                <!-- 复习趋势 -->
                <el-card class="sl-col-7" shadow="never">
                    <template #header>
                        <div class="card-header"><span>最近 30 天趋势</span></div>
                    </template>
                    <base-chart :option="trendOption" height="260px" />
                </el-card>
            </div>

            <!-- 还不熟的知识点 -->
            <el-card class="block" shadow="never">
                <template #header>
                    <div class="card-header">
                        <span>还不熟的知识点</span>
                        <el-tag v-if="weakKnowledge.length" type="warning" size="small">
                            {{ weakKnowledge.length }} 个知识点
                        </el-tag>
                    </div>
                </template>

                <el-empty v-if="weakKnowledge.length === 0" description="暂时没有不熟的知识点，继续保持" :image-size="80" />
                <base-chart v-else :option="weakOption" :height="weakHeight" />
            </el-card>

            <!-- 记住了但不会用 -->
            <el-card v-if="transferList.length" class="block" shadow="never">
                <template #header>
                    <div class="card-header">
                        <span>记住了，但不会用</span>
                        <el-tag type="warning" size="small">{{ transferList.length }} 个知识点</el-tag>
                    </div>
                </template>

                <div class="transfer-list">
                    <div v-for="item in transferList" :key="item.knowledgeId" class="transfer-item">
                        <b>#{{ item.knowledgeId }}</b>
                        <span class="hint">
                            单点 {{ percent(item.surfaceAccuracy) }} · 综合 {{ percent(item.deepAccuracy) }}
                        </span>
                        <el-tag type="warning" size="small" effect="plain">换个问法就不会</el-tag>
                    </div>
                </div>
                <div class="hint">
                    单个知识点能答对，放进综合题就答不出来。这类知识点不会拉长复习间隔，下次会优先给综合题。
                </div>
            </el-card>
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
        hint: '已录入、可以拿来练的题',
        color: '#2563eb',
        icon: 'Document'
    },
    {
        label: '今天该复习',
        value: overview.value.dueToday,
        unit: ' 个知识点',
        hint: '到了复习时间，建议今天过一遍',
        color: '#f59e0b',
        icon: 'AlarmClock'
    },
    {
        label: '已经掌握',
        value: overview.value.masteredQuestions,
        unit: ' 个知识点',
        hint: '练过至少 2 次，正确率 80% 以上',
        color: '#10b981',
        icon: 'CircleCheck'
    },
    {
        label: '连续复习',
        value: overview.value.streakDays,
        unit: ' 天',
        hint: '每天练一次就能保持',
        color: '#8b5cf6',
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
    legend: { data: ['复习次数', '正确率'], left: 0, top: 0, itemWidth: 12, textStyle: { fontSize: 12 } },
    grid: { left: 8, right: 16, top: 40, bottom: 8, containLabel: true },
    xAxis: {
        type: 'category',
        data: trend.value.map((t) => t.statDate?.slice(5) ?? ''),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    yAxis: [
        {
            type: 'value',
            splitLine: { lineStyle: { color: '#f3f4f6' } },
            axisLabel: { color: '#9ca3af', fontSize: 11 }
        },
        {
            type: 'value',
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
                showBackground: true,
                backgroundStyle: { color: '#f3f4f6', borderRadius: [0, 4, 4, 0] },
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

<style scoped>
.stat-card {
    position: relative;
    overflow: hidden;
}
.stat-card :deep(.el-card__body) {
    padding: var(--sl-s5);
}
.stat-top {
    display: flex;
    align-items: center;
    gap: var(--sl-s2);
    font-size: var(--sl-fs-sm);
    font-weight: 500;
}
.stat-value {
    margin-top: var(--sl-s3);
    font-size: 30px;
    font-weight: 700;
    color: var(--sl-text);
    font-variant-numeric: tabular-nums;
    line-height: 1.1;
}
.unit {
    margin-left: 4px;
    font-size: var(--sl-fs-sm);
    font-weight: 400;
    color: var(--sl-muted);
}
.stat-hint {
    margin-top: var(--sl-s2);
    font-size: var(--sl-fs-xs);
    color: var(--sl-muted);
    line-height: 1.6;
}

/* 旧结构保留，避免其他页面引用时样式丢失 */
.stat-accent {
    width: 4px;
    height: 44px;
    border-radius: 4px;
}
.stat-accent {
    width: 4px;
    height: 44px;
    border-radius: 4px;
}
.chart-grid {
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
.transfer-list {
    display: flex;
    flex-direction: column;
    gap: var(--sl-s3);
    margin-bottom: var(--sl-s3);
}
.transfer-item {
    display: flex;
    align-items: center;
    gap: var(--sl-s3);
    padding: 10px 12px;
    border: 1px solid var(--sl-border);
    border-radius: var(--sl-radius-sm);
    background: #fffbeb;
}
.transfer-item b {
    color: var(--sl-text);
}
.hint {
    margin-top: var(--sl-s3);
}

@media (max-width: 1100px) {
        .chart-grid {
    margin-top: var(--sl-s4);
}
}
</style>
