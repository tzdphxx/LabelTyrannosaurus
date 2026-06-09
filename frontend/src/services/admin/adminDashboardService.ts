import type { AdminDashboardOverview, AdminDashboardRange } from '../../types/admin'
import { request } from '../http'

export const adminDashboardService = {
  getOverview(range: AdminDashboardRange = '7d'): Promise<AdminDashboardOverview> {
    return request.get<AdminDashboardOverview>('/v1/admin/dashboard/overview', {
      params: { range },
    })
  },
}
