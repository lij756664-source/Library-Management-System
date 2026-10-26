/** 计算展示用罚款金额（后端未返回预估时前端兜底） */
export function calcDisplayFine(row, finePerDay = 0.5) {
  if (row.status === 'RETURNED') {
    return Number(row.fineAmount ?? 0)
  }
  if (row.status === 'BORROWING' && row.overdue) {
    const backendFine = Number(row.fineAmount ?? 0)
    if (backendFine > 0) {
      return backendFine
    }
    const days = row.overdueDays > 0
      ? row.overdueDays
      : Math.abs(Number(row.remainDays) || 0)
    if (days > 0) {
      return Number((days * finePerDay).toFixed(2))
    }
  }
  return Number(row.fineAmount ?? 0)
}

export function isEstimatedFine(row) {
  return row.status === 'BORROWING' && row.overdue
}
