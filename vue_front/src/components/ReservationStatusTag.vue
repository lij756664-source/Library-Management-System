<template>
  <span class="status-tag" :class="tagClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const map = {
  WAITING: { label: '排队中', cls: 'status-waiting' },
  READY: { label: '待取书', cls: 'status-ready' },
  FULFILLED: { label: '已取书', cls: 'status-fulfilled' },
  CANCELLED: { label: '已取消', cls: 'status-cancelled' },
  EXPIRED: { label: '已过期', cls: 'status-expired' }
}

const label = computed(() => map[props.status]?.label || props.status)
const tagClass = computed(() => map[props.status]?.cls || '')
</script>

<style scoped>
.status-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 6px;
}
.status-waiting { background: var(--color-primary-light); color: var(--color-primary-hover); }
.status-ready { background: #fff8e1; color: #f9a825; }
.status-fulfilled { background: var(--color-green-light); color: var(--color-green); }
.status-cancelled { background: #eef1f4; color: var(--color-text-muted); }
.status-expired { background: #fce4ec; color: var(--color-danger-soft); }
</style>
