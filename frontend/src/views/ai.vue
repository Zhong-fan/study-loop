<template>
    <div class="page sl-page">
        <el-card shadow="never">
            <template #header>
                <div class="card-header">
                    <span>AI 出题</span>
                    <el-tag :type="aiReady ? 'success' : 'info'" size="small">
                        {{ aiReady ? '已连接 · ' + aiModel : '未配置，去 .env 填 AI_API_KEY' }}
                    </el-tag>
                </div>
            </template>

            <el-form :inline="true" :model="form" label-width="80px">
                <el-form-item label="课程">
                    <el-select v-model="form.courseId" placeholder="选择课程" style="width: 180px" @change="onCourseChange">
                        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                </el-form-item>

                <el-form-item label="知识点">
                    <el-select
                        v-model="form.knowledgeIds"
                        multiple
                        collapse-tags
                        collapse-tags-tooltip
                        placeholder="先选课程"
                        style="width: 320px"
                        :disabled="!form.courseId"
                    >
                        <el-option v-for="k in knowledges" :key="k.id" :label="k.name" :value="k.id" />
                    </el-select>
                </el-form-item>

                <el-form-item label="题型">
                    <el-select v-model="form.type" style="width: 120px">
                        <el-option label="单选" value="SINGLE" />
                        <el-option label="多选" value="MULTIPLE" />
                        <el-option label="判断" value="JUDGE" />
                        <el-option label="综合题" value="COMPOSITE" />
                    </el-select>
                </el-form-item>

                <el-form-item label="数量">
                    <el-input-number v-model="form.count" :min="1" :max="5" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" :loading="generating" :disabled="!aiReady" @click="generate">
                        生成题目
                    </el-button>
                </el-form-item>
            </el-form>

            <div class="hint">
                单选/判断挂 1 个知识点，多选 2~3 个，综合题 5~8 个。出题会带上知识点的原文证据，
                没有证据的题目会被服务端拒绝，避免模型编造。
            </div>
        </el-card>

        <el-card class="block" shadow="never">
            <template #header>草稿（{{ drafts.length }}）</template>
            <el-table :data="drafts" border size="small" @selection-change="onSelectionChange">
                <el-table-column type="selection" width="44" />
                <el-table-column prop="stem" label="题干" show-overflow-tooltip />
                <el-table-column label="题型" width="90">
                    <template #default="{ row }">{{ typeText(row.type) }}</template>
                </el-table-column>
                <el-table-column label="知识点" width="80">
                    <template #default="{ row }">{{ row.knowledge?.length || 0 }} 个</template>
                </el-table-column>
                <el-table-column label="总分" width="70">
                    <template #default="{ row }">{{ row.total_score ?? row.totalScore ?? '—' }}</template>
                </el-table-column>
                <el-table-column label="依据" width="140">
                    <template #default="{ row }">
                        <span class="hint">
                            {{ evidenceText(row.evidence) }}
                        </span>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty
                        description="还没有草稿：先选课程和知识点，再点「生成题目」"
                        :image-size="80"
                    />
                </template>
            </el-table>

            <div class="actions">
                <el-button type="primary" :disabled="drafts.length === 0" :loading="saving" @click="saveToBank">
                    存入题库
                </el-button>
                <el-button :disabled="drafts.length === 0" @click="drafts = []">清空</el-button>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
    aiGenerateDrafts,
    getAiStatus,
    getCourses,
    getKnowledgeByCourse,
    getKnowledgeEvidence,
    saveDrafts
} from '@/api';

const courses = ref<any[]>([]);
const knowledges = ref<any[]>([]);
const evidences = ref<any[]>([]);
const drafts = ref<any[]>([]);
const selected = ref<any[]>([]);
const generating = ref(false);
const saving = ref(false);

const aiReady = ref(false);
const aiModel = ref('');

const form = reactive<any>({
    courseId: null,
    knowledgeIds: [],
    type: 'SINGLE',
    count: 2
});

const selectedKnowledges = computed(() =>
    knowledges.value.filter((k) => form.knowledgeIds.includes(k.id))
);

const typeText = (type: string) =>
    ({ SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', BLANK: '填空', SUBJECTIVE: '简答', COMPOSITE: '综合题' } as any)[type] || type;

const evidenceText = (items: any[]) => {
    if (!items || items.length === 0) {
        return '—';
    }
    const first = items[0];
    return `第 ${first.page} 页 · ${items.length} 条`;
};

const onSelectionChange = (rows: any[]) => {
    selected.value = rows;
};

const onCourseChange = async () => {
    form.knowledgeIds = [];
    drafts.value = [];
    if (!form.courseId) {
        knowledges.value = [];
        evidences.value = [];
        return;
    }
    const [ks, es] = await Promise.all([
        getKnowledgeByCourse(form.courseId),
        getKnowledgeEvidence(form.courseId)
    ]);
    knowledges.value = ks;
    evidences.value = es;
};

const generate = async () => {
    if (form.knowledgeIds.length === 0) {
        ElMessage.warning('请选择知识点');
        return;
    }
    if (form.type === 'COMPOSITE' && form.knowledgeIds.length < 5) {
        ElMessage.warning('综合题至少要选 5 个知识点');
        return;
    }

    const payload = {
        type: form.type,
        count: form.count,
        materialId: evidences.value.find((e) => form.knowledgeIds.includes(e.knowledgeId))?.materialId ?? null,
        knowledge: selectedKnowledges.value.map((k) => ({ id: k.id, name: k.name })),
        evidence: evidences.value
            .filter((e) => form.knowledgeIds.includes(e.knowledgeId))
            .map((e) => ({
                knowledgeId: e.knowledgeId,
                page: e.pageNo,
                quote: e.quote,
                signal: e.signalType
            }))
    };

    generating.value = true;
    try {
        drafts.value = await aiGenerateDrafts(payload);
        if (drafts.value.length === 0) {
            ElMessage.warning('模型没有返回题目，换一批知识点试试');
        }
    } finally {
        generating.value = false;
    }
};

const saveToBank = async () => {
    const rows = selected.value.length > 0 ? selected.value : drafts.value;
    const chapterId = selectedKnowledges.value[0]?.chapterId ?? null;
    saving.value = true;
    try {
        const ids = await saveDrafts(form.courseId, chapterId, rows);
        ElMessage.success(`已存入 ${ids.length} 道题，到「题库」页可以继续编辑`);
        drafts.value = [];
        selected.value = [];
    } finally {
        saving.value = false;
    }
};

onMounted(async () => {
    const status = await getAiStatus();
    aiReady.value = !!status.usable;
    aiModel.value = status.model || '';

    const data = await getCourses({ page: 1, size: 100 });
    courses.value = data.records;
});
</script>

<style scoped>
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.block {
    margin-top: var(--sl-s4);
}
.actions {
    margin-top: var(--sl-s4);
}
.hint {
    margin-top: var(--sl-s3);
}
</style>
