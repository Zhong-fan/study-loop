<template>
    <div ref="el" class="base-chart" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';

/**
 * 极简 ECharts 封装：
 * - 传 option 就渲染，option 变了自动更新
 * - 容器尺寸变化自动 resize（侧栏折叠、窗口缩放都能跟上）
 * - 卸载时 dispose，避免切页面后内存泄漏
 */
const props = withDefaults(
    defineProps<{
        option: echarts.EChartsOption;
        height?: string;
    }>(),
    { height: '260px' }
);

const el = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;
let observer: ResizeObserver | null = null;

const render = () => {
    if (!el.value) {
        return;
    }
    if (!chart) {
        chart = echarts.init(el.value);
    }
    chart.setOption(props.option, true);
};

onMounted(() => {
    render();
    if (el.value) {
        observer = new ResizeObserver(() => chart?.resize());
        observer.observe(el.value);
    }
});

watch(() => props.option, render, { deep: true });

onBeforeUnmount(() => {
    observer?.disconnect();
    chart?.dispose();
    chart = null;
});
</script>

<style scoped>
.base-chart {
    width: 100%;
}
</style>
