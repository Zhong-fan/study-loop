<template>
    <div class="tools mx-auto max-w-4xl">
        <header class="mb-5">
            <h1 class="text-2xl font-semibold tracking-tight text-ink-strong">工具与资料</h1>
        </header>

        <section v-for="group in groups" :key="group.title" class="mb-6">
            <div class="mb-2 flex items-center justify-between text-xs text-ink-muted">
                <span>{{ group.title }}</span>
                <span>{{ group.items.length }}</span>
            </div>

            <div class="grid gap-2 sm:grid-cols-2">
                <button
                    v-for="item in group.items"
                    :key="item.path"
                    type="button"
                    class="row flex items-center gap-3 rounded-lg border border-line bg-surface p-4 text-left transition-colors hover:border-brand hover:bg-surface-2"
                    @click="router.push(item.path)"
                >
                    <span class="text-xs text-ink-subtle">{{ item.no }}</span>
                    <span class="min-w-0 flex-1">
                        <b class="block text-sm font-medium text-ink">{{ item.title }}</b>
                        <em class="mt-0.5 block truncate text-xs not-italic text-ink-muted">{{ item.desc }}</em>
                    </span>
                    <span class="text-ink-muted" aria-hidden="true">→</span>
                </button>
            </div>
        </section>
    </div>
</template>


<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const stored = JSON.parse(localStorage.getItem('user') || '{}');
const isAdmin = stored.role === 'ADMIN';

const groups = computed(() => {
    const raw = [
        {
            title: '资料与题目',
            items: [
                { path: '/courses', title: '课程与资料', desc: '资料上传与解析' },
                { path: '/questions', title: '题库', desc: '题目维护' },
                { path: '/exams', title: '真题', desc: '历年真题' }
            ]
        },
        {
            title: '练习与预测',
            items: [
                { path: '/quiz', title: '自测', desc: '生成自测卷' },
                { path: '/ai', title: 'AI 出题', desc: '按知识点出题' },
                { path: '/predict', title: '预测卷', desc: '生成预测卷' }
            ]
        },
        {
            title: '数据',
            items: [{ path: '/stats', title: '统计', desc: '覆盖率与掌握度' }]
        }
    ];

    if (isAdmin) {
        raw.push({
            title: '系统',
            items: [{ path: '/admin', title: '用户管理', desc: '账号与 AI 配置' }]
        });
    }

    let counter = 0;
    return raw.map((group) => ({
        ...group,
        items: group.items.map((item) => ({
            ...item,
            no: String(++counter).padStart(2, '0')
        }))
    }));
});
</script>
