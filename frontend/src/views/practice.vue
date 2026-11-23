<template>
    <div class="practice">
        <div class="topbar">
            <el-button link class="back" @click="exit">
                <el-icon><ArrowLeft /></el-icon>
                今日复习
            </el-button>

            <template v-if="questions.length && !finished">
                <span class="progress-text">第 {{ index + 1 }} / {{ questions.length }} 题</span>
                <span class="score-text">已答 {{ answered }} · 答对 {{ correct }}</span>
            </template>
        </div>

        <el-progress
            v-if="questions.length && !finished"
            class="bar"
            :percentage="Math.round((index / questions.length) * 100)"
            :show-text="false"
            :stroke-width="4"
        />

        <el-card v-if="loading" shadow="never" class="panel">
            <el-skeleton :rows="7" animated />
        </el-card>

        <el-card v-else-if="!questions.length" shadow="never" class="panel">
            <el-empty description="今天没有需要复习的题目">
                <el-button type="primary" @click="router.push('/ai')">去 AI 出题</el-button>
                <el-button @click="router.push('/dashboard')">返回今日复习</el-button>
            </el-empty>
        </el-card>

        <el-card v-else-if="finished" shadow="never" class="panel done">
            <div class="done-mark">✓</div>
            <h2>这一组做完了</h2>
            <p class="done-sub">共作答 {{ answered }} 题，答对 {{ correct }} 题</p>
            <div class="done-actions">
                <el-button type="primary" size="large" @click="restart">再来一组</el-button>
                <el-button size="large" @click="router.push('/dashboard')">返回今日复习</el-button>
            </div>
        </el-card>

        <el-card v-else shadow="never" class="panel">
            <div class="meta">
                <el-tag size="small" effect="plain">{{ typeText(current.type) }}</el-tag>
                <span class="kp">知识点 #{{ current.knowledgeId }}</span>
            </div>

            <h1 class="stem">{{ current.stem }}</h1>

            <div v-if="isChoice" class="choices">
                <button
                    v-for="option in displayOptions"
                    :key="option.value"
                    type="button"
                    class="choice"
                    :class="choiceClass(option.value)"
                    :disabled="!!result"
                    @click="select(option.value)"
                >
                    <span class="badge">{{ option.badge }}</span>
                    <span class="text">{{ option.text }}</span>
                </button>
            </div>

            <el-input
                v-else-if="current.type === 'BLANK'"
                v-model="blank"
                size="large"
                class="blank"
                placeholder="输入答案后提交"
                :disabled="!!result"
                @keyup.enter="submit"
            />

            <div v-else class="subjective">
                <el-input
                    v-model="subjectiveAnswer"
                    type="textarea"
                    :rows="4"
                    placeholder="写下你的答案，再交给 AI 按评分点判分"
                    :disabled="!!result"
                />

                <div class="subjective-actions">
                    <el-button
                        type="primary"
                        :loading="grading"
                        :disabled="!subjectiveAnswer.trim() || !!result"
                        @click="gradeSubjective"
                    >
                        AI 判分
                    </el-button>
                    <el-button v-if="!revealed" @click="revealAnswer">显示参考答案</el-button>
                </div>

                <div v-if="gradeResult" class="grade-box">
                    <div class="grade-head">
                        <b>AI 判分 {{ Math.round((gradeResult.score || 0) * 100) }} 分</b>
                        <span class="muted">{{ gradeResult.comment }}</span>
                    </div>
                    <div v-for="(point, i) in gradeResult.points || []" :key="i" class="grade-point">
                        <span class="point-hit" :class="point.hit ? 'ok' : 'bad'">{{ point.hit ? '✓' : '✕' }}</span>
                        <span class="point-text">{{ point.point }}</span>
                        <span v-if="point.comment" class="point-comment">{{ point.comment }}</span>
                    </div>
                </div>

                <div v-if="revealed" class="answer-box">
                    <div><b>参考答案：</b>{{ revealedAnswer || '—' }}</div>
                    <div v-if="revealedAnalysis" class="muted"><b>解析：</b>{{ revealedAnalysis }}</div>
                </div>
            </div>

            <div class="actions">
                <template v-if="!result">
                    <el-button
                        v-if="isObjective"
                        type="primary"
                        size="large"
                        :loading="submitting"
                        :disabled="!canSubmit"
                        @click="submit"
                    >
                        提交作答
                    </el-button>
                    <template v-else>
                        <el-button v-if="revealed || gradeResult" size="large" :loading="submitting" @click="rate(0)">
                            忘记
                        </el-button>
                        <el-button v-if="revealed || gradeResult" size="large" :loading="submitting" @click="rate(1)">
                            模糊
                        </el-button>
                        <el-button v-if="revealed || gradeResult" type="primary" size="large" :loading="submitting" @click="rate(2)">
                            记住
                        </el-button>
                    </template>
                    <el-button text class="skip" @click="skip">跳过这题</el-button>
                </template>
                <el-button v-else type="primary" size="large" @click="next">
                    {{ index + 1 === questions.length ? '查看结果' : '下一题' }}
                </el-button>
            </div>

            <div v-if="result" class="result" :class="resultClass">
                <div class="result-head">
                    <b>{{ result.score >= 1 ? '回答正确' : result.score > 0 ? '部分正确' : '回答错误' }}</b>
                    <span v-if="result.nextDueDate" class="next-due">下次复习 {{ result.nextDueDate }}</span>
                </div>
                <div class="answer-line">正确答案：{{ (result.correctAnswer || []).join('、') || '—' }}</div>
                <div v-if="result.analysis" class="analysis">解析：{{ result.analysis }}</div>
                <div v-if="result.masterySynced === false" class="sync">
                    成绩已保存，掌握度统计会在一分钟内补上
                </div>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { aiGradeQuestion, getDueQuestions, rateQuestion, submitQuestionAnswer } from '@/api';
