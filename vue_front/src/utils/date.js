export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 19)
}

export function getRemainText(remainDays, overdue) {
  if (overdue) {
    const days = Math.abs(Number(remainDays) || 0)
    return `已逾期 ${days} 天`
  }
  const days = Number(remainDays) || 0
  if (days <= 0) return '今日到期'
  return `剩余 ${days} 天`
}
