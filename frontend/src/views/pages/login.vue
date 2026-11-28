<template>
    <div class="login-page flex min-h-screen flex-col items-center justify-center gap-6 bg-canvas p-6 font-sans text-base text-ink">
        <div class="grid w-full max-w-2xl overflow-hidden rounded-xl bg-tint-blue shadow-sm sm:grid-cols-[1fr_1.1fr]">
            <div class="flex flex-col justify-center p-6 sm:p-8">
                <h1 class="text-xl font-semibold tracking-tight text-brand">CMMR</h1>
                <p class="mt-2 text-sm text-ink-muted">课程资料与错题复习</p>
            </div>

            <div class="bg-surface p-6 sm:p-8">
                <div class="mb-5 flex items-center gap-2 text-xs text-ink-muted">
                    <button
                        type="button"
                        class="rounded-sm outline-none hover:text-ink focus-visible:ring-2 focus-visible:ring-focus"
                        :class="{ 'font-medium text-ink': !registerMode }"
                        @click="switchMode(false)"
                    >
                        登录
                    </button>
                    <span class="text-line">/</span>
                    <button
                        type="button"
                        class="rounded-sm outline-none hover:text-ink focus-visible:ring-2 focus-visible:ring-focus"
                        :class="{ 'font-medium text-ink': registerMode }"
                        @click="switchMode(true)"
                    >
                        注册
                    </button>
                </div>

                <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
                    <div class="flex flex-col gap-1.5">
                        <Label for="username" class="text-xs text-ink-muted">用户名</Label>
                        <Input
                            id="username"
                            v-model.trim="form.username"
                            autocomplete="username"
                            placeholder="输入用户名"
                            class="bg-reader"
                            @keyup.enter="submit"
                        />
                    </div>

                    <div v-if="registerMode" class="flex flex-col gap-1.5">
                        <Label for="nickname" class="text-xs text-ink-muted">昵称（可留空）</Label>
                        <Input id="nickname" v-model.trim="form.nickname" placeholder="昵称" class="bg-reader" />
                    </div>

                    <div class="flex flex-col gap-1.5">
                        <Label for="password" class="text-xs text-ink-muted">密码</Label>
                        <Input
                            id="password"
                            v-model="form.password"
                            :type="showPassword ? 'text' : 'password'"
                            :autocomplete="registerMode ? 'new-password' : 'current-password'"
                            :placeholder="registerMode ? '至少 6 位' : '输入密码'"
                            class="bg-reader"
                            @keyup.enter="submit"
                        />
                    </div>

                    <div v-if="registerMode" class="flex flex-col gap-1.5">
                        <Label for="confirm" class="text-xs text-ink-muted">确认密码</Label>
                        <Input
                            id="confirm"
                            v-model="form.confirm"
                            :type="showPassword ? 'text' : 'password'"
                            autocomplete="new-password"
                            placeholder="再输入一次"
                            class="bg-reader"
                            @keyup.enter="submit"
                        />
                    </div>

                    <button
                        type="button"
                        class="-mt-1 self-start text-xs text-ink-muted outline-none hover:text-ink"
                        @click="showPassword = !showPassword"
                    >
                        {{ showPassword ? '隐藏密码' : '显示密码' }}
                    </button>

                    <p v-if="errorMsg" class="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger" role="alert">
                        {{ errorMsg }}
                    </p>

                    <Button type="submit" class="w-full" :disabled="loading">
                        {{ loading ? '处理中…' : registerMode ? '注册' : '登录' }}
                    </Button>
                </form>

                <div v-if="!registerMode" class="mt-5 flex items-center gap-2 border-t border-line-soft pt-4 text-xs text-ink-muted">
                    <span>演示账号</span>
                    <code class="font-mono text-ink">admin / 123456</code>
                    <button type="button" class="ml-auto text-brand outline-none hover:underline" @click="fillDemo">
                        填入
                    </button>
                </div>
            </div>
        </div>

        <p class="text-xs text-ink-subtle">CMMR · {{ year }}</p>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { login, register } from '@/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const showPassword = ref(false);
const errorMsg = ref('');
const registerMode = ref(route.path === '/register');
const year = new Date().getFullYear();

const form = reactive({
    username: '',
    password: '',
    nickname: '',
    confirm: ''
});

watch(
    () => route.path,
    path => {
        registerMode.value = path === '/register';
        errorMsg.value = '';
        form.password = '';
        form.confirm = '';
    }
);

const switchMode = (registering: boolean) => {
    router.push(registering ? '/register' : '/login');
};

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
    errorMsg.value = '';
    if (!form.username || !form.password) {
        errorMsg.value = '请输入用户名和密码';
        return;
    }
    if (registerMode.value && form.password.length < 6) {
        errorMsg.value = '密码至少 6 位';
        return;
    }
    if (registerMode.value && form.password !== form.confirm) {
        errorMsg.value = '两次输入的密码不一致';
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
        } else {
            const data = await login({ username: form.username, password: form.password });
            rememberUser(data);
        }
        (document.activeElement as HTMLElement)?.blur();
        router.push('/dashboard');
    } catch (e: any) {
        errorMsg.value = e?.response?.data?.message || e?.message || '登录失败';
    } finally {
        loading.value = false;
    }
};

const fillDemo = () => {
    form.username = 'admin';
    form.password = '123456';
    errorMsg.value = '';
};
</script>
