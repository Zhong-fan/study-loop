<template>
    <div class="dash">
        <el-skeleton v-if="loading" :rows="6" animated />

        <template v-else>
            <section class="hero">
                <div class="hero-text">
                    <p class="eyebrow">{{ today }}</p>
                    <h1 v-if="dueCount">今天还有 <b>{{ dueCount }}</b> 道题待复习</h1>
                    <h1 v-else>今天的复习已经完成</h1>
                    <p class="sub">
                        <template v-if="dueCount">涉及 {{ pendingCount }} 个到期知识点，按 1 / 3 / 7 / 14 天节奏安排。</template>
                        <template v-else>可以再做一组自测，或者去补一些新题。</template>
                    </p>
                    <div class="hero-actions">
                        <el-button v-if="dueCount" type="primary" size="large" @click="router.push('/practice')">
                            开始复习
                        </el-button>
                        <el-button v-else type="primary" size="large" @click="router.push('/quiz')">
                            做一组自测
                        </el-button>
                        <el-button size="large" @click="router.push('/tools')">资料与工具</el-button>
                    </div>
                </div>

                <div class="streak">
                    <span>连续复习</span>
                    <b>{{ overview.streakDays }}</b>
                    <span>天</span>
                </div>
            </section>

            <section class="kpis">
                <div class="kpi">
                    <span class="kpi-label">已掌握知识点</span>
                    <b>{{ overview.masteredQuestions }}</b>
                </div>
                <div class="kpi">
                    <span class="kpi-label">今天已复习</span>
                    <b>{{ overview.reviewedToday }}</b>
                </div>
                <div class="kpi">
                    <span class="kpi-label">题库题量</span>
                    <b>{{ overview.totalQuestions }}</b>
                </div>
                <div class="kpi">
                    <span class="kpi-label">待复习知识点</span>
                    <b>{{ overview.dueToday }}</b>
                </div>
            </section>

            <el-card shadow="never" class="trend-card">
                <template #header>
                    <div class="card-head">
                        <span>最近 14 天</span>
                        <span class="muted">复习次数与正确率</span>
                    </div>
                </template>
                <base-chart v-if="trend.length" :option="trendOption" height="220px" />
                <el-empty v-else description="还没有复习记录" :image-size="70" />
            </el-card>
        </template>
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
    grid: { left: 8, right: 12, top: 28, bottom: 4, containLabel: true },
    xAxis: {
        type: 'category',
        data: trend.value.map((t: any) => t.statDate?.slice(5) ?? ''),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#9ca3af', fontSize: 11 }
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
            axisLabel: { formatter: (v: number) => `${Math.round(v * 100)}%`, color: '#9ca3af', fontSize: 11 },
            splitLine: { show: false }
        }
    ],
    series: [
        {
            name: '复习次数',
            type: 'bar',
            barMaxWidth: 18,
            itemStyle: { color: '#bfdbfe', borderRadius: [4, 4, 0, 0] },
            data: trend.value.map((t: any) => t.reviewCount)
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

<style scoped>
.dash {
    max-width: 900px;
    margin: 0 auto;
    padding: 24px 24px 56px;
}

.hero {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24px;
    padding: 26px 28px;
    background: #fff;
    border: 1px solid #eef2f7;
    border-radius: 16px;
}
.eyebrow {
    margin: 0 0 8px;
    font-size: 12px;
    color: #94a3b8;
}
.hero h1 {
    margin: 0;
    font-size: 26px;
    line-height: 1.35;
    font-weight: 700;
    color: #0f172a;
}
.hero h1 b {
    color: #2563eb;
    font-variant-numeric: tabular-nums;
}
.sub {
    margin: 10px 0 20px;
    font-size: 13px;
    line-height: 1.7;
    color: #64748b;
}
.hero-actions {
    display: flex;
    gap: 10px;
}
.hero-actions :deep(.el-button) {
    border-radius: 10px;
}

.streak {
    flex: none;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 2px;
    padding: 14px 18px;
    background: #f8fafc;
    border: 1px solid #eef2f7;
    border-radius: 12px;
}
.streak span {
    font-size: 12px;
    color: #94a3b8;
}
.streak b {
    font-size: 30px;
    line-height: 1.1;
    color: #0f172a;
    font-variant-numeric: tabular-nums;
}

.kpis {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
    margin-top: 14px;
}
.kpi {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 16px 18px;
    background: #fff;
    border: 1px solid #eef2f7;
    border-radius: 12px;
}
.kpi-label {
    font-size: 12px;
    color: #94a3b8;
}
.kpi b {
    font-size: 24px;
    line-height: 1.2;
    color: #0f172a;
    font-variant-numeric: tabular-nums;
}

.trend-card {
    margin-top: 14px;
    border-radius: 14px;
}
.card-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}
.muted {
    font-size: 12px;
    color: #94a3b8;
}

@media (max-width: 720px) {
    .dash {
        padding: 16px 14px 40px;
    }
    .hero {
        flex-direction: column;
        align-items: stretch;
        padding: 20px;
    }
    .streak {
        align-items: flex-start;
    }
    .kpis {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}
</style>
