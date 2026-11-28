<template>
    <div class="practice mx-auto max-w-3xl">
        <header class="mb-4 flex items-center justify-between gap-3">
            <button
                type="button"
                class="text-sm text-ink-muted outline-none hover:text-ink"
                @click="exit"
            >
                ← 概览
            </button>
            <div v-if="questions.length && !finished" class="text-xs text-ink-muted">
                第 {{ index + 1 }} / {{ questions.length }} 题 · 答对 {{ correct }} / {{ answered }}
            </div>
        </header>

        <div v-if="questions.length && !finished" class="mb-4 h-1 overflow-hidden rounded-full bg-surface-2">
            <span
                class="block h-full rounded-full bg-brand transition-all"
                :style="{ width: `${Math.round((index / questions.length) * 100)}%` }"
            ></span>
        </div>

        <div v-if="loading" class="space-y-3 rounded-xl border border-line bg-surface p-6">
            <div class="h-4 w-24 animate-pulse rounded bg-surface-2"></div>
            <div class="h-6 w-3/4 animate-pulse rounded bg-surface-2"></div>
            <div class="h-12 animate-pulse rounded bg-surface-2"></div>
            <div class="h-12 animate-pulse rounded bg-surface-2"></div>
        </div>

        <section v-else-if="!questions.length" class="rounded-xl border border-line bg-surface p-8 text-center">
            <h1 class="text-xl font-semibold text-ink-strong">没有到期题目</h1>
            <p class="mt-2 text-sm text-ink-muted">可以去补题，或做一组自测。</p>
            <div class="mt-6 flex justify-center gap-3">
                <button
                    type="button"
                    class="rounded-md bg-brand px-4 py-2 text-sm text-white hover:opacity-90"
                    @click="router.push('/ai')"
                >
                    去 AI 出题
                </button>
                <button
                    type="button"
                    class="rounded-md border border-line px-4 py-2 text-sm text-ink hover:bg-surface-2"
                    @click="router.push('/dashboard')"
                >
                    返回概览
                </button>
            </div>
        </section>

        <section v-else-if="finished" class="rounded-xl border border-line bg-surface p-8 text-center">
            <h1 class="text-xl font-semibold text-ink-strong">这一组完成</h1>
            <p class="mt-2 text-sm text-ink-muted">
                共作答 <b class="text-ink">{{ answered }}</b> 题，答对
                <b class="text-ink">{{ correct }}</b> 题。
            </p>
            <div class="mt-6 flex justify-center gap-3">
                <button
                    type="button"
                    class="rounded-md bg-brand px-4 py-2 text-sm text-white hover:opacity-90"
                    @click="restart"
                >
                    再来一组
                </button>
                <button
                    type="button"
                    class="rounded-md border border-line px-4 py-2 text-sm text-ink hover:bg-surface-2"
                    @click="router.push('/dashboard')"
                >
                    返回概览
                </button>
            </div>
        </section>

        <section v-else class="rounded-xl border border-line bg-surface p-6 sm:p-8">
            <div class="mb-3 flex items-center gap-3 text-xs text-ink-muted">
                <span class="rounded-full bg-brand-soft px-2 py-0.5 text-ink-blue">{{ typeText(current.type) }}</span>
                <span v-if="current.knowledgeId">知识点 #{{ current.knowledgeId }}</span>
            </div>

            <h1 class="text-lg font-medium leading-relaxed text-ink-strong sm:text-xl">{{ current.stem }}</h1>

            <div v-if="isChoice" class="mt-6 space-y-2.5">
                <button
                    v-for="option in displayOptions"
                    :key="option.value"
                    type="button"
                    class="flex w-full items-start gap-3 rounded-lg border px-4 py-3 text-left text-sm transition-colors disabled:cursor-default"
                    :class="optionTone(option.value)"
                    :disabled="!!result"
                    @click="select(option.value)"
                >
                    <span class="mt-0.5 grid size-5 shrink-0 place-items-center rounded-full border border-current text-[11px]">
                        {{ option.badge }}
                    </span>
                    <span class="text-ink">{{ option.text }}</span>
                </button>
            </div>

            <input
                v-else-if="current.type === 'BLANK'"
                v-model="blank"
                class="mt-6 w-full rounded-md border border-line bg-reader px-3 py-2.5 text-sm outline-none focus:border-brand"
                placeholder="输入答案后提交"
                :disabled="!!result"
                @keyup.enter="submit"
            />

            <div v-else class="mt-6">
                <textarea
                    v-model="subjectiveAnswer"
                    rows="5"
                    class="w-full resize-y rounded-md border border-line bg-reader px-3 py-2.5 text-sm outline-none focus:border-brand"
                    placeholder="输入答案"
                    :disabled="!!result"
                ></textarea>

                <div class="mt-3 flex flex-wrap items-center gap-3">
                    <button
                        type="button"
                        class="rounded-md bg-brand px-3 py-1.5 text-xs text-white disabled:opacity-40"
                        :disabled="!subjectiveAnswer.trim() || !!result || grading"
                        @click="gradeSubjective"
                    >
                        {{ grading ? '判分中…' : 'AI 判分' }}
                    </button>
                    <button
                        v-if="!revealed"
                        type="button"
                        class="text-xs text-ink-muted underline-offset-2 hover:underline"
                        @click="revealAnswer"
                    >
                        显示参考答案
                    </button>
                </div>

                <div v-if="gradeResult" class="mt-4 rounded-lg border border-line bg-surface-2 p-4">
                    <div class="flex items-baseline gap-3">
                        <b class="text-lg text-ink-strong">{{ Math.round((gradeResult.score || 0) * 100) }} 分</b>
                        <span class="text-xs text-ink-muted">{{ gradeResult.comment }}</span>
                    </div>
                    <div v-for="(point, i) in gradeResult.points || []" :key="i" class="mt-2 flex gap-2 text-xs">
                        <span :class="point.hit ? 'text-ok' : 'text-danger'">{{ point.hit ? '✓' : '✕' }}</span>
                        <span class="text-ink">{{ point.point }}</span>
                        <span v-if="point.comment" class="text-ink-muted">{{ point.comment }}</span>
                    </div>
                </div>

                <div v-if="revealed" class="mt-4 rounded-lg border border-line bg-reader p-4 text-sm">
                    <div><span class="text-xs text-ink-muted">答案</span> {{ revealedAnswer || '—' }}</div>
                    <div v-if="revealedAnalysis" class="mt-2 text-ink-muted">
                        <span class="text-xs">解析</span> {{ revealedAnalysis }}
                    </div>
                </div>
            </div>

            <div class="mt-6 flex flex-wrap items-center gap-3">
                <template v-if="!result">
                    <button
                        v-if="isObjective"
                        type="button"
                        class="rounded-md bg-brand px-4 py-2 text-sm text-white disabled:opacity-40"
                        :disabled="!canSubmit || submitting"
                        @click="submit"
                    >
                        {{ submitting ? '提交中…' : '提交作答' }}
                    </button>
                    <template v-else>
                        <template v-if="revealed || gradeResult">
                            <button type="button" class="rounded-md border border-line px-3 py-1.5 text-xs" :disabled="submitting" @click="rate(0)">忘记</button>
                            <button type="button" class="rounded-md border border-line px-3 py-1.5 text-xs" :disabled="submitting" @click="rate(1)">模糊</button>
                            <button type="button" class="rounded-md border border-line px-3 py-1.5 text-xs" :disabled="submitting" @click="rate(2)">记住</button>
                        </template>
                    </template>
                    <button type="button" class="text-xs text-ink-muted hover:text-ink" @click="skip">跳过这题</button>
                </template>
                <button
                    v-else
                    type="button"
                    class="rounded-md bg-brand px-4 py-2 text-sm text-white"
                    @click="next"
                >
                    {{ index + 1 === questions.length ? '查看结果' : '下一题' }}
                </button>
            </div>

            <div
                v-if="result"
                class="mt-5 rounded-lg border p-4"
                :class="result.score >= 1 ? 'border-ok bg-ok-soft' : result.score > 0 ? 'border-warn bg-warn-soft' : 'border-danger bg-danger-soft'"
            >
                <div class="flex items-baseline justify-between gap-3">
                    <b class="text-sm">{{ result.score >= 1 ? '回答正确' : result.score > 0 ? '部分正确' : '回答错误' }}</b>
                    <span v-if="result.nextDueDate" class="text-xs text-ink-muted">下次 {{ result.nextDueDate }}</span>
                </div>
                <div class="mt-2 text-sm">正确答案：{{ (result.correctAnswer || []).join('、') || '—' }}</div>
                <div v-if="result.analysis" class="mt-1 text-sm text-ink-muted">解析：{{ result.analysis }}</div>
                <div v-if="result.masterySynced === false" class="mt-2 text-xs text-ink-muted">
                    成绩已保存，掌握度统计会在一分钟内补上
                </div>
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

const optionTone = (value: string) => {
    if (!result.value) {
        return selectedValues.value.includes(value)
            ? 'border-brand bg-brand-soft'
            : 'border-line hover:bg-surface-2';
    }
    const correct = result.value.correctAnswer || [];
    if (correct.includes(value)) {
        return 'border-ok bg-ok-soft';
    }
    return selectedValues.value.includes(value)
        ? 'border-danger bg-danger-soft'
        : 'border-line opacity-60';
};

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
