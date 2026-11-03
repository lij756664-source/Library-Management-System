import request from './request'

// 统计页统一入口，日期参数只筛选分类和排行榜。
export function getOverview(params = {}) {
  return request.get('/statistics/overview', { params })
}
