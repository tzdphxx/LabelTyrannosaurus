import type { AdminCreateReviewerRequest, AdminUserResponse } from '../../types/admin'
import { request } from '../http'

export const adminUserService = {
  createReviewer(payload: AdminCreateReviewerRequest): Promise<AdminUserResponse> {
    return request.post<AdminUserResponse, AdminCreateReviewerRequest>('/v1/admin/users/reviewers', payload)
  },
}
