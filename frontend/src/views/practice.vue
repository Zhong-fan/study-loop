<template>
    <div class="practice">
        <header class="run-head">
            <button type="button" class="back" @click="exit">← 概览</button>
            <div class="run-meta">
                <span v-if="questions.length && !finished" class="mono">第 {{ index + 1 }} / {{ questions.length }} 题</span>
                <span v-if="questions.length && !finished" class="mono">答对 {{ correct }} / {{ answered }}</span>
            </div>
        </header>

        <div v-if="questions.length && !finished" class="progress">
            <span :style="{ width: `${Math.round((index / questions.length) * 100)}%` }"></span>
        </div>

        <el-skeleton v-if="loading" :rows="7" animated class="loading" />

        <section v-else-if="!questions.length" class="empty">
            <p class="empty-mark mono">No questions due</p>
            <h1>今天没有需要复习的题。</h1>
            <p class="empty-dek">可以去补一些新题，或者直接做一组自测。</p>
            <div class="empty-actions">
                <button type="button" class="cta" @click="router.push('/ai')">去 AI 出题 <span>→</span></button>
                <button type="button" class="ghost" @click="router.push('/dashboard')">返回概览</button>
            </div>
        </section>

        <section v-else-if="finished" class="done">
            <p class="empty-mark mono">Session complete</p>
            <h1>这一组做完了。</h1>
            <p class="done-line">
                共作答 <b>{{ answered }}</b> 题，答对 <b>{{ correct }}</b> 题。
            </p>
            <div class="empty-actions">
                <button type="button" class="cta" @click="restart">再来一组 <span>→</span></button>
                <button type="button" class="ghost" @click="router.push('/dashboard')">返回概览</button>
            </div>
        </section>

        <section v-else class="sheet">
            <div class="sheet-meta">
                <span class="mono">{{ typeText(current.type) }}</span>
                <span class="mono">KP #{{ current.knowledgeId }}</span>
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
                    <button
                        type="button"
                        class="mini-cta"
                        :disabled="!subjectiveAnswer.trim() || !!result || grading"
                        @click="gradeSubjective"
                    >
                        {{ grading ? '判分中…' : 'AI 判分' }}
                    </button>
                    <button v-if="!revealed" type="button" class="ghost" @click="revealAnswer">显示参考答案</button>
                </div>

                <div v-if="gradeResult" class="grade-box">
                    <div class="grade-head">
                        <b>{{ Math.round((gradeResult.score || 0) * 100) }} 分</b>
                        <span>{{ gradeResult.comment }}</span>
                    </div>
                    <div v-for="(point, i) in gradeResult.points || []" :key="i" class="grade-point">
                        <span class="point-hit" :class="point.hit ? 'ok' : 'bad'">{{ point.hit ? '✓' : '✕' }}</span>
                        <span class="point-text">{{ point.point }}</span>
                        <span v-if="point.comment" class="point-comment">{{ point.comment }}</span>
                    </div>
                </div>

                <div v-if="revealed" class="answer-box">
                    <div><span class="mono">答案</span> {{ revealedAnswer || '—' }}</div>
                    <div v-if="revealedAnalysis" class="muted"><span class="mono">解析</span> {{ revealedAnalysis }}</div>
                </div>
            </div>

            <div class="actions">
                <template v-if="!result">
                    <button
                        v-if="isObjective"
                        type="button"
                        class="cta"
                        :disabled="!canSubmit || submitting"
                        @click="submit"
                    >
                        {{ submitting ? '提交中…' : '提交作答' }} <span>→</span>
                    </button>
                    <template v-else>
                        <template v-if="revealed || gradeResult">
                            <button type="button" class="mini-cta" :disabled="submitting" @click="rate(0)">忘记</button>
                            <button type="button" class="mini-cta" :disabled="submitting" @click="rate(1)">模糊</button>
                            <button type="button" class="mini-cta" :disabled="submitting" @click="rate(2)">记住</button>
                        </template>
                    </template>
                    <button type="button" class="ghost skip" @click="skip">跳过这题</button>
                </template>
                <button v-else type="button" class="cta" @click="next">
                    {{ index + 1 === questions.length ? '查看结果' : '下一题' }} <span>→</span>
                </button>
            </div>

            <div v-if="result" class="result" :class="resultClass">
                <div class="result-head">
                    <b>{{ result.score >= 1 ? '回答正确' : result.score > 0 ? '部分正确' : '回答错误' }}</b>
                    <span v-if="result.nextDueDate" class="mono">下次 {{ result.nextDueDate }}</span>
                </div>
                <div class="answer-line">正确答案：{{ (result.correctAnswer || []).join('、') || '—' }}</div>
                <div v-if="result.analysis" class="analysis">解析：{{ result.analysis }}</div>
                <div v-if="result.masterySynced === false" class="sync">成绩已保存，掌握度统计会在一分钟内补上</div>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
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
    max-width: 760px;
    margin: 0 auto;
    padding-top: 48px;
}

