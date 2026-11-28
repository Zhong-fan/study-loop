<template>
    <div class="page sl-page">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>题库</span>
                    <div>
                        <el-select v-model="filter.courseId" placeholder="全部课程" clearable style="width: 160px">
                            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                        </el-select>
                        <el-select v-model="filter.type" placeholder="全部题型" clearable style="width: 130px; margin-left: 8px">
                            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
                        </el-select>
                        <el-button style="margin-left: 8px" @click="load">查询</el-button>
                        <el-button @click="openImport">导入</el-button>
                        <el-button @click="exportAll">导出</el-button>
                        <el-button type="primary" @click="openCreate">新建题目</el-button>
                    </div>
                </div>
            </template>

            <el-table :data="rows" v-loading="loading" border>
                <el-table-column prop="stem" label="题干" show-overflow-tooltip />
                <el-table-column label="题型" width="90">
                    <template #default="{ row }">{{ typeText(row.type) }}</template>
                </el-table-column>
                <el-table-column label="来源" width="110">
                    <template #default="{ row }">
                        <span v-if="row.materialId">资料第 {{ row.pageNo }} 页</span>
                        <span v-else class="hint">—</span>
                    </template>
                </el-table-column>
                <el-table-column prop="difficulty" label="难度" width="70" />
                <el-table-column prop="createdAt" label="创建时间" width="180" />
                <el-table-column label="操作" width="210">
                    <template #default="{ row }">
                        <el-button
                            v-if="row.type === 'SHORT' || row.type === 'COMPOSITE'"
                            link
                            type="warning"
                            @click="openGrade(row)"
                        >
                            AI 判分
                        </el-button>
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="danger" @click="remove(row)">删除</el-button>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="题库还是空的" :image-size="80">
                        <el-button type="primary" @click="openCreate">新建题目</el-button>
                        <el-button @click="router.push('/ai')">用 AI 出题</el-button>
                    </el-empty>
                </template>
            </el-table>

            <el-pagination
                class="pager"
                layout="total, prev, pager, next"
                :total="total"
                :page-size="size"
                :current-page="page"
                @current-change="onPageChange"
            />
        </el-card>

        <el-dialog v-model="formVisible" :title="form.id ? '编辑题目' : '新建题目'" width="620px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="课程" required>
                    <el-select
                        v-model="form.courseId"
                        placeholder="选择课程"
                        style="width: 100%"
                        :disabled="!!form.id"
                        @change="onCourseChange"
                    >
                        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                    <div v-if="form.id" class="hint">题目已生成复习卡，不允许换课程</div>
                </el-form-item>

                <el-form-item label="章节">
                    <el-select v-model="form.chapterId" placeholder="可不选" clearable style="width: 100%" @change="loadKnowledges">
                        <el-option v-for="ch in chapters" :key="ch.id" :label="ch.title" :value="ch.id" />
                    </el-select>
                </el-form-item>

                <el-form-item label="知识点">
                    <el-select
                        v-model="form.knowledgeId"
                        placeholder="先选章节"
                        clearable
                        :disabled="!form.chapterId"
                        style="width: 100%"
                    >
                        <el-option v-for="k in knowledges" :key="k.id" :label="k.name" :value="k.id" />
                    </el-select>
                </el-form-item>

                <el-form-item label="题型" required>
                    <el-select v-model="form.type" style="width: 100%">
                        <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
                    </el-select>
                </el-form-item>

                <el-form-item label="题干" required>
                    <el-input v-model="form.stem" type="textarea" :rows="3" />
                </el-form-item>

                <el-alert
                    v-if="form.materialId"
                    class="source-tip"
                    type="info"
                    :closable="false"
                    :title="`来源：资料第 ${form.pageNo} 页`"
                >
                    <div class="quote">{{ form.quote }}</div>
                </el-alert>

                <template v-if="form.type === 'SINGLE' || form.type === 'MULTIPLE'">
                    <el-form-item label="选项">
                        <el-input
                            v-model="form.optionsText"
                            type="textarea"
                            :rows="4"
                            placeholder="每行一个选项，不需要写 A. B. 前缀"
                        />
                    </el-form-item>
                </template>

                <el-form-item label="答案" required>
                    <el-input
                        v-model="form.answerText"
                        :placeholder="answerPlaceholder"
                    />
                    <div class="hint">{{ answerHint }}</div>
                </el-form-item>

                <el-form-item label="解析">
                    <el-input v-model="form.analysis" type="textarea" :rows="2" />
                </el-form-item>

                <el-form-item label="难度">
                    <el-rate v-model="form.difficulty" :max="5" />
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="gradeVisible" title="AI 判分（按评分点）" width="680px">
            <p class="grade-stem">{{ gradeQuestion?.stem }}</p>
            <el-input
                v-model="gradeAnswer"
                type="textarea"
                :rows="5"
                placeholder="把学生的作答粘贴进来"
            />
            <div class="grade-actions">
                <el-button type="primary" :loading="grading" :disabled="!gradeAnswer.trim()" @click="submitGrade">
                    开始判分
                </el-button>
            </div>

            <div v-if="gradeResult" class="grade-result">
                <el-alert
                    :type="gradeResult.score >= 0.6 ? 'success' : gradeResult.score > 0 ? 'warning' : 'error'"
                    :closable="false"
                    :title="`得分 ${Math.round((gradeResult.score || 0) * 100)} 分`"
                    :description="gradeResult.comment"
                />
                <el-table :data="gradeResult.points || []" size="small" class="grade-points" empty-text="本次没有返回要点明细">
                    <el-table-column prop="point" label="评分点" min-width="260" />
                    <el-table-column label="是否命中" width="100">
                        <template #default="{ row }">
                            <el-tag :type="row.hit ? 'success' : 'danger'" size="small">
                                {{ row.hit ? '命中' : '未命中' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="comment" label="说明" min-width="180" />
                </el-table>
            </div>
        </el-dialog>

        <el-dialog v-model="importVisible" title="导入题目" width="620px">
            <el-form label-width="90px">
                <el-form-item label="课程">
                    <el-select v-model="importCourseId" placeholder="选择课程" style="width: 100%">
                        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="Excel">
                    <input class="import-file" type="file" accept=".xlsx,.xls" @change="onImportFile" />
                    <div class="hint">支持 .xlsx / .xls，单次最多 2000 行</div>
                </el-form-item>
                <el-form-item label="错误处理">
                    <div class="error-mode">
                        <el-checkbox v-model="skipInvalid">跳过错误行，只导入正确的行</el-checkbox>
                        <div class="hint">默认整批导入：任何一行有错，整批都不写入</div>
                    </div>
                </el-form-item>
            </el-form>

            <el-alert
                v-if="importResult"
                :type="importResult.aborted ? 'error' : importResult.failed ? 'warning' : 'success'"
                :closable="false"
                :title="importSummary"
            />
            <el-table
                v-if="importResult?.errors?.length"
                :data="importResult.errors"
                size="small"
                max-height="240"
                class="import-errors"
            >
                <el-table-column prop="row" label="行" width="60" />
                <el-table-column prop="stem" label="题干" show-overflow-tooltip />
                <el-table-column prop="message" label="原因" width="220" show-overflow-tooltip />
            </el-table>

            <template #footer>
                <el-button @click="downloadTemplate">下载模板</el-button>
                <el-button @click="importVisible = false">关闭</el-button>
                <el-button type="primary" :loading="importing" :disabled="!importFile" @click="doImport">
                    开始导入
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { downloadFile } from '@/utils/request';
import { aiGradeQuestion, createQuestion, deleteQuestion, downloadQuestionTemplate, exportQuestions, getChapters, getCourses, getKnowledgeByChapter, getQuestion, getQuestions, importQuestions, updateQuestion } from '@/api';

const route = useRoute();
const router = useRouter();

const types = [
    { value: 'SINGLE', label: '单选' },
    { value: 'MULTIPLE', label: '多选' },
    { value: 'JUDGE', label: '判断' },
    { value: 'BLANK', label: '填空' },
    { value: 'SUBJECTIVE', label: '主观' },
    { value: 'IMAGE', label: '图片' }
];

const loading = ref(false);
const saving = ref(false);
const rows = ref<any[]>([]);
const courses = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const filter = reactive<any>({ courseId: null, type: null });

const importVisible = ref(false);
const importCourseId = ref<number | null>(null);
const importFile = ref<File | null>(null);
const importing = ref(false);
const skipInvalid = ref(false);
const importResult = ref<any>(null);

const importSummary = computed(() => {
    if (!importResult.value) return '';
    const r = importResult.value;
    if (r.aborted) return `共 ${r.total} 行，${r.failed} 行有错误，整批未写入`;
    return `共 ${r.total} 行，成功 ${r.success} 行，失败 ${r.failed} 行`;
});

const openImport = () => {
    importResult.value = null;
    importFile.value = null;
    importCourseId.value = filter.courseId || courses.value[0]?.id || null;
    importVisible.value = true;
};

const onImportFile = (event: Event) => {
    const input = event.target as HTMLInputElement;
    importFile.value = input.files?.[0] || null;
};

const doImport = async () => {
    if (!importCourseId.value || !importFile.value) {
        ElMessage.warning('请选择课程和文件');
        return;
    }
    importing.value = true;
    try {
        const form = new FormData();
        form.append('courseId', String(importCourseId.value));
        form.append('skipInvalid', String(skipInvalid.value));
        form.append('file', importFile.value);
        importResult.value = await importQuestions(form);
        if (!importResult.value.aborted) {
            await load();
        }
    } finally {
        importing.value = false;
    }
};

const downloadTemplate = () => downloadQuestionTemplate();

const exportAll = () => exportQuestions({ courseId: filter.courseId, type: filter.type });
const formVisible = ref(false);
const chapters = ref<any[]>([]);
const knowledges = ref<any[]>([]);
const form = reactive<any>({
    id: null,
    courseId: null,
    chapterId: null,
    type: 'SINGLE',
    stem: '',
    optionsText: '',
    answerText: '',
    analysis: '',
    difficulty: 3,
    materialId: null,
    pageNo: null,
    quote: ''
});

const typeMap: Record<string, string> = {
    SINGLE: '单选',
    MULTIPLE: '多选',
    JUDGE: '判断',
    BLANK: '填空',
    SUBJECTIVE: '主观',
    IMAGE: '图片'
};
const typeText = (t: string) => typeMap[t] || t;

const answerPlaceholder = computed(() => {
    if (form.type === 'SINGLE') return '例如：A';
    if (form.type === 'MULTIPLE') return '例如：A,C';
    if (form.type === 'JUDGE') return '正确 或 错误';
    return '可填多个可接受答案，用逗号分隔';
});

const answerHint = computed(() =>
    ['SINGLE', 'MULTIPLE'].includes(form.type) ? '填选项字母，多选用英文逗号分隔' : ''
);

const load = async () => {
    loading.value = true;
    try {
        const data = await getQuestions({
            page: page.value,
            size: size.value,
            courseId: filter.courseId || undefined,
            type: filter.type || undefined
        });
        rows.value = data.records;
        total.value = data.total;
    } finally {
        loading.value = false;
    }
};

const onPageChange = (p: number) => {
    page.value = p;
    load();
};

const loadKnowledges = async () => {
    form.knowledgeId = null;
    knowledges.value = form.chapterId ? await getKnowledgeByChapter(form.chapterId) : [];
};

const openCreate = () => {
    form.id = null;
    form.courseId = courses.value[0]?.id ?? null;
    form.chapterId = null;
    form.knowledgeId = null;
    knowledges.value = [];
    form.type = 'SINGLE';
    form.stem = '';
    form.optionsText = '';
    form.answerText = '';
    form.analysis = '';
    form.difficulty = 3;
    form.materialId = null;
    form.pageNo = null;
    form.quote = '';
    formVisible.value = true;
    loadChapters();
};

/** 编辑：先拉详情，把选项和答案还原成文本框里的写法 */
const gradeVisible = ref(false);
const gradeQuestion = ref<any>(null);
const gradeAnswer = ref('');
const gradeResult = ref<any>(null);
const grading = ref(false);

const openGrade = (row: any) => {
    gradeQuestion.value = row;
    gradeAnswer.value = '';
    gradeResult.value = null;
    gradeVisible.value = true;
};

const submitGrade = async () => {
    if (!gradeQuestion.value) {
        return;
    }
    grading.value = true;
    try {
        gradeResult.value = await aiGradeQuestion(gradeQuestion.value.id, gradeAnswer.value.trim());
    } finally {
        grading.value = false;
    }
};

const openEdit = async (row: any) => {
    const detail = await getQuestion(row.id);

    form.id = detail.id;
    form.courseId = detail.courseId;
    form.chapterId = detail.chapterId || null;
    form.knowledgeId = detail.knowledgeId || null;
    form.type = detail.type;
    form.stem = detail.stem;
    form.optionsText = (detail.options || []).join('\n');
    form.answerText = (detail.answer || []).join(',');
    form.analysis = detail.analysis || '';
    form.difficulty = detail.difficulty || 3;
    form.materialId = detail.materialId || null;
    form.pageNo = detail.pageNo || null;
    form.quote = detail.quote || '';

    formVisible.value = true;
    await loadChapters();
};

const loadChapters = async () => {
    if (!form.courseId) {
        chapters.value = [];
        return;
    }
    chapters.value = await getChapters(form.courseId);
};

const onCourseChange = () => {
    form.chapterId = null;
    loadChapters();
};

const save = async () => {
    if (!form.courseId) {
        ElMessage.warning('请选择课程');
        return;
    }
    if (!form.stem.trim()) {
        ElMessage.warning('题干不能为空');
        return;
    }

    // 选项按行拆，答案按逗号拆
    const options = form.optionsText
        .split('\n')
        .map((s: string) => s.trim())
        .filter((s: string) => s);

    let answer = form.answerText
        .split(/[,，\s]+/)
        .map((s: string) => s.trim())
        .filter((s: string) => s);

    if (['SINGLE', 'MULTIPLE'].includes(form.type)) {
        answer = answer.map((a: string) => a.toUpperCase());
    }
    if (answer.length === 0) {
        ElMessage.warning('答案不能为空');
        return;
    }

    saving.value = true;
    try {
        const payload = {
            chapterId: form.chapterId,
        knowledgeId: form.knowledgeId,
            type: form.type,
            stem: form.stem,
            options: ['SINGLE', 'MULTIPLE'].includes(form.type) ? options : [],
            answer,
            analysis: form.analysis,
            difficulty: form.difficulty
        };

        if (form.id) {
            await updateQuestion(form.id, payload);
            ElMessage.success('已保存');
        } else {
            await createQuestion({
                ...payload,
                courseId: form.courseId,
                materialId: form.materialId,
                pageNo: form.pageNo,
                quote: form.quote
            });
            ElMessage.success('已保存，已自动生成复习卡');
        }

        formVisible.value = false;
        load();
    } finally {
        saving.value = false;
    }
};

const remove = async (row: any) => {
    await ElMessageBox.confirm('删除后对应的复习卡也会失效，确定吗？', '提示', { type: 'warning' });
    await deleteQuestion(row.id);
    ElMessage.success('已删除');
    load();
};

onMounted(async () => {
    const data = await getCourses({ page: 1, size: 100 });
    courses.value = data.records;

    // 从"划词出题"跳过来时，带着资料、页码和原文
    if (route.query.quote) {
        openCreate();
        form.courseId = route.query.courseId ? Number(route.query.courseId) : form.courseId;
        form.stem = String(route.query.quote);
        form.materialId = route.query.materialId ? Number(route.query.materialId) : null;
        form.pageNo = route.query.pageNo ? Number(route.query.pageNo) : null;
        form.quote = String(route.query.quote);
    }

    await load();
});
</script>

<style scoped>
.import-file {
    display: block;
    width: 100%;
    padding: 8px 0;
    font-size: 13px;
}
.import-errors {
    margin-top: 12px;
}
.error-mode {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.grade-stem {
    margin: 0 0 12px;
    font-weight: 600;
}
.grade-actions {
    margin: 12px 0;
}
.grade-points {
    margin-top: 12px;
}
.page {
    padding: 20px;
}
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.pager {
    margin-top: 14px;
    justify-content: flex-end;
}
.hint {
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
}
.source-tip {
    margin-bottom: 16px;
}
.quote {
    margin-top: 6px;
    font-size: 12px;
    color: #606266;
    line-height: 1.6;
}
</style>
