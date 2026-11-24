<template>
    <div class="dash">
        <el-skeleton v-if="loading" :rows="6" animated />

        <template v-else>
            <header class="lede">
                <div class="lede-meta">
                    <span class="eyebrow">{{ today }}</span>
                    <span class="mono">Study log / {{ year }}</span>
                </div>

                <h1 v-if="dueCount" class="headline">
                    今天还有 <em>{{ dueCount }}</em> 道题<br />需要复习
                </h1>
                <h1 v-else class="headline">今天已经复习完了</h1>

                <p class="dek">
                    <template v-if="dueCount && pendingCount">
                        涉及 {{ pendingCount }} 个到期知识点。按 1 / 3 / 7 / 14 天的间隔推进，答错会回到起点。
                    </template>
                    <template v-else-if="dueCount">
                        按 1 / 3 / 7 / 14 天的间隔推进，答错会回到起点。
                    </template>
                    <template v-else>
                        没有到期的题。可以再做一组自测，或者去补一些新题。
                    </template>
                </p>

                <div class="lede-actions">
                    <button v-if="dueCount" type="button" class="cta" @click="router.push('/practice')">
                        开始复习 <span aria-hidden="true">→</span>
                    </button>
                    <button v-else type="button" class="cta" @click="router.push('/quiz')">
                        做一组自测 <span aria-hidden="true">→</span>
                    </button>
                    <button type="button" class="ghost" @click="router.push('/tools')">资料与工具</button>
                </div>
            </header>

            <section class="stats reveal">
                <div class="stat">
                    <span class="stat-label">已掌握</span>
                    <b>{{ overview.masteredQuestions }}</b>
                    <span class="stat-unit">个知识点</span>
                </div>
                <div class="stat">
                    <span class="stat-label">今天已复习</span>
                    <b>{{ overview.reviewedToday }}</b>
                    <span class="stat-unit">道题</span>
                </div>
                <div class="stat">
                    <span class="stat-label">题库题量</span>
                    <b>{{ overview.totalQuestions }}</b>
                    <span class="stat-unit">道</span>
                </div>
                <div class="stat">
                    <span class="stat-label">连续复习</span>
                    <b>{{ overview.streakDays }}</b>
                    <span class="stat-unit">天</span>
                </div>
            </section>

            <section class="trend">
                <div class="section-head">
                    <h2>最近 14 天</h2>
                    <span class="mono">复习次数 / 正确率</span>
                </div>
                <base-chart v-if="trend.length" :option="trendOption" height="240px" />
                <p v-else class="empty-note">还没有复习记录。做完第一组题，这里会长出第一条曲线。</p>
            </section>

            <aside class="rhythm">
                <span class="mono">Rhythm</span>
                <p>
                    答对一次，下次出现在 <b>1</b> 天后，然后 <b>3</b>、<b>7</b>、<b>14</b> 天；
                    答错就回到起点。间隔会随掌握度调整，不是固定日历。
                </p>
            </aside>
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
const year = new Date().getFullYear();

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
        axisLine: { lineStyle: { color: '#ddd3c1' } },
        axisTick: { show: false },
        axisLabel: { color: '#8b8172', fontSize: 10, fontFamily: 'ui-monospace, monospace' }
    },
    yAxis: [
        {
            type: 'value',
            splitLine: { lineStyle: { color: '#e8dfcf', type: 'dotted' } },
            axisLabel: { color: '#8b8172', fontSize: 10 }
        },
        {
            type: 'value',
            min: 0,
            max: 1,
            axisLabel: { formatter: (v: number) => `${Math.round(v * 100)}%`, color: '#8b8172', fontSize: 10 },
            splitLine: { show: false }
        }
    ],
    series: [
        {
            name: '复习次数',
            type: 'bar',
            barMaxWidth: 14,
            itemStyle: { color: '#e0d3bd' },
            data: trend.value.map((t: any) => t.reviewCount)
        },
        {
            name: '正确率',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            symbol: 'circle',
            symbolSize: 5,
            lineStyle: { width: 1.5, color: '#b23a1f' },
            itemStyle: { color: '#b23a1f' },
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
    padding-top: 64px;
}

.lede-meta {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--sl-line-strong);
}

.headline {
    margin: 34px 0 0;
    font-size: clamp(38px, 5vw, 58px);
    line-height: 1.12;
    font-weight: 400;
    letter-spacing: -0.025em;
    color: var(--sl-text);
}
.headline em {
    font-style: italic;
    color: var(--sl-primary);
}

.dek {
    max-width: 560px;
    margin: 18px 0 0;
    font-size: 15px;
    line-height: 1.9;
    color: var(--sl-text-secondary);
}

.lede-actions {
    display: flex;
    align-items: center;
    gap: 22px;
    margin-top: 30px;
}

.cta {
    display: inline-flex;
    align-items: center;
    gap: 14px;
    padding: 14px 22px;
    font-family: var(--sl-font-body);
    font-size: 14px;
    letter-spacing: 0.02em;
    color: var(--sl-surface);
    background: var(--sl-text);
    border: 1px solid var(--sl-text);
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease;
}
.cta span {
    transition: transform 0.24s ease;
}
.cta:hover {
    background: var(--sl-primary);
    border-color: var(--sl-primary);
}
.cta:hover span {
    transform: translateX(4px);
}

.ghost {
    padding: 0 0 3px;
    font-family: var(--sl-font-body);
    font-size: 13px;
    color: var(--sl-text-secondary);
    background: none;
    border: 0;
    border-bottom: 1px solid var(--sl-line-strong);
    cursor: pointer;
    transition: color 0.2s ease, border-color 0.2s ease;
}
.ghost:hover {
    color: var(--sl-primary);
    border-color: var(--sl-primary);
}

.stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    margin-top: 64px;
    border-top: 1px solid var(--sl-line-strong);
    border-bottom: 1px solid var(--sl-border);
}
.stat {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 26px 24px 24px 0;
}
.stat + .stat {
    padding-left: 24px;
    border-left: 1px solid var(--sl-border);
}
.stat-label {
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: var(--sl-muted);
}
.stat b {
    font-family: var(--sl-font-display);
    font-size: 44px;
    font-weight: 400;
    line-height: 1;
    color: var(--sl-text);
    font-variant-numeric: tabular-nums;
}
.stat-unit {
    font-size: 12px;
    color: var(--sl-muted);
}

.trend {
    margin-top: 56px;
}
.section-head {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--sl-line-strong);
}
.section-head h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 400;
}
.empty-note {
    margin: 26px 0 0;
    font-family: var(--sl-font-display);
    font-size: 17px;
    line-height: 1.8;
    color: var(--sl-muted);
}

.rhythm {
    display: grid;
    grid-template-columns: 120px 1fr;
    gap: 24px;
    margin-top: 64px;
    padding-top: 24px;
    border-top: 1px solid var(--sl-border);
}
.rhythm p {
    max-width: 620px;
    margin: 0;
    font-family: var(--sl-font-display);
    font-size: 17px;
    line-height: 1.9;
    color: var(--sl-text-secondary);
}
.rhythm b {
    font-weight: 500;
    color: var(--sl-text);
}

@media (max-width: 780px) {
    .dash {
        padding-top: 36px;
    }
    .stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        margin-top: 40px;
    }
    .stat {
        padding: 20px 16px 18px 0;
    }
    .stat:nth-child(odd) {
        border-left: 0;
        padding-left: 0;
    }
    .stat b {
        font-size: 34px;
    }
    .rhythm {
        grid-template-columns: 1fr;
        gap: 12px;
        margin-top: 40px;
    }
}
</style>
