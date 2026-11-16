<template>
    <div class="page">
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
                        <el-button type="primary" @click="openCreate">新建题目</el-button>
                    </div>
                </div>
            </template>

            <el-table :data="rows" v-loading="loading" border>
                <el-table-column prop="stem" label="题干" show-overflow-tooltip />
                <el-table-column label="题型" width="90">
                    <template #default="{ row }">{{ typeText(row.type) }}</template>
                </el-table-column>
                <el-table-column prop="difficulty" label="难度" width="70" />
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? '已掌握' : '待复习' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="创建时间" width="180" />
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button link type="danger" @click="remove(row)">删除</el-button>
                    </template>
                </el-table-column>
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

        <el-dialog v-model="formVisible" title="新建题目" width="620px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="课程" required>
                    <el-select v-model="form.courseId" placeholder="选择课程" style="width: 100%">
                        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
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
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createQuestion, deleteQuestion, getCourses, getQuestions } from '@/api';

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
const formVisible = ref(false);
const form = reactive<any>({
    courseId: null,
    type: 'SINGLE',
    stem: '',
    optionsText: '',
    answerText: '',
    analysis: '',
    difficulty: 3
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

const openCreate = () => {
    form.courseId = courses.value[0]?.id ?? null;
    form.type = 'SINGLE';
    form.stem = '';
    form.optionsText = '';
    form.answerText = '';
    form.analysis = '';
    form.difficulty = 3;
    formVisible.value = true;
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
        await createQuestion({
            courseId: form.courseId,
            type: form.type,
            stem: form.stem,
            options: ['SINGLE', 'MULTIPLE'].includes(form.type) ? options : [],
            answer,
            analysis: form.analysis,
            difficulty: form.difficulty
        });
        ElMessage.success('已保存，已自动生成复习卡');
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
    await load();
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
.pager {
    margin-top: 14px;
    justify-content: flex-end;
}
.hint {
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
}
</style>
