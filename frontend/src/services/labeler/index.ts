import { isRealServiceMode } from '../http'
import { mockLabelingService } from './labelingService'
import { realLabelingService } from './labelingRealService'

export const labelingService = isRealServiceMode() ? realLabelingService : mockLabelingService
