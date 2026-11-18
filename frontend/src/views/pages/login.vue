<template>
    <div class="login-page">
        <!-- 左侧品牌区：只在宽屏显示，窄屏自动隐藏 -->
        <section class="brand">
            <div class="brand-inner">
                <div class="brand-logo">
                    <img src="../../assets/img/logo.svg" alt="" />
                    <span>CMMR 学习助手</span>
                </div>

                <h1 class="brand-title">把课件变成<br />会考的题</h1>
                <p class="brand-sub">
                    解析资料、沉淀知识点、按掌握度排复习，<br />
                    让每一次练习都有依据。
                </p>

                <ul class="brand-points">
                    <li><span class="dot"></span>上传 PDF / PPT，自动抽出知识点</li>
                    <li><span class="dot"></span>按 1 / 3 / 7 / 14 天安排复习</li>
                    <li><span class="dot"></span>覆盖率与盲区一眼看清</li>
                </ul>
            </div>
            <div class="brand-footer">课程资料管理与错题复习系统</div>
        </section>

        <!-- 右侧表单区 -->
        <section class="panel">
            <div class="form-wrap">
                <h2 class="form-title">{{ registerMode ? '创建账号' : '欢迎回来' }}</h2>
                <p class="form-sub">{{ registerMode ? '注册后即可上传资料、开始练习' : '登录后继续你的复习计划' }}</p>

                <el-form :model="param" :rules="rules" ref="loginRef" size="large" class="form">
                    <el-form-item prop="username">
                        <el-input v-model="param.username" placeholder="用户名" :prefix-icon="User" />
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input
                            type="password"
                            placeholder="密码"
                            v-model="param.password"
                            :prefix-icon="Lock"
                            show-password
                            @keyup.enter="submitForm"
                        />
                    </el-form-item>

                    <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="submitForm">
                        {{ registerMode ? '注册并登录' : '登录' }}
                    </el-button>

                    <p class="form-switch">
                        {{ registerMode ? '已经有账号了？' : '还没有账号？' }}
                        <el-link type="primary" :underline="false" @click="registerMode = !registerMode">
                            {{ registerMode ? '返回登录' : '立即注册' }}
                        </el-link>
                    </p>
                </el-form>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { login, register } from '@/api';

const router = useRouter();
const loginRef = ref<FormInstance>();
const loading = ref(false);
const registerMode = ref(false);

const param = reactive({
    username: '',
    password: ''
});

const rules: FormRules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const submitForm = () => {
    if (!loginRef.value) return;
    loginRef.value.validate(async valid => {
        if (!valid) return;

        loading.value = true;
        try {
            if (registerMode.value) {
                await register({ ...param, nickname: param.username });
                ElMessage.success('注册成功，请登录');
                registerMode.value = false;
                return;
            }
            const data = await login({ ...param });
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
            ElMessage.success('登录成功');
            router.push('/dashboard');
        } catch (e) {
            // 错误提示已经在 request 拦截器里统一弹过
        } finally {
            loading.value = false;
        }
    });
};
</script>

<style scoped>
.login-page {
    display: flex;
    width: 100%;
    height: 100vh;
    background: var(--sl-surface);
}

/* 左侧品牌区 */
.brand {
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    width: 46%;
    max-width: 620px;
    padding: 56px 56px 40px;
    color: #fff;
    background: linear-gradient(150deg, #1d4ed8 0%, #2563eb 45%, #0ea5e9 100%);
    overflow: hidden;
}
.brand::after {
    content: '';
    position: absolute;
    right: -120px;
    bottom: -160px;
    width: 420px;
    height: 420px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
}
.brand-inner {
    position: relative;
    z-index: 1;
}
.brand-logo {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: var(--sl-fs-lg);
    font-weight: 600;
}
.brand-logo img {
    width: 30px;
    height: 30px;
}
.brand-title {
    margin: 56px 0 18px;
    font-size: 40px;
    line-height: 1.25;
    font-weight: 700;
    letter-spacing: 0.5px;
}
.brand-sub {
    font-size: var(--sl-fs-base);
    line-height: 1.9;
    color: rgba(255, 255, 255, 0.85);
}
.brand-points {
    margin-top: 40px;
    list-style: none;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 14px;
    font-size: var(--sl-fs-sm);
    color: rgba(255, 255, 255, 0.9);
}
.brand-points li {
    display: flex;
    align-items: center;
    gap: 10px;
}
.brand-points .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #fff;
    opacity: 0.75;
}
.brand-footer {
    position: relative;
    z-index: 1;
    font-size: var(--sl-fs-xs);
    color: rgba(255, 255, 255, 0.6);
}

/* 右侧表单区 */
.panel {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px;
}
.form-wrap {
    width: 100%;
    max-width: 380px;
}
.form-title {
    margin: 0;
    font-size: var(--sl-fs-2xl);
    font-weight: 700;
    color: var(--sl-text);
}
.form-sub {
    margin: 10px 0 32px;
    font-size: var(--sl-fs-sm);
    color: var(--sl-muted);
}
.login-btn {
    width: 100%;
    margin-top: 8px;
    height: 44px;
    font-size: var(--sl-fs-base);
    border-radius: var(--sl-radius-sm);
}
.form-switch {
    margin-top: 18px;
    text-align: center;
    font-size: var(--sl-fs-sm);
    color: var(--sl-muted);
}

@media (max-width: 900px) {
    .brand {
        display: none;
    }
}
</style>
