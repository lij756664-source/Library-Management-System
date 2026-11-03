import request from './request'

export function getBooks(params) {
  return request.get('/books', { params })
}

export function getBook(id) {
  return request.get(`/books/${id}`)
}

export function addBook(data) {
  return request.post('/books', data)
}

export function updateBook(id, data) {
  return request.put(`/books/${id}`, data)
}

export function deleteBook(id) {
  return request.delete(`/books/${id}`)
}

/** 管理员增减馆藏库存：PUT /api/books/{id}/stock  body: { delta } */
export function adjustBookStock(id, delta) {
  return request.put(`/books/${id}/stock`, { delta })
}