import { http } from '@/utils/request';

const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const questions = ref<any[]>([]);
const index = ref(0);
const correct = ref(0);
const answered = ref(0);
const single = ref('');
const multiple = ref<string[]>([]);
const blank = ref('');
const result = ref<any>(null);
const revealed = ref(false);
const revealedAnswer = ref('');
const revealedAnalysis = ref('');
const subjectiveAnswer = ref('');
const grading = ref(false);
const gradeResult = ref<any>(null);
const startAt = ref(Date.now());

const current = computed(() => questions.value[index.value] || {});
const finished = computed(() => questions.value.length > 0 && index.value >= questions.value.length);

const isObjective = computed(() =>
    ['SINGLE', 'MULTIPLE', 'JUDGE', 'BLANK'].includes(current.value?.type)
);
const isChoice = computed(() => ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(current.value?.type));

const displayOptions = computed(() => {
    if (!current.value?.type) {
        return [];
    }
    if (current.value.type === 'JUDGE') {
        return [
            { value: '正确', badge: '✓', text: '正确' },
            { value: '错误', badge: '✕', text: '错误' }
        ];
    }
    return (current.value.options || []).map((text: string, i: number) => ({
        value: String.fromCharCode(65 + i),
        badge: String.fromCharCode(65 + i),
        text
    }));
});

const selectedValues = computed(() => {
    if (current.value?.type === 'MULTIPLE') {
        return multiple.value;
    }
    return single.value ? [single.value] : [];
});

const canSubmit = computed(() => {
    if (current.value?.type === 'MULTIPLE') {
        return multiple.value.length > 0;
    }
    if (current.value?.type === 'BLANK') {
        return blank.value.trim().length > 0;
    }
    return single.value !== '';
});

const resultClass = computed(() => {
    if (!result.value) {
        return '';
    }
    if (result.value.score >= 1) {
        return 'ok';
    }
    return result.value.score > 0 ? 'part' : 'bad';
});

