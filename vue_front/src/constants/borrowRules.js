export const ruleFields = [
  { key: 'borrow_days', name: '借阅期限', unit: '天', min: 1, max: 90, defaultValue: '30', help: '借出日不计入期限。' },
  { key: 'max_renew_count', name: '续借次数上限', unit: '次', min: 0, max: 5, defaultValue: '2', help: '0 表示禁止续借。' },
  { key: 'renew_days', name: '每次续借天数', unit: '天', min: 1, max: 30, defaultValue: '7', help: '从当前应还日期延长；禁止续借时也须填写。' },
  { key: 'fine_per_day', name: '每日罚金', unit: '元 / 天', min: 0, max: 5, defaultValue: '0.50', help: '最多两位小数；0 元不收费，但仍认定逾期。' },
  { key: 'max_borrow_count', name: '同时借阅上限', unit: '册', min: 1, max: 10, defaultValue: '5', help: '逾期未还计入册数；降低上限不改变已有借阅。' }
]

export function formatRule(field, value) {
  if (value === undefined || value === null) return '—'
  return (field.key === 'fine_per_day' ? Number(value).toFixed(2) : value) + ' ' + field.unit
}

export function validateRule(field, value) {
  if (value === undefined || value === null || String(value).trim() === '') return '请填写' + field.name
  const money = field.key === 'fine_per_day'
  const valid = money ? /^[0-9]+(\.[0-9]{1,2})?$/.test(String(value)) : /^[0-9]+$/.test(String(value))
  if (!valid) return field.name + (money ? '最多保留两位小数，且须为非负数' : '须为整数')
  if (Number(value) < field.min || Number(value) > field.max) return field.name + '的范围为 ' + field.min + '—' + (money ? '5.00' : field.max)
  return ''
}
