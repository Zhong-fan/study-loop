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
                <el-card><div class="num">{{ overview.masteredQuestions }}</div><div class="label">已掌握</div></el-card>
            </el-col>
            <el-col :span="6">
                <el-card><div class="num">{{ overview.streakDays }}</div><div class="label">连续复习天数</div></el-card>
            </el-col>
        </el-row>

        <el-card class="block">
            <template #header>薄弱章节 Top 5</template>
            <el-table :data="weak" border size="small">
                <el-table-column prop="chapterId" label="章节 ID" width="120" />
                <el-table-column prop="wrongCount" label="答错次数" width="120" />
                <el-table-column prop="totalCount" label="复习次数" width="120" />
                <el-table-column label="错误率">
                    <template #default="{ row }">{{ (row.wrongRate * 100).toFixed(0) }}%</template>
                </el-table-column>
            </el-table>
            <div class="hint">章节名称在课程服务里，这里按 ID 展示；前端可以做映射</div>
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
import { getOverview, getTrend, getWeakChapters } from '@/api';

const overview = ref<any>({
    totalQuestions: 0,
    dueToday: 0,
    masteredQuestions: 0,
    streakDays: 0
});
const weak = ref<any[]>([]);
const trend = ref<any[]>([]);

onMounted(async () => {
    overview.value = await getOverview();
    weak.value = await getWeakChapters();
    trend.value = await getTrend(30);
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
    margin-top: 6px;
    color: #909399;
    font-size: 13px;
}
.block {
    margin-top: 16px;
}
.hint {
    margin-top: 8px;
    color: #909399;
    font-size: 12px;
}
</style>
