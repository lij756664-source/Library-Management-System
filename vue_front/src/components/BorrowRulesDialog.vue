<template>
  <el-dialog :model-value="modelValue" :title="helpOnly ? '借阅规则说明' : '最新借阅规则'" width="620px" class="borrow-rules-dialog" @update:model-value="$emit('update:modelValue', $event)">
    <div v-loading="loading">
      <el-table :data="ruleFields" stripe class="data-table">
        <el-table-column prop="name" label="规则" min-width="130" />
        <el-table-column v-if="!helpOnly" label="当前规则" min-width="140"><template #default="{ row }">{{ formatRule(row, values[row.key]) }}</template></el-table-column>
        <el-table-column v-if="helpOnly" label="可设范围" width="120"><template #default="{ row }">{{ row.min }}—{{ row.key === 'fine_per_day' ? '5.00' : row.max }} {{ row.unit }}</template></el-table-column>
        <el-table-column v-if="helpOnly" prop="help" label="说明" min-width="220" />
      </el-table>
      <p class="rules-note">新借阅使用最新规则，已有借阅保留借出时的约定；新增借阅的数量限制始终使用最新上限。</p>
      <p v-if="failed" class="rules-error" role="alert">规则加载失败，请重试。</p>
    </div>
    <template #footer><el-button v-if="!helpOnly" :loading="loading" @click="load">刷新规则</el-button><el-button type="primary" @click="$emit('update:modelValue', false)">我知道了</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import request from '../api/request'
import { ruleFields, formatRule } from '../constants/borrowRules'
const props = defineProps({ modelValue: Boolean, helpOnly: Boolean })
defineEmits(['update:modelValue'])
const loading = ref(false), values = ref({}), failed = ref(false)
async function load() {
  loading.value = true
  failed.value = false
  try { values.value = (await request.get('/rules/latest', { silentError: true })).data }
  catch { values.value = {}; failed.value = true }
  finally { loading.value = false }
}
watch(() => props.modelValue, (open) => { if (open && !props.helpOnly) load() })
</script>

<style scoped>
.rules-note { margin: 18px 0 0; line-height: 1.8; font-size: 13px; color: var(--color-text-secondary); }
.rules-error { color: var(--color-danger-soft); font-size: 13px; }
</style>
