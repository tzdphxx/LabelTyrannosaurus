import type { OwnerDashboardOverview } from '../../types/dashboard'
import { request } from '../http'

export const ownerDashboardService = {
  getOverview(trendDays = 7): Promise<OwnerDashboardOverview> {
    return request.get<OwnerDashboardOverview>('/v1/owner/dashboard/overview', {
      params: { trendDays },
    })
  },
}
