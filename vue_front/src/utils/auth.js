export function hasRole(userRole, requiredRoles) {
  if (!requiredRoles || requiredRoles.length === 0) return true
  return requiredRoles.includes(userRole)
}