.run-head {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--sl-line-strong);
}
.back {
    padding: 0;
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.1em;
    color: var(--sl-muted);
    background: none;
    border: 0;
    cursor: pointer;
    transition: color 0.18s ease;
}
.back:hover {
    color: var(--sl-primary);
}
.run-meta {
    display: flex;
    gap: 24px;
    color: var(--sl-muted);
}

.progress {
    height: 1px;
    margin-top: 14px;
    background: var(--sl-border);
}
.progress span {
    display: block;
    height: 1px;
    background: var(--sl-primary);
    transition: width 0.4s cubic-bezier(0.16, 0.84, 0.3, 1);
}

.loading {
    margin-top: 48px;
}

.sheet {
    padding-top: 44px;
}
.sheet-meta {
    display: flex;
    gap: 18px;
    color: var(--sl-muted);
}

.stem {
    margin: 18px 0 34px;
    font-family: var(--sl-font-display);
    font-size: clamp(24px, 2.6vw, 32px);
    line-height: 1.55;
    font-weight: 400;
    letter-spacing: -0.01em;
    color: var(--sl-text);
}

.choices {
    display: flex;
    flex-direction: column;
    border-top: 1px solid var(--sl-border);
}
.choice {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    width: 100%;
    padding: 16px 4px;
    text-align: left;
    background: none;
    border: 0;
    border-bottom: 1px solid var(--sl-border);
    cursor: pointer;
    transition: background 0.18s ease, padding-left 0.18s ease;
}
.choice:hover:not(:disabled) {
    padding-left: 10px;
    background: rgba(95, 2, 62, 0.04);
}
.choice:disabled {
    cursor: default;
}
.badge {
    flex: none;
    width: 22px;
    font-family: var(--sl-font-mono);
    font-size: 11px;
    line-height: 1.7;
    color: var(--sl-muted);
}
.text {
    font-size: 15px;
    line-height: 1.7;
    color: var(--sl-text-secondary);
}
.choice.selected .badge,
.choice.selected .text {
    color: var(--sl-primary);
}
.choice.correct .badge,
.choice.correct .text {
    color: var(--sl-success);
}
.choice.correct {
    background: rgba(47, 107, 79, 0.05);
}
.choice.wrong .badge,
.choice.wrong .text {
    color: var(--sl-danger);
}
.choice.wrong {
    background: rgba(163, 48, 34, 0.05);
}
.choice.muted .text {
    color: var(--sl-muted);
}

.blank {
    max-width: 420px;
}

.subjective {
    margin-top: 6px;
}
.subjective-actions {
    display: flex;
    align-items: center;
    gap: 20px;
    margin: 16px 0 6px;
}

