import request from './request'

/** 分页查询操作日志（username 模糊、operation 精确筛选，仅管理员） */
export function getLogs(params) {
  return request.get('/admin/logs', { params })
}
