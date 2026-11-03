// 金额与天数统一由后端计算，0 元也是有效结果。
export function calcDisplayFine(row) { return Number(row.fineAmount ?? 0) }
export function isEstimatedFine(row) { return row.status === 'BORROWING' }
