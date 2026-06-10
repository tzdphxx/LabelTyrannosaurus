import type { LabelerDashboardOverview } from '../../types/dashboard'
import { request } from '../http'

export const labelerDashboardService = {
  getOverview(range = 14): Promise<LabelerDashboardOverview> {
    return request.get<LabelerDashboardOverview>('/v1/labeler/dashboard/overview', {
      params: { range },
    })
  },
}
