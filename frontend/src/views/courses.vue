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
            <el-table :data="chapters" size="small" border style="margin-top: 10px">
                <el-table-column prop="title" label="章节" />
                <el-table-column prop="sort" label="排序" width="80" />
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button link type="danger" @click="removeChapter(row)">删除</el-button>
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
                        <el-button link type="primary" @click="download(row)">下载</el-button>
                        <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-drawer>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
    createChapter,
    createCourse,
    deleteChapter,
    deleteCourse,
    deleteMaterial,
    getChapters,
    getCourses,
    getMaterials,
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

const download = (row: any) => {
    // 下载要走网关并带 token，这里用 a 标签 + 后端短链不适用，直接新窗口打开
    window.open(`/api/materials/${row.id}/download?token=${localStorage.getItem('token')}`, '_blank');
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
</style>