const typeMap: Record<string, string> = {
    SINGLE: '单选',
    MULTIPLE: '多选',
    JUDGE: '判断',
    BLANK: '填空',
    SUBJECTIVE: '主观',
    COMPOSITE: '综合',
    IMAGE: '图片'
};
const typeText = (type: string) => typeMap[type] || type;

const choiceClass = (value: string) => {
    const selected = selectedValues.value.includes(value);
    if (result.value) {
        const correctAnswer = result.value.correctAnswer || [];
        if (correctAnswer.includes(value)) {
            return 'correct';
        }
        if (selected) {
            return 'wrong';
        }
        return 'muted';
    }
    return selected ? 'selected' : '';
};

const select = (value: string) => {
    if (result.value) {
        return;
    }
    if (current.value.type === 'MULTIPLE') {
        const i = multiple.value.indexOf(value);
        if (i >= 0) {
            multiple.value.splice(i, 1);
        } else {
            multiple.value.push(value);
        }
        return;
    }
    single.value = value;
};

const resetCard = () => {
    single.value = '';
    multiple.value = [];
    blank.value = '';
    result.value = null;
    revealed.value = false;
    revealedAnswer.value = '';
    revealedAnalysis.value = '';
    subjectiveAnswer.value = '';
    gradeResult.value = null;
    startAt.value = Date.now();
};

const load = async () => {
    loading.value = true;
    try {
        const data = await getDueQuestions();
        questions.value = data.questions || [];
        index.value = 0;
        answered.value = 0;
        correct.value = 0;
        resetCard();
    } finally {
        loading.value = false;
    }
};

const buildAnswer = () => {
    if (current.value.type === 'MULTIPLE') {
        return [...multiple.value].sort();
    }
    if (current.value.type === 'BLANK') {
        return [blank.value.trim()];
    }
    return [single.value];
};

const submit = async () => {
    if (!canSubmit.value) {
        ElMessage.warning('请先作答');
        return;
    }
    submitting.value = true;
    try {
        const data = await submitQuestionAnswer(
            current.value.questionId,
            buildAnswer(),
            Date.now() - startAt.value
        );
        result.value = data;
        answered.value += 1;
        if (data.score >= 1) {
            correct.value += 1;
        }
    } finally {
        submitting.value = false;
    }
};

const revealAnswer = async () => {
    const detail = await http.get(`/questions/${current.value.questionId}`);
    revealedAnswer.value = (detail.answer || []).join('、');
    revealedAnalysis.value = detail.analysis || '';
    revealed.value = true;
};

const gradeSubjective = async () => {
    if (!subjectiveAnswer.value.trim()) {
        return;
    }
    grading.value = true;
    try {
        gradeResult.value = await aiGradeQuestion(current.value.questionId, subjectiveAnswer.value.trim());
    } finally {
        grading.value = false;
    }
};

const rate = async (rating: number) => {
    submitting.value = true;
    try {
        const data = await rateQuestion(current.value.questionId, rating, Date.now() - startAt.value);
        result.value = data;
        answered.value += 1;
        if (data.score >= 1) {
            correct.value += 1;
        }
    } finally {
        submitting.value = false;
    }
};

const next = () => {
    index.value += 1;
    resetCard();
};

const skip = () => {
    next();
};

const restart = () => {
    load();
};

const exit = () => {
    router.push('/dashboard');
};

onMounted(load);
</script>

<style scoped>
.practice {
    max-width: 880px;
    margin: 0 auto;
    padding: 24px 24px 56px;
}

.topbar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 12px;
}
.back {
    font-size: 13px;
    color: #64748b;
}
.progress-text {
    margin-left: auto;
    font-size: 13px;
    color: #334155;
    font-variant-numeric: tabular-nums;
}
.score-text {
    font-size: 13px;
    color: #94a3b8;
    font-variant-numeric: tabular-nums;
}

.bar {
    margin-bottom: 16px;
}

.panel {
    border-radius: 14px;
}

.meta {
    display: flex;
    align-items: center;
    gap: 10px;
}
.kp {
    font-size: 12px;
    color: #94a3b8;
}

