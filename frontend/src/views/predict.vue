<template>
    <div class="page sl-page">
        <el-card shadow="never">
            <template #header>
                <div class="card-header">
                    <span>预测卷</span>
                    <span class="hint">根据课件里的「重点」标注、例题和反复出现的内容，挑更可能考的题</span>
                </div>
            </template>

            <div class="row">
                <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="load">
                    <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
                <el-input-number v-model="size" :min="1" :max="30" />
                <span class="hint">题</span>
                <el-button type="primary" :loading="loading" :disabled="!courseId" @click="build">
                    生成预测卷
                </el-button>
            </div>

            <div v-if="lowConfidence" class="hint warn">
                有些题在课件里找不到出处，参考价值不高，建议以有依据的题为主。
            </div>

            <div class="hint note">
                怎么排的：老师标「重点」、出现例题、反复讲到、能找到原文出处，这四类线索越强，排得越靠前。
                找不到出处的知识点会排在最后，只少量出现。
            </div>
        </el-card>

        <el-card class="block" shadow="never">
            <template #header>
                <div class="card-header">
                    <span>题目（{{ items.length }}）</span>
                    <el-tag v-if="items.length" type="success" size="small">按可能性从高到低</el-tag>
                </div>
            </template>

            <el-skeleton v-if="loading" :rows="5" animated />
            <el-empty v-else-if="items.length === 0" description="先选课程，再点生成预测卷" :image-size="90" />

            <div v-else class="paper sl-stagger">
                <div v-for="(item, index) in items" :key="item.questionId" class="question" :style="{ '--i': index }">
                    <div class="stem">
                        <b>{{ index + 1 }}.</b>
                        <span>{{ item.stem }}</span>
                        <el-tag size="small" effect="plain">{{ typeText(item.type) }}</el-tag>
                    </div>
                    <div class="reason">
                        <el-tag type="warning" size="small" effect="plain">
                            考到可能性 {{ Math.round(item.weight * 100) }}%
                        </el-tag>
                        <span class="hint">{{ item.reason }}</span>
                        <el-popover v-if="item.evidenceQuote" placement="bottom" :width="420" trigger="click">
                            <template #reference>
                                <el-link type="primary" :underline="false">看看依据</el-link>
                            </template>
                            <div class="quote-page">第 {{ item.evidencePage }} 页</div>
                            <div class="quote-text">{{ item.evidenceQuote }}</div>
                        </el-popover>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getCourses, getPredict } from '@/api';

const courses = ref<any[]>([]);
const courseId = ref<number | null>(null);
const size = ref(10);
const loading = ref(false);
const items = ref<any[]>([]);

const lowConfidence = computed(() => items.value.some((item) => !item.evidenceQuote));

const typeText = (type: string) =>
    ({ SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', BLANK: '填空', SUBJECTIVE: '简答', COMPOSITE: '综合题' } as any)[type] || type;

const build = async () => {
    loading.value = true;
    try {
        const data = await getPredict(courseId.value as number, size.value);
        items.value = data.items || [];
        if (items.value.length === 0) {
            ElMessage.warning(data.message || '还没有可用的题目，先去 AI 出题');
        }
    } finally {
        loading.value = false;
    }
};

const load = async () => {
    items.value = [];
    if (courseId.value) {
        await build();
    }
};

onMounted(async () => {
    const data = await getCourses({ page: 1, size: 100 });
    courses.value = data.records;
    if (courses.value.length > 0) {
        courseId.value = courses.value[0].id;
        await build();
    }
});
</script>

<style scoped>
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.row {
    display: flex;
    align-items: center;
    gap: var(--sl-s3);
}
.block {
    margin-top: var(--sl-s4);
}
.note {
    margin-top: var(--sl-s3);
}
.paper {
    display: flex;
    flex-direction: column;
    gap: var(--sl-s4);
}
.question {
    padding: var(--sl-s4);
    border: 1px solid var(--sl-border);
    border-radius: var(--sl-radius-sm);
}
.stem {
    display: flex;
    align-items: flex-start;
    gap: var(--sl-s2);
    line-height: 1.7;
}
.reason {
    display: flex;
    align-items: center;
    gap: var(--sl-s2);
    margin-top: var(--sl-s2);
}
.quote-page {
    font-size: 12px;
    color: var(--sl-muted);
    margin-bottom: 6px;
}
.quote-text {
    font-size: 13px;
    line-height: 1.7;
    color: var(--sl-text);
}
.warn {
    color: #b45309;
    margin-top: var(--sl-s2);
}
</style>
