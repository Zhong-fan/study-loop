<template>
    <div class="tools">
        <header class="head">
            <h1>工具与资料</h1>
        </header>

        <section v-for="group in groups" :key="group.title" class="group">
            <div class="group-head">
                <span class="mono">{{ group.title }}</span>
                <span class="mono">{{ group.items.length }}</span>
            </div>

            <button
                v-for="item in group.items"
                :key="item.path"
                type="button"
                class="row"
                @click="router.push(item.path)"
            >
                <span class="no">{{ item.no }}</span>
                <span class="body">
                    <b>{{ item.title }}</b>
                    <em>{{ item.desc }}</em>
                </span>
                <span class="arrow" aria-hidden="true">→</span>
            </button>
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

<style scoped>
.tools {
    max-width: 860px;
    margin: 0 auto;
    padding-top: 56px;
}

.head .eyebrow {
    padding-bottom: 14px;
    border-bottom: 1px solid var(--sl-line-strong);
}

.head h1 {
    margin: 32px 0 0;
    font-size: clamp(36px, 4.6vw, 52px);
    line-height: 1.15;
    font-weight: 400;
    letter-spacing: -0.02em;
}

.dek {
    max-width: 520px;
    margin: 16px 0 0;
    font-size: 15px;
    line-height: 1.9;
    color: var(--sl-text-secondary);
}

.group {
    margin-top: 46px;
}
.group-head {
    display: flex;
    justify-content: space-between;
    padding-bottom: 10px;
    color: var(--sl-muted);
    border-bottom: 1px solid var(--sl-text);
}

.row {
    display: grid;
    grid-template-columns: 56px 1fr 32px;
    align-items: baseline;
    gap: 8px;
    width: 100%;
    padding: 22px 4px;
    text-align: left;
    background: none;
    border: 0;
    border-bottom: 1px solid var(--sl-border);
    cursor: pointer;
    transition: background 0.2s ease, padding-left 0.2s ease;
}
.row:hover {
    padding-left: 12px;
    background: rgba(95, 2, 62, 0.035);
}

.no {
    font-family: var(--sl-font-mono);
    font-size: 10px;
    letter-spacing: 0.08em;
    color: var(--sl-muted);
}

.body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
}
.body b {
    font-family: var(--sl-font-display);
    font-size: 22px;
    font-weight: 400;
    color: var(--sl-text);
}
.body em {
    font-style: normal;
    font-size: 13px;
    line-height: 1.6;
    color: var(--sl-muted);
}

.arrow {
    font-size: 17px;
    color: var(--sl-muted);
    transition: transform 0.22s ease, color 0.22s ease;
}
.row:hover .arrow {
    color: var(--sl-primary);
    transform: translateX(4px);
}

@media (max-width: 640px) {
    .tools {
        padding-top: 32px;
    }
    .row {
        grid-template-columns: 40px 1fr 24px;
        padding: 18px 2px;
    }
    .body b {
        font-size: 19px;
    }
}
</style>
