import type {
  AssignableReviewer,
  AssignableReviewerQuery,
  AssignableTask,
  AssignableTaskQuery,
  PageResponse,
  ReviewerProgress,
  ReviewerProgressQuery,
} from '../../types/adminReviewAssignment'
import { request } from '../http'

function compactParams(params: object) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

export const adminReviewAssignmentService = {
  listAssignableTasks(query: AssignableTaskQuery) {
    return request.get<PageResponse<AssignableTask>>('/v1/admin/review/tasks/assignable', {
      params: compactParams(query),
    })
  },

  listAssignableReviewers(query: AssignableReviewerQuery) {
    return request.get<PageResponse<AssignableReviewer>>('/v1/admin/review/reviewers/assignable', {
      params: compactParams(query),
    })
  },

  listReviewerProgress(query: ReviewerProgressQuery) {
    return request.get<ReviewerProgress[]>('/v1/admin/review/reviewers/progress', {
      params: compactParams(query),
    })
  },
}
