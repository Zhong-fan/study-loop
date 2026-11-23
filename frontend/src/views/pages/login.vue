<template>
    <div class="auth-page">
        <div class="auth-card">
            <div class="brand">
                <img src="../../assets/img/logo.svg" alt="" />
                <span>CMMR 学习助手</span>
            </div>

            <div class="tabs" role="tablist">
                <router-link to="/login" class="tab" :class="{ active: !registerMode }">登录</router-link>
                <router-link to="/register" class="tab" :class="{ active: registerMode }">注册</router-link>
            </div>

            <h1 class="title">{{ registerMode ? '创建你的账号' : '欢迎回来' }}</h1>
            <p class="subtitle">
                {{ registerMode ? '注册后即可上传资料，自动生成知识点与复习计划。' : '登录后继续你的复习计划。' }}
            </p>

            <el-form
                ref="loginRef"
                :model="form"
                :rules="rules"
                label-position="top"
                hide-required-asterisk
                class="form"
                @submit.prevent
            >
                <div class="field">
                    <label class="field-label" for="username">用户名</label>
                    <el-form-item prop="username">
                        <el-input
                            id="username"
                            v-model.trim="form.username"
                            size="large"
                            placeholder="请输入用户名"
                            autocomplete="username"
                        />
                    </el-form-item>
                </div>

                <div v-if="registerMode" class="field">
                    <label class="field-label" for="nickname">昵称</label>
                    <el-form-item prop="nickname">
                        <el-input
                            id="nickname"
                            v-model.trim="form.nickname"
                            size="large"
                            placeholder="展示在页面右上角，可留空"
                        />
                    </el-form-item>
                </div>

                <div class="field">
                    <label class="field-label" for="password">密码</label>
                    <el-form-item prop="password">
                        <el-input
                            id="password"
                            v-model="form.password"
                            type="password"
                            size="large"
                            show-password
                            :placeholder="registerMode ? '至少 6 位' : '请输入密码'"
                            :autocomplete="registerMode ? 'new-password' : 'current-password'"
                            @keyup.enter="submit"
                        />
                    </el-form-item>
                </div>

                <div v-if="registerMode" class="field">
                    <label class="field-label" for="confirm">确认密码</label>
                    <el-form-item prop="confirm">
                        <el-input
                            id="confirm"
                            v-model="form.confirm"
                            type="password"
                            size="large"
                            show-password
                            placeholder="再输入一次"
                            autocomplete="new-password"
                            @keyup.enter="submit"
                        />
                    </el-form-item>
                </div>

                <el-button class="submit" type="primary" size="large" :loading="loading" @click="submit">
                    {{ registerMode ? '注册并登录' : '登录' }}
                </el-button>
            </el-form>

            <div v-if="!registerMode" class="demo">
                <span>演示账号</span>
                <code>admin / 123456</code>
                <el-button link type="primary" @click="fillDemo">填入</el-button>
            </div>

            <p class="legal">
                {{ registerMode ? '已有账号？' : '还没有账号？' }}
                <router-link :to="registerMode ? '/login' : '/register'" class="legal-link">
                    {{ registerMode ? '返回登录' : '立即注册' }}
                </router-link>
            </p>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { login, register } from '@/api';

const router = useRouter();
const route = useRoute();
const loginRef = ref<FormInstance>();
const loading = ref(false);
const registerMode = ref(route.path === '/register');

const form = reactive({
    username: '',
    password: '',
    nickname: '',
    confirm: ''
});

const rules = computed<FormRules>(() => ({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        ...(registerMode.value
            ? [{ min: 3, max: 20, message: '用户名长度 3–20 位', trigger: 'blur' }]
            : [])
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        ...(registerMode.value
            ? [{ min: 6, max: 32, message: '密码至少 6 位', trigger: 'blur' }]
            : [])
    ],
    confirm: registerMode.value
        ? [
              { required: true, message: '请再输入一次密码', trigger: 'blur' },
              {
                  validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
                      if (value !== form.password) {
                          callback(new Error('两次输入的密码不一致'));
                          return;
                      }
                      callback();
                  },
                  trigger: 'blur'
              }
          ]
        : []
}));

watch(
    () => route.path,
    path => {
        registerMode.value = path === '/register';
        form.password = '';
        form.confirm = '';
        loginRef.value?.clearValidate();
    }
);

const rememberUser = (data: any) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem(
        'user',
        JSON.stringify({
            userId: data.userId,
            username: data.username,
            nickname: data.nickname,
            role: data.role || 'STUDENT'
        })
    );
};

