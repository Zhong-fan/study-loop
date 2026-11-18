<template>
    <div class="page">
        <el-card v-if="loading" shadow="never">
            <el-skeleton :rows="6" animated />
        </el-card>

        <el-card v-else-if="!current">
            <el-empty :description="pending.length > 0
                ? `有 ${pending.length} 个知识点到期，但还没有题目`
                : '今天没有需要复习的题目'">
                <el-button v-if="pending.length > 0" type="primary" @click="router.push('/ai')">去 AI 出题</el-button>
                <el-button @click="load">刷新</el-button>
            </el-empty>
        </el-card>

        <el-card v-else>
            <template #header>
                <div class="card-header">
                    <span>
                        剩余 {{ cards.length - index }} 题 ·
                        <el-tag size="small">{{ typeText(current.type) }}</el-tag>
                    </span>
                    <span class="muted">知识点 #{{ current.knowledgeId }}</span>
                </div>
            </template>

            <h3 class="stem">{{ current.stem }}</h3>

            <!-- 单选 / 判断 -->
            <el-radio-group
                v-if="current.type === 'SINGLE' || current.type === 'JUDGE'"
                v-model="single"
                class="options"
            >
                <el-radio
                    v-for="(item, i) in displayOptions"
                    :key="i"
                    :value="item.value"
                    class="option"
                >
                    {{ item.label }}
                </el-radio>
            </el-radio-group>

            <!-- 多选 -->
            <el-checkbox-group v-else-if="current.type === 'MULTIPLE'" v-model="multiple" class="options">
                <el-checkbox
                    v-for="(item, i) in displayOptions"
                    :key="i"
                    :value="item.value"
                    class="option"
                >
                    {{ item.label }}
                </el-checkbox>
            </el-checkbox-group>

            <!-- 填空 -->
            <el-input
                v-else-if="current.type === 'BLANK'"
                v-model="blank"
                placeholder="请输入答案"
                style="max-width: 420px"
            />

            <!-- 主观 / 图片：先看答案再自评 -->
            <div v-else class="subjective">
                <el-button v-if="!revealed" @click="revealAnswer">显示答案</el-button>
                <div v-if="revealed" class="answer-box">
                    <div><b>参考答案：</b>{{ revealedAnswer }}</div>
                    <div v-if="revealedAnalysis" class="muted"><b>解析：</b>{{ revealedAnalysis }}</div>
                </div>
            </div>

            <div class="actions">
                <template v-if="!result">
                    <el-button
                        v-if="isObjective"
                        type="primary"
                        :loading="submitting"
                        @click="submit"
                    >
                        提交作答
                    </el-button>
                    <template v-else>
                        <el-button v-if="revealed" :loading="submitting" @click="rate(0)">忘记</el-button>
                        <el-button v-if="revealed" :loading="submitting" @click="rate(1)">模糊</el-button>
                        <el-button v-if="revealed" type="primary" :loading="submitting" @click="rate(2)">
                            记住
                        </el-button>
                    </template>
                </template>
                <el-button v-else type="primary" @click="next">下一题</el-button>
            </div>

            <el-alert
                v-if="result"
                class="result"
                :type="result.score >= 1 ? 'success' : result.score > 0 ? 'warning' : 'error'"
                :closable="false"
                :title="`得分 ${result.score} · 下次复习 ${result.nextDueDate}`"
            >
                <div>正确答案：{{ (result.correctAnswer || []).join('、') }}</div>
                <div v-if="result.analysis">解析：{{ result.analysis }}</div>
            </el-alert>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { getDueQuestions, rateQuestion, submitQuestionAnswer } from '@/api';
import { http } from '@/utils/request';

const router = useRouter();

const loading = ref(false);
const submitting = ref(false);
const cards = ref<any[]>([]);
const pending = ref<number[]>([]);
const index = ref(0);
const single = ref('');
const multiple = ref<string[]>([]);
const blank = ref('');
const result = ref<any>(null);
const revealed = ref(false);
const revealedAnswer = ref('');
const revealedAnalysis = ref('');
const startAt = ref(Date.now());

const current = computed(() => cards.value[index.value]);
const isObjective = computed(() =>
    ['SINGLE', 'MULTIPLE', 'JUDGE', 'BLANK'].includes(current.value?.type)
);

const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F'];
const displayOptions = computed(() => {
    if (!current.value) return [];
    if (current.value.type === 'JUDGE') {
        return [
            { value: '正确', label: '正确' },
            { value: '错误', label: '错误' }
        ];
    }
    return (current.value.options || []).map((text: string, i: number) => ({
        value: LETTERS[i],
        label: `${LETTERS[i]}. ${text}`
    }));
});

const typeMap: Record<string, string> = {
    SINGLE: '单选',
    MULTIPLE: '多选',
    JUDGE: '判断',
    BLANK: '填空',
    SUBJECTIVE: '主观',
    IMAGE: '图片'
};
const typeText = (type: string) => typeMap[type] || type;

const load = async () => {
    loading.value = true;
    try {
        const data = await getDueQuestions();
        cards.value = data.questions || [];
        pending.value = data.pendingKnowledgeIds || [];
        index.value = 0;
        resetCard();
    } finally {
        loading.value = false;
    }
};

const resetCard = () => {
    single.value = '';
    multiple.value = [];
    blank.value = '';
    result.value = null;
    revealed.value = false;
    revealedAnswer.value = '';
    revealedAnalysis.value = '';
    startAt.value = Date.now();
};

const buildAnswer = () => {
    if (current.value.type === 'MULTIPLE') return [...multiple.value].sort();
    if (current.value.type === 'BLANK') return [blank.value];
    return [single.value];
};

const submit = async () => {
    const answer = buildAnswer();
    if (answer.length === 0 || answer[0] === '') {
        ElMessage.warning('请先作答');
        return;
    }
    submitting.value = true;
    try {
        result.value = await submitQuestionAnswer(current.value.questionId, answer, Date.now() - startAt.value);
    } finally {
        submitting.value = false;
    }
};

/** 主观题要先拿到答案才能自评，这里单独查一次题目详情 */
const revealAnswer = async () => {
    const detail = await http.get(`/questions/${current.value.questionId}`);
    revealedAnswer.value = (detail.answer || []).join('、');
    revealedAnalysis.value = detail.analysis || '';
    revealed.value = true;
};

const rate = async (rating: number) => {
    submitting.value = true;
    try {
        result.value = await rateQuestion(current.value.questionId, rating, Date.now() - startAt.value);
    } finally {
        submitting.value = false;
    }
};

const next = () => {
    index.value += 1;
    resetCard();
};

onMounted(load);
</script>

<style scoped>
.page {
    padding: 20px;
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
.stem {
    margin: 6px 0 18px;
    line-height: 1.6;
}
.options {
    display: flex;
    flex-direction: column;
    gap: 10px;
}
.option {
    height: auto;
    margin-right: 0;
}
.actions {
    margin-top: 22px;
}
.result {
    margin-top: 18px;
}
.answer-box {
    margin-top: 10px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
    line-height: 1.8;
}
</style>
