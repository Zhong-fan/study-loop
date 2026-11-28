<template>
    <div class="mx-auto max-w-5xl">
        <div class="mb-4 flex flex-wrap items-center gap-2">
            <h1 class="mr-auto text-base font-semibold text-ink-strong">真题</h1>
            <el-select v-model="courseId" placeholder="选择课程" style="width: 180px" @change="load">
                <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <button
                type="button"
                class="rounded-md bg-brand px-3 py-2 text-sm text-white disabled:opacity-40"
                :disabled="!courseId"
                @click="openCreate"
            >
                新建试卷
            </button>
        </div>

        <div class="overflow-hidden rounded-xl border border-line bg-surface">
            <div class="overflow-x-auto">
                <table class="w-full min-w-[640px] text-sm">
                    <thead class="bg-surface-2 text-xs text-ink-muted">
                        <tr>
                            <th class="px-4 py-2.5 text-left font-medium">试卷</th>
                            <th class="w-20 px-3 py-2.5 text-left font-medium">年份</th>
                            <th class="w-40 px-3 py-2.5 text-left font-medium">学校</th>
                            <th class="w-20 px-3 py-2.5 text-left font-medium">题目数</th>
                            <th class="w-52 px-3 py-2.5 text-right font-medium">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in exams" :key="row.id" class="border-t border-line-soft">
                            <td class="px-4 py-3 font-medium text-ink">{{ row.title }}</td>
                            <td class="px-3 py-3 text-xs text-ink-muted">{{ row.examYear || '—' }}</td>
                            <td class="px-3 py-3 text-xs text-ink-muted">{{ row.school || '—' }}</td>
                            <td class="px-3 py-3 text-xs text-ink-muted">{{ row.itemCount }}</td>
                            <td class="px-3 py-3 text-right">
                                <button type="button" class="mr-2 text-xs text-brand hover:underline" @click="openImport(row)">导入题目</button>
                                <button type="button" class="mr-2 text-xs text-ink-muted hover:underline" @click="openDistribution(row)">看分布</button>
                                <button type="button" class="text-xs text-danger hover:underline" @click="remove(row)">删除</button>
                            </td>
                        </tr>
                        <tr v-if="!exams.length">
                            <td colspan="5" class="px-4 py-12 text-center text-sm text-ink-muted">
                                还没有真题，先新建一份试卷
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>


        <!-- 新建 -->
        <el-dialog v-model="createVisible" title="新建试卷" width="460">
            <el-form label-width="80px">
                <el-form-item label="名称" required>
                    <el-input v-model="form.title" placeholder="例如：2024 春季期末" />
                </el-form-item>
                <el-form-item label="年份">
                    <el-input-number v-model="form.examYear" :min="2000" :max="2100" />
                </el-form-item>
                <el-form-item label="学校">
                    <el-input v-model="form.school" placeholder="可不填" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="createVisible = false">取消</el-button>
                <el-button type="primary" @click="submitCreate">创建</el-button>
            </template>
        </el-dialog>

        <!-- 导入 -->
        <el-dialog v-model="importVisible" title="从资料导入题目" width="520">
            <p class="hint">选择一个已解析的资料，系统会把里面的题目抽出来挂到这张试卷上。</p>
            <el-select v-model="importMaterialId" placeholder="选择资料" style="width: 100%">
                <el-option v-for="m in materials" :key="m.id" :label="m.title" :value="m.id" />
            </el-select>
            <p v-if="importResult" class="hint result">
                导入 {{ importResult.imported }} 题，跳过 {{ importResult.skipped }} 题
            </p>
            <template #footer>
                <el-button @click="importVisible = false">关闭</el-button>
                <el-button type="primary" :loading="importing" :disabled="!importMaterialId" @click="submitImport">
                    开始导入
                </el-button>
            </template>
        </el-dialog>

        <!-- 分布 -->
        <el-dialog v-model="distributionVisible" title="真题知识点分布" width="640">
            <div v-if="distribution" class="summary">
                共 {{ distribution.totalQuestions }} 题，已关联知识点 {{ distribution.linkedQuestions }} 题，总分
                {{ distribution.totalScore }}
            </div>
            <el-table :data="distribution?.items || []" size="small" empty-text="还没有关联到知识点">
                <el-table-column prop="knowledgeName" label="知识点" min-width="200" />
                <el-table-column prop="count" label="出现次数" width="100" />
                <el-table-column prop="score" label="累计分值" width="100" />
            </el-table>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
    createExam,
    deleteExam,
    getCourses,
    getExamDistribution,
    getExams,
    getMaterials,
    importExam
} from '@/api';

const courses = ref<any[]>([]);
const courseId = ref<number | null>(null);
const exams = ref<any[]>([]);
const loading = ref(false);

const createVisible = ref(false);
const importVisible = ref(false);
const distributionVisible = ref(false);
const importing = ref(false);
const importResult = ref<any>(null);
const importMaterialId = ref<number | null>(null);
const materials = ref<any[]>([]);
const currentExam = ref<any>(null);
const distribution = ref<any>(null);

const form = reactive({ title: '', examYear: new Date().getFullYear(), school: '' });

const load = async () => {
    if (!courseId.value) {
        exams.value = [];
        return;
    }
    loading.value = true;
    try {
        exams.value = await getExams(courseId.value);
    } finally {
        loading.value = false;
    }
};

const openCreate = () => {
    form.title = '';
    form.examYear = new Date().getFullYear();
    form.school = '';
    createVisible.value = true;
};

const submitCreate = async () => {
    if (!form.title.trim()) {
        ElMessage.warning('试卷名称不能为空');
        return;
    }
    await createExam({ courseId: courseId.value, ...form });
    createVisible.value = false;
    ElMessage.success('已创建');
    await load();
};

const openImport = async (row: any) => {
    currentExam.value = row;
    importResult.value = null;
    importMaterialId.value = null;
    materials.value = await getMaterials(courseId.value as number);
    importVisible.value = true;
};

const submitImport = async () => {
    if (!currentExam.value || !importMaterialId.value) {
        return;
    }
    importing.value = true;
    try {
        importResult.value = await importExam(currentExam.value.id, importMaterialId.value);
        ElMessage.success(`导入 ${importResult.value.imported} 题`);
        await load();
    } finally {
        importing.value = false;
    }
};

const openDistribution = async (row: any) => {
    distribution.value = await getExamDistribution(row.id);
    distributionVisible.value = true;
};

const remove = async (row: any) => {
    await ElMessageBox.confirm(`确定删除「${row.title}」？题目也会一起删掉。`, '删除试卷', { type: 'warning' });
    await deleteExam(row.id);
    ElMessage.success('已删除');
    await load();
};

onMounted(async () => {
    const data = await getCourses({ page: 1, size: 50 });
    courses.value = data.records || [];
    courseId.value = courses.value[0]?.id ?? null;
    await load();
});
</script>

<style scoped>
.block {
    margin-bottom: var(--sl-s4);
}
.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--sl-s3);
}
.card-header .hint {
    margin-left: var(--sl-s2);
    color: var(--sl-muted);
    font-size: var(--sl-fs-xs);
}
.actions {
    display: flex;
    gap: var(--sl-s2);
}
.hint {
    color: var(--sl-muted);
    font-size: var(--sl-fs-sm);
    line-height: 1.7;
}
.result {
    color: var(--sl-success);
    margin-top: var(--sl-s2);
}
.summary {
    margin-bottom: var(--sl-s3);
    color: var(--sl-text);
}
</style>
