import request from './request'

export function getDashboard() {
  return request.get('/statistics/dashboard')
}

export function getCategoryStats() {
  return request.get('/statistics/category')
}

export function getTopBooks(limit = 10) {
  return request.get('/statistics/top-books', { params: { limit } })
}

export function getOverview() {
  return request.get('/statistics/overview')
}
