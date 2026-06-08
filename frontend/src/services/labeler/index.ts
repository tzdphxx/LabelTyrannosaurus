import { isRealServiceMode } from '../http'
export { labelerDashboardService } from './labelerDashboardService'
import { mockLabelingService } from './labelingService'
import { realLabelingService } from './labelingRealService'

export const labelingService = isRealServiceMode() ? realLabelingService : mockLabelingService
