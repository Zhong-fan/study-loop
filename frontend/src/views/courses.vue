<template>
    <div class="mx-auto max-w-5xl">
        <div class="mb-4 flex flex-wrap items-center gap-2">
            <h1 class="mr-auto text-base font-semibold text-ink-strong">课程与资料</h1>
            <input
                v-model="keyword"
                placeholder="按名称搜索"
                class="h-9 w-52 rounded-md border border-line bg-reader px-3 text-sm outline-none focus:border-brand"
                @keyup.enter="load"
            />
            <button type="button" class="rounded-md border border-line px-3 py-2 text-sm hover:bg-surface-2" @click="load">
                查询
            </button>
            <button type="button" class="rounded-md bg-brand px-3 py-2 text-sm text-white hover:opacity-90" @click="openCreate">
                新建课程
            </button>
        </div>

        <div class="overflow-hidden rounded-xl border border-line bg-surface">
            <div class="overflow-x-auto">
                <table class="w-full min-w-[640px] text-sm">
                    <thead class="bg-surface-2 text-xs text-ink-muted">
                        <tr>
                            <th class="w-52 px-4 py-2.5 text-left font-medium">课程</th>
                            <th class="px-3 py-2.5 text-left font-medium">简介</th>
                            <th class="w-40 px-3 py-2.5 text-left font-medium">创建时间</th>
                            <th class="w-44 px-3 py-2.5 text-right font-medium">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in courses" :key="row.id" class="border-t border-line-soft">
                            <td class="px-4 py-3 font-medium text-ink">{{ row.name }}</td>
                            <td class="max-w-[320px] truncate px-3 py-3 text-ink-muted">{{ row.description || '—' }}</td>
                            <td class="px-3 py-3 text-xs text-ink-muted">{{ row.createdAt }}</td>
                            <td class="px-3 py-3 text-right">
                                <button type="button" class="mr-2 text-xs text-brand hover:underline" @click="openDetail(row)">
                                    章节与资料
                                </button>
                                <button type="button" class="mr-2 text-xs text-ink-muted hover:underline" @click="openEdit(row)">编辑</button>
                                <button type="button" class="text-xs text-danger hover:underline" @click="remove(row)">删除</button>
                            </td>
                        </tr>
                        <tr v-if="!courses.length">
                            <td colspan="4" class="px-4 py-12 text-center text-sm text-ink-muted">
                                <p>还没有课程</p>
                                <p class="mt-1 text-xs">先建一门，再上传资料</p>
                                <button type="button" class="mt-3 rounded-md bg-brand px-3 py-1.5 text-xs text-white" @click="openCreate">
                                    新建课程
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div class="flex items-center justify-between border-t border-line px-4 py-3 text-xs text-ink-muted">
                <span>共 {{ total }} 门</span>
                <div class="flex items-center gap-2">
                    <button
                        type="button"
                        class="rounded-md border border-line px-2.5 py-1 disabled:opacity-40"
                        :disabled="page <= 1"
                        @click="onPageChange(page - 1)"
                    >
                        上一页
                    </button>
                    <span>{{ page }}</span>
                    <button
                        type="button"
                        class="rounded-md border border-line px-2.5 py-1 disabled:opacity-40"
                        :disabled="page * size >= total"
                        @click="onPageChange(page + 1)"
                    >
                        下一页
                    </button>
                </div>
            </div>
        </div>


        <!-- 新建 / 编辑课程 -->
        <el-dialog v-model="formVisible" :title="form.id ? '编辑课程' : '新建课程'" width="480px">
            <el-form :model="form" label-width="70px">
                <el-form-item label="名称" required>
                    <el-input v-model="form.name" maxlength="50" />
                </el-form-item>
                <el-form-item label="简介">
                    <el-input v-model="form.description" type="textarea" maxlength="200" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>

        <!-- 章节与资料 -->
        <el-drawer v-model="detailVisible" :title="current?.name" size="620px">
            <h4>章节</h4>
            <div class="row">
                <el-input v-model="chapterTitle" placeholder="新章节名称" style="width: 220px" />
                <el-button type="primary" @click="addChapter">添加</el-button>
            </div>
            <el-table
                :data="chapters"
                size="small"
                border
                highlight-current-row
                style="margin-top: 10px"
                @row-click="selectChapter"
            >
                <el-table-column prop="title" label="章节" />
                <el-table-column prop="sort" label="排序" width="80" />
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button link type="danger" @click.stop="removeChapter(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <h4 style="margin-top: 24px">知识点（节）</h4>
            <div class="row">
                <span class="muted" style="width: 150px">
                    {{ activeChapter ? activeChapter.title : '先点上面某一行章节' }}
                </span>
                <el-input
                    v-model="knowledgeName"
                    placeholder="新知识点名称"
                    style="width: 200px"
                    :disabled="!activeChapter"
                />
                <el-button type="primary" :disabled="!activeChapter" @click="addKnowledge">添加</el-button>
            </div>
            <el-table :data="knowledges" size="small" border style="margin-top: 10px">
                <el-table-column prop="name" label="知识点" />
                <el-table-column prop="type" label="类型" width="90" />
                <el-table-column prop="importance" label="重要度" width="80" />
                <el-table-column prop="pageNo" label="页码" width="70" />
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button link type="danger" @click="removeKnowledge(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <h4 style="margin-top: 24px">资料</h4>
            <el-upload :show-file-list="false" :http-request="doUpload" style="margin-bottom: 10px">
                <el-button type="primary">上传课件</el-button>
                <template #tip>
                    <span class="muted">支持 pdf / ppt / doc / 图片，单个不超过 50MB</span>
                </template>
            </el-upload>
            <el-table :data="materials" size="small" border>
                <el-table-column prop="title" label="名称" show-overflow-tooltip />
                <el-table-column prop="fileType" label="类型" width="80" />
                <el-table-column label="操作" width="350">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="preview(row)">预览</el-button>
                        <el-button link type="primary" @click="openPages(row)">文字</el-button>
                        <el-button link type="primary" @click="download(row)">下载</el-button>
                        <el-button link type="primary" :loading="parsingId === row.id" @click="parseOne(row)">
                            解析
                        </el-button>
                        <el-button link type="primary" :loading="extractingId === row.id" @click="extractOne(row)">
                            抽知识点
                        </el-button>
                        <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-drawer>

        <!-- 资料文字：PDF 解析结果，可搜索 -->
        <el-dialog v-model="pageVisible" :title="`文字内容 · ${pageMaterial?.title || ''}`" width="760px">
            <div class="row" style="margin-bottom: 12px">
                <el-input
                    v-model="pageKeyword"
                    placeholder="在本文档里搜关键词（至少 2 个字）"
                    style="width: 320px"
                    clearable
                    @keyup.enter="doSearch"
                />
                <el-button type="primary" @click="doSearch">搜索</el-button>
                <el-button @click="clearSearch">看全文</el-button>
            </div>

            <el-empty v-if="!hitMode && pages.length === 0" description="这份文件没有抽到文字，可能是扫描件" />

            <el-table v-if="hitMode" :data="hits" size="small" border max-height="420">
                <el-table-column prop="pageNo" label="页" width="60" />
                <el-table-column prop="snippet" label="命中片段" show-overflow-tooltip />
            </el-table>

            <div v-else class="pages">
                <div v-for="p in pages" :key="p.pageNo" class="page-item" @mouseup="onSelect(p.pageNo)">
                    <div class="page-no">第 {{ p.pageNo }} 页</div>
                    <div class="page-text">{{ p.content }}</div>
                </div>
            </div>

            <div v-if="selectedText" class="select-bar">
                <span>已选 {{ selectedText.length }} 字 · 第 {{ selectedPage }} 页</span>
                <el-button type="primary" size="small" @click="goCreateQuestion">用这段出题</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { downloadFile, openBlob } from '@/utils/request';
