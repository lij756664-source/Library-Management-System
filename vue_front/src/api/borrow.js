import request from './request'

/**
 * 图书借阅
 * POST /api/borrows   body: { bookId }
 * 不传 userId，后端用 token 识别当前登录读者
 */
export function borrowBook(bookId) {
  return request.post('/borrows', { bookId })
}

/**
 * 图书续借
 * PUT /api/borrows/{借阅记录id}/renew
 */
export function renewBook(id) {
  return request.put(`/borrows/${id}/renew`)
}

/**
 * 图书归还（读者还书 / 管理员代还都走这个接口）
 * PUT /api/borrows/{借阅记录id}/return
 */
export function returnBook(id) {
  return request.put(`/borrows/${id}/return`)
}

/**
 * 借阅记录查询（读者：只查自己）
 * GET /api/borrows/my?status=&keyword=
 */
export function getMyBorrows(params) {
  return request.get('/borrows/my', { params })
}

/**
 * 借阅记录查询（管理员：全馆记录）
 * GET /api/borrows?status=&keyword=
 */
export function getAllBorrows(params) {
  return request.get('/borrows', { params })
}

/** 借阅规则：借期、续借次数、罚款等，来自 system_config */
export function getBorrowRules() {
  return request.get('/borrows/rules')
}
