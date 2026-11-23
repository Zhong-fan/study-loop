<template>
    <div class="page sl-page">
        <el-card v-if="isAdmin" class="block">
            <template #header>
                <div class="card-header">
                    <span>AI 配置</span>
                    <span class="hint">来源：{{ ai.source || '—' }}</span>
                </div>
            </template>

            <el-form label-width="100px" style="max-width: 620px">
                <el-form-item label="启用 AI">
                    <el-switch v-model="ai.enabled" />
                </el-form-item>
                <el-form-item label="Base URL">
                    <el-input v-model="ai.baseUrl" placeholder="https://api.deepseek.com/v1" />
                </el-form-item>
                <el-form-item label="模型">
                    <el-select
                        v-model="ai.model"
                        filterable
                        allow-create
                        default-first-option
                        placeholder="选择或输入模型名"
                        style="width: 100%"
                    >
                        <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
                    </el-select>
                </el-form-item>
                <el-form-item label="API Key">
                    <el-input
                        v-model="ai.apiKey"
                        type="password"
                        show-password
                        :placeholder="ai.keySet ? '已设置，留空表示不修改' : '尚未设置'"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button :loading="aiTesting" @click="testAi">测试连接</el-button>
                    <el-button type="primary" :loading="aiSaving" @click="saveAi">保存</el-button>
                    <span v-if="aiTest" class="test-result" :class="aiTest.ok ? 'ok' : 'bad'">
                        {{ aiTest.ok ? `连接正常，耗时 ${aiTest.latencyMs}ms（${aiTest.model}）` : `失败：${aiTest.message}` }}
                    </span>
                </el-form-item>
                <el-form-item v-if="ai.updatedAt">
                    <span class="hint">最后修改：{{ ai.updatedAt }}（用户 {{ ai.updatedBy }}）</span>
                </el-form-item>
            </el-form>
        </el-card>

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
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getAiConfig, getUsers, saveAiConfig, setUserStatus, testAiConfig } from '@/api';

const users = ref<any[]>([]);

const modelOptions = [
    'deepseek-v4-1-flash',
    'deepseek-chat',
    'deepseek-reasoner',
    'gpt-4o-mini',
    'qwen-plus'
];

const ai = reactive<any>({ enabled: true, baseUrl: '', model: '', apiKey: '', keySet: false, source: '', updatedAt: '', updatedBy: null });
const aiSaving = ref(false);
const aiTesting = ref(false);
const aiTest = ref<any>(null);

const loadAi = async () => {
    const data = await getAiConfig();
    Object.assign(ai, data, { apiKey: '' });
};

const saveAi = async () => {
    aiSaving.value = true;
    try {
        const data = await saveAiConfig({
            enabled: ai.enabled,
            baseUrl: ai.baseUrl,
            model: ai.model,
            apiKey: ai.apiKey || undefined
        });
        Object.assign(ai, data, { apiKey: '' });
        aiTest.value = null;
        ElMessage.success('已保存，立即生效');
    } finally {
        aiSaving.value = false;
    }
};

const testAi = async () => {
    aiTesting.value = true;
    try {
        aiTest.value = await testAiConfig({
            enabled: ai.enabled,
            baseUrl: ai.baseUrl,
            model: ai.model,
            apiKey: ai.apiKey || undefined
        });
    } finally {
        aiTesting.value = false;
    }
};

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

onMounted(() => {
    load();
    if (isAdmin) {
        loadAi();
    }
});
</script>

<style scoped>
.block { margin-bottom: var(--sl-s4); }
.test-result { margin-left: 12px; font-size: 12px; }
.test-result.ok { color: var(--sl-success); }
.test-result.bad { color: var(--sl-danger); }
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
