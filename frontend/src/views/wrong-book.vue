<template>
    <div class="mx-auto max-w-3xl">
        <header class="mb-4 flex items-end justify-between">
            <div>
                <h1 class="text-2xl font-semibold tracking-tight text-ink-strong">错题本</h1>
                <p class="mt-1 text-sm text-ink-muted">最后一次作答仍未满分的题</p>
            </div>
            <span class="text-xs text-ink-muted">{{ rows.length }} 题</span>
        </header>

        <div v-if="loading" class="space-y-3">
            <div v-for="i in 3" :key="i" class="h-24 animate-pulse rounded-xl border border-line bg-surface"></div>
        </div>

        <div v-else-if="!rows.length" class="rounded-xl border border-line bg-surface p-10 text-center">
            <p class="text-sm text-ink-muted">还没有错题。</p>
            <RouterLink to="/practice" class="mt-4 inline-flex text-sm text-brand no-underline hover:underline">
                去做题
            </RouterLink>
        </div>

        <div v-else class="space-y-3">
            <article v-for="row in rows" :key="row.id" class="rounded-xl border border-line bg-surface p-4">
                <div class="flex items-start justify-between gap-4">
                    <div class="min-w-0">
                        <div class="mb-2 flex flex-wrap items-center gap-2">
                            <span class="rounded-full bg-brand-soft px-2 py-0.5 text-xs text-ink-blue">{{ typeText(row.type) }}</span>
                            <span v-if="row.knowledgeId" class="text-xs text-ink-muted">知识点 #{{ row.knowledgeId }}</span>
                            <span v-if="row.materialId" class="text-xs text-ink-muted">第 {{ row.pageNo }} 页</span>
                        </div>
                        <p class="text-sm leading-relaxed text-ink">{{ row.stem }}</p>
                    </div>
                    <button
                        type="button"
                        class="shrink-0 rounded-md border border-line px-2.5 py-1 text-xs text-ink hover:bg-surface-2"
                        @click="toggle(row.id)"
                    >
                        {{ open === row.id ? '收起' : '看答案' }}
                    </button>
                </div>

                <div v-if="open === row.id" class="mt-4 border-t border-line-soft pt-4">
                    <div v-if="row.options?.length" class="space-y-2">
                        <div
                            v-for="(option, i) in row.options"
                            :key="i"
                            class="flex gap-3 rounded-md border px-3 py-2 text-sm"
                            :class="isCorrect(row, i) ? 'border-ok bg-ok-soft' : 'border-line'"
                        >
                            <span class="text-xs text-ink-muted">{{ String.fromCharCode(65 + i) }}</span>
                            <span class="text-ink">{{ option }}</span>
                        </div>
                    </div>
                    <p class="mt-3 text-sm"><span class="text-xs text-ink-muted">答案</span> {{ (row.answer || []).join('、') || '—' }}</p>
                    <p v-if="row.analysis" class="mt-2 text-sm text-ink-muted">解析：{{ row.analysis }}</p>
                    <p v-if="row.quote" class="mt-2 text-xs text-ink-muted">原文：{{ row.quote }}</p>
                </div>
            </article>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getWrongQuestions } from '@/api';

const loading = ref(true);
const rows = ref<any[]>([]);
const open = ref<number | null>(null);

const typeMap: Record<string, string> = {
    SINGLE: '单选',
    MULTIPLE: '多选',
    JUDGE: '判断',
    BLANK: '填空',
    SUBJECTIVE: '主观',
    IMAGE: '图片',
    SHORT: '简答',
    COMPOSITE: '综合'
};

const typeText = (type: string) => typeMap[type] || type || '题目';

const isCorrect = (row: any, index: number) => {
    const letter = String.fromCharCode(65 + index);
    return (row.answer || []).includes(letter);
};

const toggle = (id: number) => {
    open.value = open.value === id ? null : id;
};

onMounted(async () => {
    try {
        rows.value = (await getWrongQuestions()) || [];
    } finally {
        loading.value = false;
    }
});
</script>
