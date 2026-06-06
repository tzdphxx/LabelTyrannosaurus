import { request } from '../http'
import type {
  AiReviewLogQuery,
  AiReviewResultPageResponse,
  AiReviewResultResponse,
  BatchReviewResponse,
  ManualReviewActionPayload,
  ReviewActionResponse,
  ReviewClaimResponse,
  ReviewDetail,
  ReviewQueueItem,
  ReviewQueueQuery,
  ReviewerSubmissionDetail,
  ReviewerSubmissionListItem,
  ReviewerTaskItemPageResponse,
  ReviewerTaskItemQuery,
  ReviewerTaskSummary,
  SubmissionVersion,
} from '../../types/review'
import {
  getReviewerSubmissionItems,
  mapReviewerAiReviewStatusToResult,
  mapReviewerSubmissionDetailToDetail,
  mapReviewerSubmissionToQueueItem,
  matchesAiReviewLogQuery,
  matchesRealQueueQuery,
  normalizeAiReviewResultResponse,
  paginateAiReviewLogs,
  type ReviewerAiReviewStatusResponse,
  type ReviewerSubmissionListResponse,
} from './reviewMappers'

export async function listManualReviewQueue(query: ReviewQueueQuery): Promise<ReviewQueueItem[]> {
  const response = await request.get<ReviewerSubmissionListResponse>('/v1/reviewer/submissions', {
    params: {
      scope: 'CLAIMED',
      page: 1,
      size: 100,
    },
  })
  const items = getReviewerSubmissionItems(response)

  return items.map(mapReviewerSubmissionToQueueItem).filter((item) => matchesRealQueueQuery(item, query))
}

export function listReviewerTasks(): Promise<ReviewerTaskSummary[]> {
  return request.get<ReviewerTaskSummary[]>('/v1/reviewer/tasks')
}

export function listReviewerTaskItems(taskId: string | number, query: ReviewerTaskItemQuery): Promise<ReviewerTaskItemPageResponse> {
  return request.get<ReviewerTaskItemPageResponse>(`/v1/reviewer/tasks/${taskId}/items`, {
    params: query,
  })
}

export async function listClaimableReviewerSubmissions(): Promise<ReviewerSubmissionListItem[]> {
  const response = await request.get<ReviewerSubmissionListResponse>('/v1/reviewer/submissions', {
    params: {
      scope: 'AVAILABLE',
      page: 1,
      size: 100,
    },
  })

  return getReviewerSubmissionItems(response)
}

export async function claimReviewerSubmissions(taskId: string): Promise<ReviewClaimResponse> {
  const response = await request.post<Partial<ReviewClaimResponse>, undefined>(`/v1/reviewer/tasks/${taskId}/claim`, undefined, {
    params: {
      reviewLevel: 1,
    },
  })

  return {
    taskId: response.taskId ?? Number(taskId),
    reviewLevel: response.reviewLevel ?? 1,
    claimedSubmissionIds: response.claimedSubmissionIds ?? [],
    claimedCount: response.claimedCount ?? response.claimedSubmissionIds?.length ?? 0,
  }
}

export async function listReviewHistory(): Promise<ReviewQueueItem[]> {
  const response = await request.get<ReviewerSubmissionListResponse>('/v1/reviewer/submissions', {
    params: {
      scope: 'CLAIMED',
      page: 1,
      size: 100,
    },
  })
  const items = getReviewerSubmissionItems(response)

  return items.map(mapReviewerSubmissionToQueueItem)
}



export async function getReviewDetail(reviewId: string): Promise<ReviewDetail | null> {
  const detail = await request.get<ReviewerSubmissionDetail>(`/v1/reviewer/submissions/${reviewId}`)

  return detail ? mapReviewerSubmissionDetailToDetail(detail) : null
}

export async function listAllAiReviewLogs(query: AiReviewLogQuery): Promise<AiReviewResultPageResponse> {
  const response = await request.get<ReviewerAiReviewStatusResponse>('/v1/reviewer/ai-review-status')
  const items = (response ?? []).map(mapReviewerAiReviewStatusToResult).filter((item) => matchesAiReviewLogQuery(item, query))

  return paginateAiReviewLogs(items, query)
}

export function listTaskAiReviewLogs(taskId: string, query: AiReviewLogQuery): Promise<AiReviewResultPageResponse> {
  return request.get<AiReviewResultPageResponse>(`/v1/tasks/${taskId}/ai-review-logs`, {
    params: query,
  })
}

export async function getSubmissionAiReview(submissionId: string): Promise<AiReviewResultResponse> {
  const response = await request.get<AiReviewResultResponse>(`/v1/submissions/${submissionId}/ai-review`)

  return normalizeAiReviewResultResponse(response)
}

export async function retrySubmissionAiReview(submissionId: string): Promise<AiReviewResultResponse> {
  const response = await request.post<AiReviewResultResponse>(`/v1/submissions/${submissionId}/ai-review/retry`)

  return normalizeAiReviewResultResponse(response)
}

export function listSubmissionVersions(submissionId: string): Promise<SubmissionVersion[]> {
  return request.get<SubmissionVersion[]>(`/v1/submissions/${submissionId}/versions`)
}

export function submitManualReviewActionRequest(reviewId: string, payload: ManualReviewActionPayload): Promise<ReviewActionResponse> {
  if (payload.decision === 'approved') {
    return request.post<ReviewActionResponse, { reviewComment?: string; reviewLevel: number; revisedAnswerJson?: string }>(
      `/v1/reviewer/submissions/${reviewId}/approve`,
      {
        reviewComment: payload.comment,
        reviewLevel: 1,
        revisedAnswerJson: payload.revisedAnswerJson,
      },
    )
  }

  return request.post<ReviewActionResponse, { reason: string; reviewLevel: number }>(
    `/v1/reviewer/submissions/${reviewId}/reject`,
    {
      reason: payload.reason ?? payload.comment ?? '',
      reviewLevel: 1,
    },
  )
}

export function submitBatchManualReviewActionRequest(reviewIds: string[], payload: ManualReviewActionPayload): Promise<BatchReviewResponse> {
  if (payload.decision === 'approved') {
    return request.post<BatchReviewResponse, { submissionIds: number[]; reviewComment?: string; reviewLevel: number }>(
      '/v1/reviewer/submissions/batch/approve',
      {
        submissionIds: reviewIds.map(Number),
        reviewComment: payload.comment,
        reviewLevel: 1,
      },
    )
  }

  return request.post<BatchReviewResponse, { submissionIds: number[]; reason: string; reviewLevel: number }>(
    '/v1/reviewer/submissions/batch/reject',
    {
      submissionIds: reviewIds.map(Number),
      reason: payload.reason ?? payload.comment ?? '',
      reviewLevel: 1,
    },
  )
}
