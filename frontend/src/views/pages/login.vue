<template>
    <div class="auth">
        <header class="auth-head">
            <router-link to="/login" class="brand">
                <span class="mark">CMMR</span>
                <span class="sub">学习手记</span>
            </router-link>
            <span class="mono">课程资料管理与错题复习</span>
        </header>

        <main class="auth-main">
            <section class="lede">
                <p class="eyebrow">Study loop / {{ year }}</p>
                <h1>
                    把课件<br />
                    变成<em>会考</em>的题。
                </h1>
                <p class="dek">
                    上传资料，抽出知识点与证据；按掌握度安排复习，只练不会的。
                </p>
                <ol class="facts">
                    <li><span class="mono">01</span>PDF / 图片自动解析，保留原文证据</li>
                    <li><span class="mono">02</span>按 1 / 3 / 7 / 14 天推进复习</li>
                    <li><span class="mono">03</span>单点会、综合不会，也能看出来</li>
                </ol>
            </section>

            <section class="panel">
                <div class="tabs">
                    <router-link to="/login" class="tab" :class="{ active: !registerMode }">登录</router-link>
                    <span class="slash">/</span>
                    <router-link to="/register" class="tab" :class="{ active: registerMode }">注册</router-link>
                </div>

                <h2>{{ registerMode ? '创建账号' : '欢迎回来' }}</h2>
                <p class="panel-sub">
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

                    <template v-if="registerMode">
                        <label class="field-label" for="nickname">昵称</label>
                        <el-form-item prop="nickname">
                            <el-input
                                id="nickname"
                                v-model.trim="form.nickname"
                                size="large"
                                placeholder="展示在右上角，可留空"
                            />
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
                            :placeholder="registerMode ? '至少 6 位' : '请输入密码'"
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
                        {{ loading ? '处理中…' : registerMode ? '注册并登录' : '登录' }}
                        <span aria-hidden="true">→</span>
                    </button>
                </el-form>

                <div v-if="!registerMode" class="demo">
                    <span class="mono">Demo</span>
                    <code>admin / 123456</code>
                    <button type="button" class="fill" @click="fillDemo">填入</button>
                </div>

                <p class="switch">
                    {{ registerMode ? '已有账号？' : '还没有账号？' }}
                    <router-link :to="registerMode ? '/login' : '/register'" class="switch-link">
                        {{ registerMode ? '返回登录' : '立即注册' }}
                    </router-link>
                </p>
            </section>
        </main>

        <footer class="auth-foot">
            <span>CMMR · 课程资料管理与错题复习</span>
            <span class="mono">Built for review, not for busywork</span>
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
            ElMessage.success('注册成功，已为你登录');
        } else {
            const data = await login({ username: form.username, password: form.password });
            rememberUser(data);
            ElMessage.success('登录成功');
        }
        // 收起移动端键盘/输入框带来的视口偏移
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
    animation: page-in 520ms cubic-bezier(0.16, 0.84, 0.3, 1) both;
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    padding: 0 40px;
}

.auth-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: var(--sl-header-h);
    border-bottom: 1px solid var(--sl-border);
}
.brand {
    display: flex;
    align-items: baseline;
    gap: 10px;
    text-decoration: none;
}
.mark {
    font-family: var(--sl-font-display);
    font-size: 24px;
    letter-spacing: -0.02em;
    color: var(--sl-text);
}
.sub {
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.18em;
    text-transform: uppercase;
    color: var(--sl-muted);
}
.auth-head .mono {
    color: var(--sl-muted);
}

.auth-main {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 400px;
    gap: 80px;
    align-items: start;
    width: 100%;
    max-width: var(--sl-content-w);
    margin: 0 auto;
    padding: 80px 0 72px;
}

.lede h1 {
    margin: 26px 0 0;
    font-size: clamp(46px, 6vw, 76px);
    line-height: 1.08;
    font-weight: 400;
    letter-spacing: -0.03em;
    color: var(--sl-text);
}
.lede h1 em {
    font-style: italic;
    color: var(--sl-primary);
}
.dek {
    max-width: 430px;
    margin: 24px 0 0;
    font-size: 15px;
    line-height: 1.9;
    color: var(--sl-text-secondary);
}

.facts {
    margin: 40px 0 0;
    padding: 0;
    list-style: none;
    border-top: 1px solid var(--sl-border);
}
.facts li {
    display: flex;
    gap: 20px;
    padding: 14px 0;
    font-size: 13px;
    color: var(--sl-text-secondary);
    border-bottom: 1px solid var(--sl-border);
}
.facts .mono {
    color: var(--sl-muted);
}

.panel {
    padding-top: 4px;
}
.tabs {
    display: flex;
    align-items: baseline;
    gap: 12px;
    padding-bottom: 18px;
    border-bottom: 1px solid var(--sl-line-strong);
}
.tab {
    font-family: var(--sl-font-mono);
    font-size: 11px;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: var(--sl-muted);
    text-decoration: none;
    transition: color 0.18s ease;
}
.tab.active,
.tab:hover {
    color: var(--sl-primary);
}
.slash {
    color: var(--sl-border);
}

.panel h2 {
    margin: 28px 0 0;
    font-size: 34px;
    line-height: 1.2;
    font-weight: 400;
    letter-spacing: -0.02em;
}
.panel-sub {
    margin: 10px 0 28px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--sl-muted);
}

.field-label {
    display: block;
    margin-bottom: 7px;
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: var(--sl-muted);
}
.form :deep(.el-form-item) {
    margin-bottom: 18px;
}
.form :deep(.el-input__wrapper) {
    padding: 1px 0;
    border-radius: 0;
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
}
.form :deep(.el-form-item__error) {
    padding-top: 5px;
    font-size: 11px;
    color: var(--sl-danger);
}

.submit {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    margin-top: 14px;
    padding: 15px 20px;
    font-family: var(--sl-font-body);
    font-size: 14px;
    letter-spacing: 0.02em;
    color: var(--sl-surface);
    background: var(--sl-text);
    border: 1px solid var(--sl-text);
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease;
}
.submit span {
    transition: transform 0.24s ease;
}
.submit:hover:not(:disabled) {
    background: var(--sl-primary);
    border-color: var(--sl-primary);
}
.submit:hover:not(:disabled) span {
    transform: translateX(4px);
}
.submit:disabled {
    opacity: 0.5;
    cursor: default;
}

.demo {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 26px;
    padding-top: 16px;
    border-top: 1px solid var(--sl-border);
    font-size: 12px;
    color: var(--sl-muted);
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
    margin: 22px 0 0;
    font-size: 12px;
    color: var(--sl-muted);
}
.switch-link {
    color: var(--sl-primary);
    text-decoration: none;
    border-bottom: 1px solid transparent;
    transition: border-color 0.18s ease;
}
.switch-link:hover {
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

@media (max-width: 900px) {
    .auth {
        padding: 0 20px;
    }
    .auth-main {
        grid-template-columns: 1fr;
        gap: 48px;
        padding: 44px 0 56px;
    }
    .lede h1 {
        font-size: clamp(38px, 10vw, 52px);
    }
    .facts {
        margin-top: 28px;
    }
    .auth-head .mono,
    .auth-foot .mono {
        display: none;
    }
}
</style>