import {
    createChapter,
    createCourse,
    createKnowledge,
    deleteChapter,
    deleteCourse,
    deleteKnowledge,
    deleteMaterial,
    getChapters,
    getCourses,
    getKnowledgeByChapter,
    getMaterialPages,
    getMaterials,
    extractKnowledge,
    parseMaterial,
    searchMaterial,
    updateCourse,
    uploadMaterial
} from '@/api';

const loading = ref(false);
const saving = ref(false);
const courses = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');

const formVisible = ref(false);
const form = reactive<any>({ id: null, name: '', description: '' });

const detailVisible = ref(false);
const current = ref<any>(null);
const chapters = ref<any[]>([]);
const materials = ref<any[]>([]);
const chapterTitle = ref('');

const activeChapter = ref<any>(null);
const knowledges = ref<any[]>([]);
const knowledgeName = ref('');

/** 点章节行，下面切换成这一章的知识点 */
const selectChapter = async (row: any) => {
    activeChapter.value = row;
    knowledges.value = await getKnowledgeByChapter(row.id);
};

const addKnowledge = async () => {
    if (!knowledgeName.value.trim()) {
        ElMessage.warning('知识点名称不能为空');
        return;
    }
    await createKnowledge(current.value.id, {
        chapterId: activeChapter.value.id,
        name: knowledgeName.value.trim(),
        type: 'CONCEPT',
        importance: 3
    });
    knowledgeName.value = '';
    knowledges.value = await getKnowledgeByChapter(activeChapter.value.id);
    ElMessage.success('已添加');
};