.grade-box {
    margin-top: 20px;
    padding: 18px 20px;
    border: 1px solid var(--sl-border);
    background: var(--sl-surface-strong);
}
.grade-head {
    display: flex;
    align-items: baseline;
    gap: 14px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--sl-border);
}
.grade-head b {
    font-family: var(--sl-font-display);
    font-size: 26px;
    font-weight: 400;
    color: var(--sl-primary);
}
.grade-head span {
    font-size: 12px;
    color: var(--sl-muted);
}
.grade-point {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 7px 0;
    font-size: 12px;
    line-height: 1.7;
    color: var(--sl-text-secondary);
    border-bottom: 1px dotted var(--sl-border);
}
.point-hit.ok {
    color: var(--sl-success);
}
.point-hit.bad {
    color: var(--sl-danger);
}
.point-text {
    flex: 1;
}
.point-comment {
    color: var(--sl-muted);
}

.answer-box {
    margin-top: 16px;
    padding: 16px 0;
    font-family: var(--sl-font-display);
    font-size: 16px;
    line-height: 1.9;
    color: var(--sl-text-secondary);
    border-top: 1px solid var(--sl-border);
}
.answer-box .mono {
    margin-right: 8px;
    color: var(--sl-muted);
}

.actions {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-top: 34px;
}
.cta {
    display: inline-flex;
    align-items: center;
    gap: 12px;
    padding: 13px 20px;
    font-family: var(--sl-font-body);
    font-size: 14px;
    color: var(--sl-surface);
    background: var(--sl-text);
    border: 1px solid var(--sl-text);
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease, opacity 0.2s ease;
}
.cta span {
    transition: transform 0.24s ease;
}
.cta:hover:not(:disabled) {
    background: var(--sl-primary);
    border-color: var(--sl-primary);
}
.cta:hover:not(:disabled) span {
    transform: translateX(4px);
}
.cta:disabled {
    opacity: 0.35;
    cursor: default;
}
.mini-cta {
    padding: 9px 16px;
    font-family: var(--sl-font-body);
    font-size: 13px;
    color: var(--sl-text);
    background: none;
    border: 1px solid var(--sl-line-strong);
    cursor: pointer;
    transition: border-color 0.18s ease, color 0.18s ease;
}
.mini-cta:hover:not(:disabled) {
    color: var(--sl-primary);
    border-color: var(--sl-primary);
}
.mini-cta:disabled {
    opacity: 0.4;
    cursor: default;
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
}
.ghost:hover {
    color: var(--sl-primary);
    border-color: var(--sl-primary);
}
.skip {
    margin-left: auto;
    color: var(--sl-muted);
}

.result {
    margin-top: 32px;
    padding: 18px 20px;
    border-left: 2px solid var(--sl-line-strong);
    background: var(--sl-surface-strong);
}
.result.ok {
    border-left-color: var(--sl-success);
}
.result.part {
    border-left-color: var(--sl-warning);
}
.result.bad {
    border-left-color: var(--sl-danger);
}
.result-head {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    font-family: var(--sl-font-display);
    font-size: 20px;
}
.result-head b {
    font-weight: 500;
}
.answer-line,
.analysis {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.8;
    color: var(--sl-text-secondary);
}
.sync {
    margin-top: 8px;
    font-size: 12px;
    color: var(--sl-muted);
}

.empty,
.done {
    padding-top: 96px;
}
.empty-mark {
    color: var(--sl-muted);
}
.empty h1,
.done h1 {
    margin: 16px 0 0;
    font-size: clamp(34px, 4.4vw, 48px);
    line-height: 1.2;
    font-weight: 400;
    letter-spacing: -0.02em;
}
.empty-dek {
    margin: 16px 0 0;
    font-size: 15px;
    color: var(--sl-text-secondary);
}
.done-line {
    margin: 16px 0 0;
    font-family: var(--sl-font-display);
    font-size: 18px;
    color: var(--sl-text-secondary);
}
.done-line b {
    font-weight: 500;
    color: var(--sl-primary);
}
.empty-actions {
    display: flex;
    align-items: center;
    gap: 22px;
    margin-top: 30px;
}

@media (max-width: 640px) {
    .practice {
        padding-top: 28px;
    }
    .stem {
        margin-bottom: 24px;
    }
    .run-meta {
        gap: 12px;
    }
    .empty,
    .done {
        padding-top: 56px;
    }
}
</style>
