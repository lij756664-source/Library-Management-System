<template>
  <span class="status-tag" :class="tagClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  overdue: { type: Boolean, default: false }
})

const label = computed(() => {
  if (props.status === 'RETURNED') return '已归还'
  if (props.overdue) return '已逾期'
  return '借阅中'
})

const tagClass = computed(() => {
  if (props.status === 'RETURNED') return 'status-returned'
  if (props.overdue) return 'status-overdue'
  return 'status-borrowing'
})
</script>

<style scoped>
.status-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 6px;
  font-weight: 400;
}

.status-borrowing {
  background: var(--color-green-light);
  color: var(--color-green);
}

.status-overdue {
  background: #fce4ec;
  color: var(--color-danger-soft);
}

.status-returned {
  background: #eef1f4;
  color: var(--color-text-muted);
}
</style>
