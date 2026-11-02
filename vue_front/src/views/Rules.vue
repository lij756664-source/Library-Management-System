<template>
  <el-card class="page-card rules-page" shadow="never">
    <template #header><div class="card-heading"><span>借阅规则设置</span><el-button :icon="QuestionFilled" @click="helpVisible = true">规则说明</el-button></div></template>
    <div class="rules-toolbar"><span class="section-label">编辑规则</span><el-tag :type="unknown ? 'warning' : dirty ? 'warning' : 'info'" size="small">{{ unknown ? '保存结果待核实' : dirty ? '有未保存修改' : '与生效规则一致' }}</el-tag></div>
    <el-alert v-if="unknown" title="保存结果尚未确认" :description="checking ? '正在重新查询实际生效规则，请稍候。' : '暂时无法核实，请恢复连接后点击“重新查询”。确认前请勿重复保存。'" type="warning" show-icon :closable="false" class="save-notice" />
    <el-alert v-if="loadFailed && !unknown" title="当前规则加载失败，请重新查询后再编辑。" type="error" show-icon :closable="false" class="save-notice" />
    <el-form @submit.prevent="save" :disabled="loading || saving">
      <el-table :data="ruleFields" v-loading="loading" stripe class="data-table rules-table">
        <el-table-column prop="name" label="规则名称" min-width="150" />
        <el-table-column label="设置值" min-width="250"><template #default="{ row }"><el-form-item :error="errors[row.key]" class="rule-input-item"><el-input :disabled="!ready" v-model="draft[row.key]" :aria-label="row.name" :inputmode="row.key === 'fine_per_day' ? 'decimal' : 'numeric'" @input="errors[row.key] = ''"><template #append>{{ row.unit }}</template></el-input></el-form-item></template></el-table-column>
        <el-table-column :label="unknown ? '上次确认值' : '当前生效'" min-width="160"><template #default="{ row }"><span class="effective-value">{{ formatRule(row, current[row.key]) }}</span></template></el-table-column>
      </el-table>
      <div class="rules-actions"><el-button type="primary" :icon="Check" :loading="saving" :disabled="loading || unknown || !ready" @click="save">整组保存</el-button><el-button :icon="Refresh" :disabled="saving || loading" @click="load(true)">重新查询</el-button></div>
    </el-form>
    <BorrowRulesDialog v-model="helpVisible" help-only />
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh, QuestionFilled } from '@element-plus/icons-vue'
import request from '../api/request'
import BorrowRulesDialog from '../components/BorrowRulesDialog.vue'
import { ruleFields, formatRule, validateRule } from '../constants/borrowRules'
const current = ref({}), draft = ref({}), errors = ref({})
const loading = ref(false), saving = ref(false), unknown = ref(false), checking = ref(false)
const ready = ref(false), loadFailed = ref(false), helpVisible = ref(false)
const dirty = computed(() => ready.value && ruleFields.some(field => String(draft.value[field.key]) !== String(current.value[field.key])))
async function load(notify = false) {
  loading.value = true
  try {
    current.value = (await request.get('/rules', { silentError: true })).data
    draft.value = { ...current.value }
    errors.value = {}; unknown.value = false; ready.value = true; loadFailed.value = false
    if (notify) ElMessage.success('已重新查询当前生效规则')
  } catch { loadFailed.value = true; if (!unknown.value) ElMessage.error('当前规则加载失败，请稍后重新查询') }
  finally { loading.value = false }
}
async function save() {
  if (saving.value || loading.value || unknown.value || !ready.value) return
  errors.value = Object.fromEntries(ruleFields.map(field => [field.key, validateRule(field, draft.value[field.key])]))
  if (Object.values(errors.value).some(Boolean)) return
  saving.value = true
  try {
    current.value = (await request.put('/rules', { ...draft.value }, { silentError: true })).data
    draft.value = { ...current.value }; unknown.value = false
    ElMessage.success('规则已保存并生效')
  } catch (error) {
    const uncertain = !error.businessCode && !error.response
    if (!uncertain) { ElMessage.error(error.message || '保存失败，请重新查询'); return }
    unknown.value = true; checking.value = true
    try {
      current.value = (await request.get('/rules', { silentError: true })).data
      unknown.value = false
      ElMessage.info('已核实当前生效规则，请对照查看；未保存输入已保留')
    } catch { /* 保持未知状态，等待用户主动核实。 */ }
    finally { checking.value = false }
  } finally { saving.value = false }
}
onMounted(() => load())
</script>

<style scoped>
.card-heading, .rules-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.rules-toolbar { margin-bottom: 16px; justify-content: flex-start; }
.section-label { font-size: 14px; color: var(--color-text-secondary); }
.rule-input-item { margin: 10px 0 16px; max-width: 300px; }
.effective-value { font-variant-numeric: tabular-nums; color: var(--color-text); }
.rules-actions { display: flex; flex-wrap: wrap; gap: 12px; padding-top: 24px; }
.rules-actions .el-button + .el-button { margin-left: 0; }
.save-notice { margin-bottom: 16px; }
</style>
