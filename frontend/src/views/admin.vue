<template>
    <div class="page">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>用户管理</span>
                    <el-button size="small" @click="load">刷新</el-button>
                </div>
            </template>

            <el-alert
                v-if="!isAdmin"
                type="warning"
                :closable="false"
                title="当前账号不是管理员，看不到用户列表"
                description="注册时把用户名设成 admin（可在 user-service 的 app.admin.username 里改）就会获得管理员角色。"
            />

            <el-table v-else :data="users" border size="small">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="username" label="用户名" width="160" />
                <el-table-column prop="nickname" label="昵称" width="160" />
                <el-table-column label="角色" width="110">
                    <template #default="{ row }">
                        <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
                            {{ row.role }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.deleted === 0 ? 'success' : 'info'" size="small">
                            {{ row.deleted === 0 ? '正常' : '已禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="注册时间" width="180" />
                <el-table-column label="操作">
                    <template #default="{ row }">
                        <el-button
                            v-if="row.role !== 'ADMIN'"
                            link
                            :type="row.deleted === 0 ? 'danger' : 'primary'"
                            @click="toggle(row)"
                        >
                            {{ row.deleted === 0 ? '禁用' : '启用' }}
                        </el-button>
                        <span v-else class="hint">管理员不可禁用</span>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getUsers, setUserStatus } from '@/api';

const users = ref<any[]>([]);

const stored = JSON.parse(localStorage.getItem('user') || '{}');
const isAdmin = stored.role === 'ADMIN';

const load = async () => {
    if (!isAdmin) {
        return;
    }
    users.value = await getUsers();
};

const toggle = async (row: any) => {
    await setUserStatus(row.id, row.deleted === 0 ? 1 : 0);
    ElMessage.success('已更新');
    load();
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
.hint {
    color: #909399;
    font-size: 12px;
}
</style>
