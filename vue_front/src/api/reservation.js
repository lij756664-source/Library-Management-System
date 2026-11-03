import request from './request'

/**
 * 无库存图书预约：加入 WAITING 排队
 * POST /api/reservations   body: { bookId }
 */
export function reserveBook(bookId) {
  return request.post('/reservations', { bookId })
}

/**
 * 取消预约：WAITING/READY 可取消；若取消的是待取书，队列会顺延
 * DELETE /api/reservations/{预约id}
 */
export function cancelReservation(id) {
  return request.delete(`/reservations/${id}`)
}

/**
 * 待取书后领取，预约转为正式借阅（FULFILLED）
 * PUT /api/reservations/{预约id}/pickup
 */
export function pickupReservation(id) {
  return request.put(`/reservations/${id}/pickup`)
}

/** 我的预约列表 GET /api/reservations/my?status= */
export function getMyReservations(params) {
  return request.get('/reservations/my', { params })
}

/** 管理员预约列表 GET /api/reservations?keyword=&status= */
export function getAllReservations(params) {
  return request.get('/reservations', { params })
}
