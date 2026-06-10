import type { ReviewerDashboardOverview } from '../../types/dashboard'
import { request } from '../http'

export const reviewerDashboardService = {
  getOverview(range = 14): Promise<ReviewerDashboardOverview> {
    return request.get<ReviewerDashboardOverview>('/v1/reviewer/dashboard/overview', {
      params: { range },
    })
  },
}
