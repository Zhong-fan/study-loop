<template>
    <div class="page">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>我的课程</span>
                    <div>
                        <el-input
                            v-model="keyword"
                            placeholder="按名称搜索"
                            style="width: 200px; margin-right: 8px"
                            clearable
                            @keyup.enter="load"
                        />
                        <el-button @click="load">查询</el-button>
                        <el-button type="primary" @click="openCreate">新建课程</el-button>
                    </div>
                </div>
            </template>

            <el-table :data="courses" v-loading="loading" border>
                <el-table-column prop="name" label="课程" width="200" />
                <el-table-column prop="description" label="简介" show-overflow-tooltip />
                <el-table-column prop="createdAt" label="创建时间" width="180" />
                <el-table-column label="操作" width="240">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openDetail(row)">章节与资料</el-button>
                        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="danger" @click="remove(row)">删除</el-button>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="还没有课程，先建一门再上传资料" :image-size="80">
                        <el-button type="primary" @click="openCreate">新建课程</el-button>
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
                <el-table-column label="操作" width="150">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="preview(row)">预览</el-button>
                        <el-button link type="primary" @click="openPages(row)">文字</el-button>
                        <el-button link type="primary" @click="download(row)">下载</el-button>
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