const removeKnowledge = async (row: any) => {
    await ElMessageBox.confirm('删除后该知识点的掌握记录会成为孤儿，确定吗？', '提示', { type: 'warning' });
    await deleteKnowledge(row.id);
    knowledges.value = await getKnowledgeByChapter(activeChapter.value.id);
};

const pageVisible = ref(false);
const pageMaterial = ref<any>(null);
const pages = ref<any[]>([]);
const hits = ref<any[]>([]);
const hitMode = ref(false);
const pageKeyword = ref('');
const selectedText = ref('');
const selectedPage = ref(0);
const router = useRouter();

/** 鼠标在页文本上松开时，看看有没有选中文字 */
const onSelect = (pageNo: number) => {
    const text = window.getSelection()?.toString().trim() || '';
    if (text) {
        selectedText.value = text;
        selectedPage.value = pageNo;
    }
};

const goCreateQuestion = () => {
    pageVisible.value = false;
    router.push({
        path: '/questions',
        query: {
            courseId: String(pageMaterial.value.courseId),
            materialId: String(pageMaterial.value.id),
            pageNo: String(selectedPage.value),
            quote: selectedText.value.slice(0, 500)
        }
    });
};

const openPages = async (row: any) => {
    pageMaterial.value = row;
    pageVisible.value = true;
    pages.value = await getMaterialPages(row.id);
    hits.value = [];
    hitMode.value = false;
    pageKeyword.value = '';
};

const doSearch = async () => {
    if (pageKeyword.value.trim().length < 2) {
        ElMessage.warning('关键词至少 2 个字');
        return;
    }
    hits.value = await searchMaterial(pageMaterial.value.id, pageKeyword.value.trim());
    hitMode.value = true;
};

const clearSearch = () => {
    hitMode.value = false;
    hits.value = [];
};

