<template>
    <div class="page">
        <el-card>
            <div class="row">
                <el-select v-model="courseId" placeholder="选择课程" style="width: 200px">
                    <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
                <el-input-number v-model="count" :min="1" :max="50" />
                <span class="muted">题</span>
                <el-button type="primary" :loading="loading" @click="generate">生成试卷</el-button>
                <el-button v-if="result" @click="reset">再来一套</el-button>
            </div>
            <div class="muted tip">只抽单选、多选、判断、填空；主观题没法自动判分，不参与组卷。</div>
        </el-card>

        <el-card v-if="quiz && !result" class="block">
            <template #header>
                <div class="card-header">
                    <span>{{ quiz.title }} · 共 {{ quiz.questions.length }} 题</span>
                    <span class="muted">答完记得交卷</span>
                </div>
            </template>

            <div v-for="q in quiz.questions" :key="q.questionId" class="q">
                <div class="stem">{{ q.sort }}. {{ q.stem }}</div>

                <el-radio-group
                    v-if="q.type === 'SINGLE' || q.type === 'JUDGE'"
                    v-model="answers[q.questionId]"
                >
                    <el-radio
                        v-for="(opt, i) in optionsOf(q)"
                        :key="i"
                        :value="opt.value"
                        class="option"
                    >
                        {{ opt.label }}
                    </el-radio>
                </el-radio-group>

                <el-checkbox-group
                    v-else-if="q.type === 'MULTIPLE'"
                    v-model="answers[q.questionId]"
                >
                    <el-checkbox
                        v-for="(opt, i) in optionsOf(q)"
                        :key="i"
                        :value="opt.value"
                        class="option"
                    >
                        {{ opt.label }}
                    </el-checkbox>
                </el-checkbox-group>

                <el-input
                    v-else
                    v-model="answers[q.questionId]"
                    placeholder="填写答案"
                    style="max-width: 360px"
                />
            </div>

            <el-button type="primary" size="large" :loading="submitting" @click="submit">交卷</el-button>
        </el-card>

        <el-card v-if="result" class="block">
            <div class="score-box">
                <div class="score">{{ result.score }}</div>
                <div class="muted">分 · 共 {{ result.total }} 题 · 全对 {{ result.correctCount }} 题</div>
            </div>

            <div v-for="item in result.items" :key="item.questionId" class="result-item">
                <div class="stem">{{ item.stem }}</div>
                <div class="answers">
                    <el-tag :type="item.score >= 1 ? 'success' : item.score > 0 ? 'warning' : 'danger'" size="small">
                        {{ item.score >= 1 ? '答对' : item.score > 0 ? '部分对' : '答错' }}
                    </el-tag>
                    <span>你的答案：{{ (item.yourAnswer || []).join('、') || '未作答' }}</span>
                    <span>正确答案：{{ (item.correctAnswer || []).join('、') }}</span>
                </div>
                <div v-if="item.analysis" class="analysis">解析：{{ item.analysis }}</div>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createQuiz, getCourses, getQuiz, submitQuiz } from '@/api';

const courses = ref<any[]>([]);
const courseId = ref<number | null>(null);
const count = ref(10);
const loading = ref(false);
const submitting = ref(false);
const quiz = ref<any>(null);
const result = ref<any>(null);
const answers = reactive<Record<string, any>>({});

const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F'];

const optionsOf = (q: any) => {
    if (q.type === 'JUDGE') {
        return [
            { value: '正确', label: '正确' },
            { value: '错误', label: '错误' }
        ];
    }
    return (q.options || []).map((text: string, i: number) => ({
        value: LETTERS[i],
        label: `${LETTERS[i]}. ${text}`
    }));
};

const generate = async () => {
    if (!courseId.value) {
        ElMessage.warning('请先选择课程');
        return;
    }
    loading.value = true;
    try {
        const quizId = await createQuiz({ courseId: courseId.value, count: count.value });
        quiz.value = await getQuiz(quizId);
        result.value = null;

        // 初始化作答容器，多选用数组，其余用字符串
        Object.keys(answers).forEach(k => delete answers[k]);
        for (const q of quiz.value.questions) {
            answers[q.questionId] = q.type === 'MULTIPLE' ? [] : '';
        }
    } finally {
        loading.value = false;
    }
};

const submit = async () => {
    const payload = quiz.value.questions.map((q: any) => {
        const value = answers[q.questionId];
        let list: string[] = [];
        if (Array.isArray(value)) {
            list = [...value].sort();
        } else if (value) {
            list = [value];
        }
        return { questionId: q.questionId, answer: list };
    });

    submitting.value = true;
    try {
        result.value = await submitQuiz(quiz.value.id, payload);
        ElMessage.success('已交卷，这次结果已经影响复习计划');
    } finally {
        submitting.value = false;
    }
};

const reset = () => {
    quiz.value = null;
    result.value = null;
};

onMounted(async () => {
    const data = await getCourses({ page: 1, size: 100 });
    courses.value = data.records;
    courseId.value = data.records[0]?.id ?? null;
});
</script>

<style scoped>
.page {
    padding: 20px;
}
.row {
    display: flex;
    align-items: center;
    gap: 10px;
}
.tip {
    margin-top: 10px;
}
.block {
    margin-top: 16px;
}
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.muted {
    color: #909399;
    font-size: 13px;
}
.q {
    padding: 14px 0;
    border-bottom: 1px solid #f0f2f5;
}
.stem {
    font-weight: 500;
    margin-bottom: 10px;
    line-height: 1.6;
}
.option {
    display: block;
    height: 30px;
}
.score-box {
    display: flex;
    align-items: baseline;
    gap: 10px;
    margin-bottom: 16px;
}
.score {
    font-size: 42px;
    font-weight: 700;
    color: #409eff;
}
.result-item {
    padding: 12px 0;
    border-bottom: 1px dashed #ebeef5;
}
.answers {
    display: flex;
    gap: 14px;
    font-size: 13px;
    color: #606266;
    margin-top: 6px;
}
.analysis {
    margin-top: 6px;
    font-size: 13px;
    color: #909399;
}
</style>
