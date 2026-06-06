import type {
  BatchManualReviewResult,
  ManualReviewActionPayload,
  ReviewDetail,
  ReviewOutcomeSyncPayload,
} from '../../types/review'
import {
  claimReviewerSubmissions,
  getReviewDetail,
  getSubmissionAiReview,
  listAllAiReviewLogs,
  listClaimableReviewerSubmissions,
  listManualReviewQueue,
  listReviewHistory,
  listReviewerTaskItems,
  listReviewerTasks,
  listSubmissionVersions,
  listTaskAiReviewLogs,
  retrySubmissionAiReview,
  submitBatchManualReviewActionRequest,
  submitManualReviewActionRequest,
} from './reviewApi'
import {
  getAiReviewResult,
  listManualReviewRecords,
  processSubmissionWithAi,
} from './reviewLocalRuntime'
import { getNowLabel } from './reviewUtils'

type ReviewOutcomeSyncHandler = (payload: ReviewOutcomeSyncPayload) => void
type BatchReviewFailure = { submissionId?: number; reviewId?: string; reason: string }

let reviewOutcomeSyncHandler: ReviewOutcomeSyncHandler | null = null

function getBatchReviewFailures(response: {
  failures?: BatchReviewFailure[]
  results?: Array<{ submissionId: number; success: boolean; reason?: string }>
}): BatchReviewFailure[] {
  return (
    response.failures ??
    (response.results ?? [])
      .filter((item) => !item.success)
      .map((item) => ({
        submissionId: item.submissionId,
        reason: item.reason ?? '处理失败',
      }))
  )
}

export const reviewService = {
  registerReviewOutcomeSync(handler: ReviewOutcomeSyncHandler) {
    reviewOutcomeSyncHandler = handler
  },

  listManualReviewQueue,
  listReviewerTasks,
  listReviewerTaskItems,
  listClaimableReviewerSubmissions,
  claimReviewerSubmissions,
  listReviewHistory,
  getReviewDetail,
  getAiReviewResult,
  listAllAiReviewLogs,
  listTaskAiReviewLogs,
  getSubmissionAiReview,
  retrySubmissionAiReview,
  listManualReviewRecords,
  listSubmissionVersions,

  async submitManualReviewAction(reviewId: string, payload: ManualReviewActionPayload): Promise<ReviewDetail | null> {
    const response = await submitManualReviewActionRequest(reviewId, payload)
    const current = await getReviewDetail(String(response.submissionId))

    reviewOutcomeSyncHandler?.({
      submissionId: String(response.submissionId),
      status: payload.decision === 'approved' ? 'approved' : 'rejected',
      reviewedAt: getNowLabel(),
      reviewSource: 'manual',
      reviewStatus: payload.decision === 'approved' ? 'manual_approved' : 'manual_rejected',
      rejectReason: payload.decision === 'rejected' ? payload.reason ?? payload.comment : undefined,
      reviewComment: payload.comment ?? payload.reason,
    })

    return current
  },

  async submitBatchManualReviewAction(reviewIds: string[], payload: ManualReviewActionPayload): Promise<BatchManualReviewResult> {
    const response = await submitBatchManualReviewActionRequest(reviewIds, payload)
    const responseFailures = getBatchReviewFailures(response)
    const failedIds = new Set(responseFailures.map((item) => String(item.submissionId ?? item.reviewId)))
    const successfulIds = reviewIds.filter((reviewId) => !failedIds.has(reviewId)).slice(0, response.successCount)
    const success = (
      await Promise.all(successfulIds.map((reviewId) => getReviewDetail(reviewId).catch(() => null)))
    ).filter((detail): detail is ReviewDetail => Boolean(detail))

    return {
      success,
      failed: responseFailures.map((failure) => ({
        reviewId: String(failure.submissionId ?? failure.reviewId ?? ''),
        reason: failure.reason,
      })),
    }
  },

  processSubmissionWithAi,
}
