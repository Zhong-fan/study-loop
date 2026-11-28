<template>
    <div class="auth">
        <header class="auth-head">
            <router-link to="/login" class="brand">CMMR</router-link>
        </header>

        <main class="auth-main">
            <div class="tabs">
                <router-link to="/login" class="tab" :class="{ active: !registerMode }">登录</router-link>
                <span class="slash">/</span>
                <router-link to="/register" class="tab" :class="{ active: registerMode }">注册</router-link>
            </div>

            <h1>{{ registerMode ? '注册' : '登录' }}</h1>

            <el-form
                ref="loginRef"
                :model="form"
                :rules="rules"
                label-position="top"
                hide-required-asterisk
                class="form"
                @submit.prevent
            >
                <label class="field-label" for="username">用户名</label>
                <el-form-item prop="username">
                    <el-input
                        id="username"
                        v-model.trim="form.username"
                        size="large"
                        placeholder="用户名"
                        autocomplete="username"
                    />
                </el-form-item>

                <template v-if="registerMode">
                    <label class="field-label" for="nickname">昵称</label>
                    <el-form-item prop="nickname">
                        <el-input id="nickname" v-model.trim="form.nickname" size="large" placeholder="昵称（可留空）" />
                    </el-form-item>
                </template>

                <label class="field-label" for="password">密码</label>
                <el-form-item prop="password">
                    <el-input
                        id="password"
                        v-model="form.password"
                        type="password"
                        size="large"
                        show-password
                        :placeholder="registerMode ? '至少 6 位' : '密码'"
                        :autocomplete="registerMode ? 'new-password' : 'current-password'"
                        @keyup.enter="submit"
                    />
                </el-form-item>

                <template v-if="registerMode">
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
                </template>

                <button type="button" class="submit" :disabled="loading" @click="submit">
                    {{ loading ? '处理中…' : registerMode ? '注册' : '登录' }}
                </button>
            </el-form>

            <div v-if="!registerMode" class="demo">
                <span>演示账号</span>
                <code>admin / 123456</code>
                <button type="button" class="fill" @click="fillDemo">填入</button>
            </div>

            <p class="switch">
                <router-link :to="registerMode ? '/login' : '/register'" class="switch-link">
                    {{ registerMode ? '返回登录' : '注册账号' }}
                </router-link>
            </p>
        </main>

        <footer class="auth-foot">
            <span>CMMR</span>
            <span class="mono">{{ year }}</span>
        </footer>
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
const year = new Date().getFullYear();

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
            ElMessage.success('注册成功，已登录');
        } else {
            const data = await login({ username: form.username, password: form.password });
            rememberUser(data);
            ElMessage.success('登录成功');
        }
        (document.activeElement as HTMLElement)?.blur();
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
.auth {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    padding: 0 40px;
    animation: page-in 520ms cubic-bezier(0.16, 0.84, 0.3, 1) both;
}

.auth-head {
    display: flex;
    align-items: center;
    height: var(--sl-header-h);
    border-bottom: 1px solid var(--sl-border);
}
.brand {
    font-family: var(--sl-font-display);
    font-size: 24px;
    letter-spacing: -0.02em;
    color: var(--sl-text);
    text-decoration: none;
}

.auth-main {
    width: 100%;
    max-width: 380px;
    margin: 72px auto 0;
}

.tabs {
    display: flex;
    align-items: baseline;
    gap: 10px;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--sl-line-strong);
}
.tab {
    font-family: var(--sl-font-mono);
    font-size: 11px;
    letter-spacing: 0.1em;
    color: var(--sl-muted);
    text-decoration: none;
}
.tab.active {
    color: var(--sl-primary);
}
.slash {
    color: var(--sl-border);
}

.auth-main h1 {
    margin: 28px 0 30px;
    font-size: 34px;
    font-weight: 400;
    letter-spacing: -0.02em;
}

.field-label {
    display: block;
    margin-bottom: 6px;
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.1em;
    color: var(--sl-muted);
}
.form :deep(.el-form-item) {
    margin-bottom: 18px;
}
.form :deep(.el-input__wrapper) {
    padding: 1px 0;
    border-radius: 0;
    background-color: transparent !important;
    background: transparent !important;
    box-shadow: none;
    border-bottom: 1px solid var(--sl-line-strong);
    transition: border-color 0.18s ease;
}
.form :deep(.el-input__wrapper:hover),
.form :deep(.el-input__wrapper.is-focus) {
    box-shadow: none;
    border-bottom-color: var(--sl-primary);
}
.form :deep(.el-input__inner) {
    height: 40px;
    background: transparent;
}
.form :deep(.el-form-item__error) {
    padding-top: 5px;
    font-size: 11px;
    color: var(--sl-danger);
}

.submit {
    width: 100%;
    margin-top: 10px;
    padding: 14px 18px;
    font-family: var(--sl-font-ui);
    font-size: 14px;
    color: var(--sl-surface);
    background: var(--sl-text);
    border: 1px solid var(--sl-text);
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease;
}
.submit:hover:not(:disabled) {
    background: var(--sl-primary);
    border-color: var(--sl-primary);
}
.submit:disabled {
    opacity: 0.5;
    cursor: default;
}

.demo {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 24px;
    padding-top: 14px;
    font-size: 12px;
    color: var(--sl-muted);
    border-top: 1px solid var(--sl-border);
}
.demo code {
    font-family: var(--sl-font-mono);
    font-size: 11px;
    color: var(--sl-text-secondary);
}
.fill {
    margin-left: auto;
    padding: 0;
    font-size: 12px;
    color: var(--sl-primary);
    background: none;
    border: 0;
    cursor: pointer;
}

.switch {
    margin: 18px 0 0;
    font-size: 12px;
    text-align: left;
}
.switch-link {
    color: var(--sl-text-secondary);
    text-decoration: none;
    border-bottom: 1px solid var(--sl-border);
}
.switch-link:hover {
    color: var(--sl-primary);
    border-color: var(--sl-primary);
}

.auth-foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    max-width: var(--sl-content-w);
    margin: auto auto 0;
    padding: 22px 0 30px;
    font-size: 12px;
    color: var(--sl-muted);
    border-top: 1px solid var(--sl-border);
}

@media (max-width: 640px) {
    .auth {
        padding: 0 20px;
    }
    .auth-main {
        margin-top: 40px;
    }
}
</style>
