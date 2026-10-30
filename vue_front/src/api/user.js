import request from './request'

/** 查询当前登录用户个人信息 */
export function getProfile() {
  return request.get('/user/profile')
}

/** 修改个人信息（昵称/邮箱/手机号） */
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

/** 修改密码（需原密码） */
export function changePassword(data) {
  return request.put('/user/password', data)
}

/** 上传头像（multipart 表单，字段名 file），返回 { url } */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/* ===== 管理员-用户管理接口 ===== */

/** 分页查询账号列表（username 模糊、status、role 筛选） */
export function getUsers(params) {
  return request.get('/admin/users', { params })
}

/** 查询账号详情（含角色列表） */
export function getUserDetail(id) {
  return request.get(`/admin/users/${id}`)
}

/** 启用/停用账号（status: 1-启用 0-停用） */
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

/** 修改账号资料（昵称/邮箱/手机号） */
export function updateUserInfo(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

/** 重置账号密码 */
export function resetUserPassword(id, password) {
  return request.put(`/admin/users/${id}/password`, { password })
}
