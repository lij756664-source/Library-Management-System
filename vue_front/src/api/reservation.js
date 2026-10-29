import request from './request'

export function reserveBook(bookId) {
  return request.post('/reservations', { bookId })
}

export function cancelReservation(id) {
  return request.delete(`/reservations/${id}`)
}

export function pickupReservation(id) {
  return request.put(`/reservations/${id}/pickup`)
}

export function getMyReservations(params) {
  return request.get('/reservations/my', { params })
}

export function getAllReservations(params) {
  return request.get('/reservations', { params })
}
