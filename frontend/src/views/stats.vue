<template>
    <div class="page">
        <el-row :gutter="16">
            <el-col :span="6">
                <el-card><div class="num">{{ overview.totalQuestions }}</div><div class="label">题目总数</div></el-card>
            </el-col>
            <el-col :span="6">
                <el-card><div class="num">{{ overview.dueToday }}</div><div class="label">今日待复习</div></el-card>
            </el-col>
            <el-col :span="6">
                <el-card><div class="num">{{ overview.masteredQuestions }}</div><div class="label">已掌握题目</div></el-card>
            </el-col>
            <el-col :span="6">
                <el-card><div class="num">{{ overview.streakDays }}</div><div class="label">连续复习天数</div></el-card>
            </el-col>
        </el-row>

        <el-card class="block">
            <template #header>
                <div class="card-header">
                    <span>知识点覆盖率</span>
                    <el-select v-model="courseId" placeholder="选择课程" style="width: 200px" @change="loadCoverage">
                        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                </div>
            </template>

            <el-row :gutter="16">
                <el-col :span="6">
                    <div class="num">{{ coverage.mastered }}</div>
                    <div class="label">已掌握知识点</div>
                </el-col>
                <el-col :span="6">
                    <div class="num">{{ coverage.practiced }}</div>
                    <div class="label">练过的知识点</div>
                </el-col>
                <el-col :span="6">
                    <div class="num">{{ coverage.totalKnowledge }}</div>
                    <div class="label">知识点总数</div>
                </el-col>
                <el-col :span="6">
                    <div class="num">{{ (coverage.coverage * 100).toFixed(0) }}%</div>
                    <div class="label">覆盖率</div>
                </el-col>
            </el-row>

            <div class="hint">
                掌握 = 正确率 ≥ 80% 且至少练过 2 次；只练一次就全对不算，避免蒙对。
            </div>
        </el-card>

        <el-card class="block">
            <template #header>盲区清单（正确率 &lt; 60%）</template>
            <el-table :data="weakKnowledge" border size="small">
                <el-table-column prop="knowledgeId" label="知识点 ID" width="120" />
                <el-table-column label="正确率" width="100">
                    <template #default="{ row }">{{ (row.accuracy * 100).toFixed(0) }}%</template>
                </el-table-column>
                <el-table-column prop="totalCount" label="练过次数" width="100" />
                <el-table-column prop="stage" label="当前档位" width="100" />
                <el-table-column prop="nextReview" label="下次复习" />
            </el-table>
            <div class="hint">知识点名称在内容服务里，这里按 ID 展示</div>
        </el-card>

        <el-card class="block">
            <template #header>最近 30 天复习趋势</template>
            <el-table :data="trend" border size="small" max-height="360">
                <el-table-column prop="statDate" label="日期" width="140" />
                <el-table-column prop="reviewCount" label="复习次数" width="120" />
                <el-table-column prop="correctCount" label="正确次数" width="120" />
                <el-table-column label="正确率">
                    <template #default="{ row }">{{ (row.accuracy * 100).toFixed(0) }}%</template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getCoverage, getCourses, getOverview, getTrend, getWeakKnowledge } from '@/api';

const overview = ref<any>({
    totalQuestions: 0,
    dueToday: 0,
    masteredQuestions: 0,
    streakDays: 0
});
const coverage = ref<any>({
    totalKnowledge: 0,
    practiced: 0,
    mastered: 0,
    coverage: 0
});
const courses = ref<any[]>([]);
const courseId = ref<number | null>(null);
const weakKnowledge = ref<any[]>([]);
const trend = ref<any[]>([]);

const loadCoverage = async () => {
    if (!courseId.value) {
        return;
    }
    coverage.value = await getCoverage(courseId.value);
};

onMounted(async () => {
    overview.value = await getOverview();
    trend.value = await getTrend(30);
    weakKnowledge.value = await getWeakKnowledge();

    const data = await getCourses({ page: 1, size: 100 });
    courses.value = data.records;
    if (courses.value.length > 0) {
        courseId.value = courses.value[0].id;
        await loadCoverage();
    }
});
</script>

<style scoped>
.page {
    padding: 20px;
}
.num {
    font-size: 26px;
    font-weight: 600;
    color: #409eff;
}
.label {
    color: #909399;
    font-size: 13px;
    margin-top: 4px;
}
.block {
    margin-top: 16px;
}
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.hint {
    color: #909399;
    font-size: 12px;
    margin-top: 10px;
}
</style>