const submit = async () => {
    if (!loginRef.value) {
        return;
    }
    const valid = await loginRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }

    loading.value = true;
    try {
        if (registerMode.value) {
            await register({
                username: form.username,
                password: form.password,
                nickname: form.nickname || form.username
            });
            const data = await login({ username: form.username, password: form.password });
            rememberUser(data);
            ElMessage.success('注册成功，已为你登录');
        } else {
            const data = await login({ username: form.username, password: form.password });
            rememberUser(data);
            ElMessage.success('登录成功');
        }
        router.push('/dashboard');
    } catch (e) {
        // 错误提示由请求拦截器统一处理
    } finally {
        loading.value = false;
    }
};

const fillDemo = () => {
    form.username = 'admin';
    form.password = '123456';
    loginRef.value?.clearValidate();
};
</script>

<style scoped>
.auth-page {
    position: relative;
    display: flex;
    min-height: 100vh;
    padding: 40px 24px;
    background:
        radial-gradient(1000px 500px at 12% -10%, rgba(37, 99, 235, 0.1), transparent 60%),
        radial-gradient(800px 400px at 100% 0%, rgba(14, 165, 233, 0.08), transparent 55%),
        #f6f8fb;
    overflow: hidden;
}
.auth-page::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: radial-gradient(rgba(15, 23, 42, 0.07) 1px, transparent 1px);
    background-size: 22px 22px;
    mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.55), transparent 70%);
    pointer-events: none;
}

.auth-card {
    position: relative;
    width: 100%;
    max-width: 440px;
    margin: auto;
    padding: 40px;
    background: #fff;
    border: 1px solid rgba(15, 23, 42, 0.08);
    border-radius: 20px;
    box-shadow:
        0 1px 2px rgba(16, 24, 40, 0.04),
        0 24px 60px -28px rgba(16, 24, 40, 0.22);
}

.brand {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 30px;
    font-size: 15px;
    font-weight: 600;
    color: var(--sl-text);
}
.brand img {
    width: 26px;
    height: 26px;
}

.tabs {
    display: inline-flex;
    gap: 4px;
    padding: 4px;
    margin-bottom: 26px;
    background: #f1f5f9;
    border-radius: 10px;
}
.tab {
    padding: 7px 18px;
    font-size: 13px;
    font-weight: 500;
    color: #64748b;
    border-radius: 7px;
    text-decoration: none;
    transition: all 0.16s ease;
}
.tab:hover {
    color: #0f172a;
}
.tab.active {
    color: #0f172a;
    background: #fff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.title {
    margin: 0;
    font-size: 27px;
    line-height: 1.25;
    font-weight: 700;
    letter-spacing: -0.3px;
    color: #0f172a;
}
.subtitle {
    margin: 10px 0 28px;
    font-size: 14px;
    line-height: 1.6;
    color: #64748b;
}

.field {
    margin-bottom: 4px;
}
.field-label {
    display: block;
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 500;
    color: #334155;
}
.form :deep(.el-form-item) {
    margin-bottom: 16px;
}
.form :deep(.el-input__wrapper) {
    padding: 2px 12px;
    border-radius: 10px;
    box-shadow: 0 0 0 1px #e2e8f0 inset;
}
.form :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px #cbd5e1 inset;
}
.form :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 3px rgba(37, 99, 235, 0.12);
}
.form :deep(.el-input__inner) {
    height: 42px;
}
.form :deep(.el-form-item__error) {
    padding-top: 4px;
    font-size: 12px;
}

.submit {
    width: 100%;
    height: 46px;
    margin-top: 8px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 10px;
    background: #2563eb;
    border-color: #2563eb;
}
.submit:hover,
.submit:focus {
    background: #1d4ed8;
    border-color: #1d4ed8;
}

.demo {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 22px;
    padding: 10px 12px;
    font-size: 12px;
    color: #64748b;
    background: #f8fafc;
    border: 1px dashed #e2e8f0;
    border-radius: 10px;
}
.demo code {
    padding: 2px 6px;
    color: #0f172a;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.demo :deep(.el-button) {
    margin-left: auto;
    font-size: 12px;
}

.legal {
    margin: 20px 0 0;
    font-size: 12px;
    color: #94a3b8;
    text-align: center;
}
.legal-link {
    color: #2563eb;
    text-decoration: none;
}
.legal-link:hover {
    text-decoration: underline;
}

@media (max-width: 520px) {
    .auth-page {
        padding: 20px;
    }
    .auth-card {
        padding: 28px 22px;
        border-radius: 16px;
    }
    .title {
        font-size: 24px;
    }
}
</style>
