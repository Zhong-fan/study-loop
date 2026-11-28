<template>
    <div class="mx-auto max-w-3xl space-y-4">
        <div>
            <h1 class="text-2xl font-semibold tracking-tight text-ink-strong">预测卷</h1>
            <p class="mt-1 text-sm text-ink-muted">按重点标注、例题、重复出现和原文依据排序。</p>
        </div>

        <section class="rounded-xl border border-line bg-surface p-4">
            <div class="flex flex-wrap items-center gap-3">
                <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="load">
                    <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
                <el-input-number v-model="size" :min="1" :max="30" />
                <span class="text-sm text-ink-muted">题</span>
                <button
                    type="button"
                    class="rounded-md bg-brand px-4 py-2 text-sm text-white disabled:opacity-40"
                    :disabled="!courseId || loading"
                    @click="build"
                >
                    {{ loading ? '生成中…' : '生成预测卷' }}
                </button>
            </div>

            <p v-if="lowConfidence" class="mt-3 rounded-md bg-warn-soft px-3 py-2 text-xs text-warn">
                有些题找不到原文出处，参考价值有限。
            </p>
        </section>

        <section class="overflow-hidden rounded-xl border border-line bg-surface">
            <div class="flex items-center justify-between border-b border-line-soft px-4 py-3">
                <span class="text-sm font-medium text-ink">题目（{{ items.length }}）</span>
                <span v-if="items.length" class="text-xs text-ink-muted">按可能性从高到低</span>
            </div>

            <div v-if="loading" class="space-y-3 p-4">
                <div v-for="i in 4" :key="i" class="h-16 animate-pulse rounded-lg bg-surface-2"></div>
            </div>

            <div v-else-if="items.length === 0" class="px-4 py-12 text-center text-sm text-ink-muted">
                先选课程，再生成预测卷。
            </div>

            <div v-else class="divide-y divide-line-soft">
                <article v-for="(item, index) in items" :key="item.questionId" class="p-4">
                    <div class="flex items-start gap-3">
                        <span class="mt-0.5 text-xs text-ink-subtle">{{ index + 1 }}</span>
                        <div class="min-w-0 flex-1">
                            <div class="flex flex-wrap items-center gap-2">
                                <span class="rounded-full bg-brand-soft px-2 py-0.5 text-xs text-ink-blue">{{ typeText(item.type) }}</span>
                                <span class="rounded-full bg-warn-soft px-2 py-0.5 text-xs text-warn">
                                    可能性 {{ Math.round(item.weight * 100) }}%
                                </span>
                            </div>
                            <p class="mt-2 text-sm leading-relaxed text-ink">{{ item.stem }}</p>
                            <p class="mt-1 text-xs text-ink-muted">{{ item.reason }}</p>

                            <details v-if="item.evidenceQuote" class="mt-2 text-xs text-ink-muted">
                                <summary class="cursor-pointer text-brand">看依据</summary>
                                <div class="mt-2 rounded-md bg-reader p-3">
                                    <p class="mb-1">第 {{ item.evidencePage }} 页</p>
                                    <p class="leading-relaxed">{{ item.evidenceQuote }}</p>
                                </div>
                            </details>
                        </div>
                    </div>
                </article>
            </div>
        </section>
    </div>
</template>


<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getCourses, getPredict } from '@/api';

const courses = ref<any[]>([]);
const courseId = ref<number | null>(null);
const size = ref(10);
const loading = ref(false);
const items = ref<any[]>([]);

const lowConfidence = computed(() => items.value.some((item) => !item.evidenceQuote));

const typeText = (type: string) =>
    ({ SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', BLANK: '填空', SUBJECTIVE: '简答', COMPOSITE: '综合题' } as any)[type] || type;

const build = async () => {
    loading.value = true;
    try {
        const data = await getPredict(courseId.value as number, size.value);
        items.value = data.items || [];
        if (items.value.length === 0) {
            ElMessage.warning(data.message || '还没有可用的题目，先去 AI 出题');
        }
    } finally {
        loading.value = false;
    }
};

const load = async () => {
    items.value = [];
    if (courseId.value) {
        await build();
    }
};

onMounted(async () => {
    const data = await getCourses({ page: 1, size: 100 });
    courses.value = data.records;
    if (courses.value.length > 0) {
        courseId.value = courses.value[0].id;
        await build();
    }
});
</script>
