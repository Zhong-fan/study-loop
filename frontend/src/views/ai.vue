<template>
    <div class="page">
        <el-card>
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
                        v-model="form.knowledgeId"
                        placeholder="先选课程"
                        style="width: 220px"
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
                    </el-select>
                </el-form-item>

                <el-form-item label="数量">
                    <el-input-number v-model="form.count" :min="1" :max="10" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" :loading="generating" :disabled="!aiReady" @click="generate">
                        生成题目
                    </el-button>
                </el-form-item>
            </el-form>

            <div class="hint">
                生成的题目只是草稿：勾选后点「存入题库」才会真正写库，并可到「题库」页继续编辑。
            </div>
        </el-card>

        <el-card class="block">
            <template #header>草稿（{{ drafts.length }}）</template>
            <el-table :data="drafts" border size="small" @selection-change="onSelectionChange">
                <el-table-column type="selection" width="44" />
                <el-table-column prop="stem" label="题干" show-overflow-tooltip />
                <el-table-column label="选项" width="280">
                    <template #default="{ row }">
                        <span v-for="(op, i) in row.options" :key="i" class="option">
                            {{ String.fromCharCode(65 + i) }}. {{ op }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="答案" width="90">
                    <template #default="{ row }">{{ row.answer.join(',') }}</template>
                </el-table-column>
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
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
    aiGenerateQuestions,
    createQuestion,
    getAiStatus,
    getCourses,
    getKnowledgeByCourse
} from '@/api';

const courses = ref<any[]>([]);
const knowledges = ref<any[]>([]);
const drafts = ref<any[]>([]);
const selected = ref<any[]>([]);
const generating = ref(false);
const saving = ref(false);

const aiReady = ref(false);
const aiModel = ref('');

const form = reactive<any>({
    courseId: null,
    knowledgeId: null,
    type: 'SINGLE',
    count: 3
});

const onSelectionChange = (rows: any[]) => {
    selected.value = rows;
};

const onCourseChange = async () => {
    form.knowledgeId = null;
    knowledges.value = form.courseId ? await getKnowledgeByCourse(form.courseId) : [];
};

const generate = async () => {
    if (!form.knowledgeId) {
        ElMessage.warning('请选择知识点');
        return;
    }
    const knowledge = knowledges.value.find((k) => k.id === form.knowledgeId);

    generating.value = true;
    try {
        drafts.value = await aiGenerateQuestions({
            knowledgeId: form.knowledgeId,
            knowledgeName: knowledge?.name || '',
            count: form.count,
            type: form.type
        });
        if (drafts.value.length === 0) {
            ElMessage.warning('模型没有返回题目，换个知识点试试');
        }
    } finally {
        generating.value = false;
    }
};

const saveToBank = async () => {
    const rows = selected.value.length > 0 ? selected.value : drafts.value;
    saving.value = true;
    try {
        for (const row of rows) {
            await createQuestion({
                courseId: form.courseId,
                chapterId: knowledges.value.find((k) => k.id === form.knowledgeId)?.chapterId,
                knowledgeId: form.knowledgeId,
                type: form.type,
                stem: row.stem,
                options: row.options,
                answer: row.answer,
                analysis: row.analysis,
                difficulty: 3
            });
        }
        ElMessage.success(`已存入 ${rows.length} 道题`);
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
.page {
    padding: 20px;
}
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.block {
    margin-top: 16px;
}
.hint {
    color: #909399;
    font-size: 12px;
}
.option {
    display: block;
    font-size: 12px;
    color: #606266;
}
.actions {
    margin-top: 12px;
}
</style>
