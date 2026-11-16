<template>
    <div class="login-bg">
        <div class="login-container">
            <div class="login-header">
                <img class="logo mr10" src="../../assets/img/logo.svg" alt="" />
                <div class="login-title">CMMR 学习助手</div>
            </div>
            <el-form :model="param" :rules="rules" ref="loginRef" size="large">
                <el-form-item prop="username">
                    <el-input v-model="param.username" placeholder="用户名" />
                </el-form-item>
                <el-form-item prop="password">
                    <el-input
                        type="password"
                        placeholder="密码"
                        v-model="param.password"
                        @keyup.enter="submitForm"
                    />
                </el-form-item>
                <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="submitForm">
                    登录
                </el-button>
                <p class="login-text">
                    没有账号？
                    <el-link type="primary" @click="registerMode = !registerMode">
                        {{ registerMode ? '返回登录' : '立即注册' }}
                    </el-link>
                </p>
            </el-form>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
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
                JSON.stringify({ userId: data.userId, username: data.username, nickname: data.nickname })
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
.login-bg {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100vh;
    background: #eef0fc;
}
.login-container {
    width: 380px;
    padding: 30px 30px 20px;
    border-radius: 8px;
    background: #fff;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}
.login-header {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;
}
.logo {
    width: 34px;
}
.login-title {
    font-size: 20px;
    font-weight: 600;
}
.login-btn {
    width: 100%;
}
.login-text {
    margin-top: 12px;
    text-align: center;
    font-size: 13px;
    color: #909399;
}
.mr10 {
    margin-right: 10px;
}
</style>