const load = async () => {
    loading.value = true;
    try {
        const data = await getCourses({ page: page.value, size: size.value, keyword: keyword.value });
        courses.value = data.records;
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
    form.id = null;
    form.name = '';
    form.description = '';
    formVisible.value = true;
};

const openEdit = (row: any) => {
    form.id = row.id;
    form.name = row.name;
    form.description = row.description;
    formVisible.value = true;
};

const save = async () => {
    if (!form.name.trim()) {
        ElMessage.warning('课程名称不能为空');
        return;
    }
    saving.value = true;
    try {
        if (form.id) {
            await updateCourse(form.id, { name: form.name, description: form.description });
        } else {
            await createCourse({ name: form.name, description: form.description });
        }
        ElMessage.success('已保存');
        formVisible.value = false;
        load();
    } finally {
        saving.value = false;
    }
};

const remove = async (row: any) => {
    await ElMessageBox.confirm(`确定删除《${row.name}》吗？`, '提示', { type: 'warning' });
    await deleteCourse(row.id);
    ElMessage.success('已删除');
    load();
};

const openDetail = async (row: any) => {
    current.value = row;
    detailVisible.value = true;
    // 换课程时清掉上一门课选中的章节和知识点，避免串数据
    activeChapter.value = null;
    knowledges.value = [];
    knowledgeName.value = '';
    await loadDetail();
};

const loadDetail = async () => {
    chapters.value = await getChapters(current.value.id);
    materials.value = await getMaterials(current.value.id);
};

/** 页面解析：把这份资料的文字页逐页交给大模型，产出结构化内容 */
const parsingId = ref<number | null>(null);
const parseOne = async (row: any) => {
    parsingId.value = row.id;
    try {
        const result = await parseMaterial(row.id);
        ElMessage.success(`解析完成：成功 ${result.parsed} 页，待复核 ${result.needsReview} 页`);
    } finally {
        parsingId.value = null;
    }
};

/** 知识点抽取：需要先选章节，抽完直接入库 */
const extractingId = ref<number | null>(null);
const extractOne = async (row: any) => {
    const chapterId = row.chapterId || activeChapter.value?.id;
    if (!chapterId) {
        ElMessage.warning('先在左边选一个章节，抽出的知识点要归到章下');
        return;
    }
    extractingId.value = row.id;
    try {
        const result = await extractKnowledge(current.value.id, chapterId, row.id);
        ElMessage.success(`新增 ${result.created} 个知识点，跳过重复 ${result.duplicates} 个`);
        if (activeChapter.value && activeChapter.value.id === chapterId) {
            knowledges.value = await getKnowledgeByChapter(chapterId);
        }
    } finally {
        extractingId.value = null;
    }
};

const addChapter = async () => {
    if (!chapterTitle.value.trim()) {
        ElMessage.warning('请输入章节名称');
        return;
    }
    await createChapter(current.value.id, { title: chapterTitle.value });
    chapterTitle.value = '';
    await loadDetail();
};

const removeChapter = async (row: any) => {
    await deleteChapter(row.id);
    await loadDetail();
};

const doUpload = async (options: any) => {
    const formData = new FormData();
    formData.append('courseId', String(current.value.id));
    formData.append('file', options.file);
    await uploadMaterial(formData);
    ElMessage.success('上传成功');
    await loadDetail();
};

const download = async (row: any) => {
    await downloadFile(`/materials/${row.id}/download`, row.fileName);
};

const preview = async (row: any) => {
    await openBlob(`/materials/${row.id}/preview`);
};

const removeMaterial = async (row: any) => {
    await deleteMaterial(row.id);
    ElMessage.success('已删除');
    await loadDetail();
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
.pager {
    margin-top: 14px;
    justify-content: flex-end;
}
.row {
    display: flex;
    gap: 8px;
}
.muted {
    color: #909399;
    font-size: 12px;
}
.pages {
    max-height: 420px;
    overflow-y: auto;
}
.page-item {
    padding: 8px 0;
    border-bottom: 1px dashed #ebeef5;
}
.page-no {
    font-weight: 600;
    color: #409eff;
    margin-bottom: 4px;
}
.page-text {
    white-space: pre-wrap;
    line-height: 1.7;
    font-size: 13px;
    color: #303133;
}
.select-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    padding: 8px 12px;
    background: #ecf5ff;
    border-radius: 4px;
    font-size: 13px;
}
</style>
