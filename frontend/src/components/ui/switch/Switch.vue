<script setup lang="ts">
import type { SwitchRootEmits, SwitchRootProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { SwitchRoot, SwitchThumb, useForwardPropsEmits } from 'reka-ui'
import { cn } from '@/lib/utils'

/**
 * 开关。项目里第一个「开关」语义的控件（此前二态一律用 Checkbox）。
 *
 * <p>⚠️ 形状口径跟着 shape 标尺走：轨道 `rounded-full`（唯一的例外——开关的胶囊形状本身就是语义），
 * 尺寸与 Checkbox 那一档对齐（轨道 36×20，滑块 16）。
 */
const props = defineProps<SwitchRootProps & { class?: HTMLAttributes['class'] }>()
const emits = defineEmits<SwitchRootEmits>()

const delegatedProps = reactiveOmit(props, 'class')
const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <SwitchRoot
    data-slot="switch"
    v-bind="forwarded"
    :class="
      cn(
        'peer inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border border-transparent transition-colors outline-none',
        'data-[state=unchecked]:bg-line data-[state=checked]:bg-brand',
        'focus-visible:ring-focus focus-visible:ring-3',
        'disabled:cursor-not-allowed disabled:opacity-50',
        props.class
      )
    "
  >
    <SwitchThumb
      data-slot="switch-thumb"
      class="bg-reader pointer-events-none block size-4 rounded-full shadow-xs ring-0 transition-transform data-[state=checked]:translate-x-4 data-[state=unchecked]:translate-x-0.5"
    />
  </SwitchRoot>
</template>