.stem {
    margin: 14px 0 22px;
    font-size: 20px;
    line-height: 1.65;
    font-weight: 600;
    color: #0f172a;
}

.choices {
    display: flex;
    flex-direction: column;
    gap: 10px;
}
.choice {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 14px 16px;
    font-size: 14px;
    line-height: 1.55;
    text-align: left;
    color: #334155;
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    cursor: pointer;
    transition: border-color 0.15s ease, background 0.15s ease;
}
.choice:hover:not(:disabled) {
    border-color: #93c5fd;
    background: #f8fbff;
}
.choice:disabled {
    cursor: default;
}
.badge {
    flex: none;
    width: 24px;
    height: 24px;
    line-height: 24px;
    font-size: 12px;
    text-align: center;
    color: #64748b;
    background: #f1f5f9;
    border-radius: 50%;
}
.choice.selected {
    border-color: #2563eb;
    background: #eff6ff;
}
.choice.selected .badge {
    color: #fff;
    background: #2563eb;
}
.choice.correct {
    border-color: #10b981;
    background: #ecfdf5;
}
.choice.correct .badge {
    color: #fff;
    background: #10b981;
}
.choice.wrong {
    border-color: #ef4444;
    background: #fef2f2;
}
.choice.wrong .badge {
    color: #fff;
    background: #ef4444;
}
.choice.muted {
    color: #94a3b8;
}

.blank {
    max-width: 420px;
}

.subjective {
    margin-top: 4px;
}
.subjective-actions {
    display: flex;
    gap: 10px;
    margin-top: 12px;
}
.grade-box {
    margin-top: 14px;
    padding: 14px 16px;
    background: #f8fafc;
    border: 1px solid #eef2f7;
    border-radius: 10px;
}
.grade-head {
    display: flex;
    align-items: baseline;
    gap: 10px;
    margin-bottom: 8px;
    font-size: 13px;
}
.grade-point {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 5px 0;
    font-size: 12px;
    line-height: 1.6;
    color: #475569;
}
.point-hit.ok {
    color: #10b981;
}
.point-hit.bad {
    color: #ef4444;
}
.point-text {
    flex: 1;
}
.point-comment {
    color: #94a3b8;
}
.answer-box {
    margin-top: 12px;
    padding: 14px 16px;
    line-height: 1.8;
    background: #f8fafc;
    border: 1px solid #eef2f7;
    border-radius: 10px;
}
.muted {
    color: #909399;
    font-size: 13px;
}

.actions {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 24px;
}
.skip {
    margin-left: auto;
    color: #94a3b8;
}

.result {
    margin-top: 20px;
    padding: 16px;
    border: 1px solid #e5e7eb;
    border-left-width: 4px;
    border-radius: 10px;
    background: #f8fafc;
}
.result.ok {
    border-left-color: #10b981;
    background: #f0fdf4;
}
.result.part {
    border-left-color: #f59e0b;
    background: #fffbeb;
}
.result.bad {
    border-left-color: #ef4444;
    background: #fef2f2;
}
.result-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-size: 14px;
}
.next-due {
    font-size: 12px;
    color: #94a3b8;
}
.answer-line,
.analysis {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.7;
    color: #334155;
}
.sync {
    margin-top: 6px;
    font-size: 12px;
    color: #94a3b8;
}

.done {
    padding: 48px 24px;
    text-align: center;
}
.done-mark {
    width: 56px;
    height: 56px;
    margin: 0 auto 16px;
    font-size: 28px;
    line-height: 56px;
    color: #fff;
    background: #10b981;
    border-radius: 50%;
}
.done h2 {
    margin: 0 0 8px;
    font-size: 22px;
    color: #0f172a;
}
.done-sub {
    margin: 0 0 24px;
    color: #64748b;
}
.done-actions {
    display: flex;
    justify-content: center;
    gap: 10px;
}

@media (max-width: 640px) {
    .practice {
        padding: 16px 14px 40px;
    }
    .stem {
        font-size: 18px;
    }
}
</style>
