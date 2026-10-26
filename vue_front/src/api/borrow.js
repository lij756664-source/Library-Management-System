import request from './request'

export function borrowBook(bookId) {
  return request.post('/borrows', { bookId })
}

export function renewBook(id) {
  return request.put(`/borrows/${id}/renew`)
}

export function returnBook(id) {
  return request.put(`/borrows/${id}/return`)
}

export function getMyBorrows(params) {
  return request.get('/borrows/my', { params })
}

export function getAllBorrows(params) {
  return request.get('/borrows', { params })
}

export function getBorrowRules() {
  return request.get('/borrows/rules')
}
