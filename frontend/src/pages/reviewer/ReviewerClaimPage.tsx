import { Alert, Button, Empty, Progress, Space, Tag, Typography, message } from 'antd'
import { CheckCircleOutlined, ReloadOutlined, RightOutlined } from '@ant-design/icons'
import { useEffect, useMemo } from 'react'
import type { MouseEvent } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useReviewStore } from '../../stores/reviewStore'
import type { ReviewerTaskSummary } from '../../types/review'
import styles from './ReviewerPages.module.css'

function getTaskTitle(task: ReviewerTaskSummary) {
  return task.taskTitle?.trim() || `Task ${task.taskId}`
}

function getWorkloadPercent(task: ReviewerTaskSummary) {
  const total = task.pendingCount + task.totalReviewedCount

  if (total <= 0) {
    return 0
  }

  return Math.round((task.totalReviewedCount / total) * 100)
}

export function ReviewerClaimPage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const reviewerTasks = useReviewStore((state) => state.reviewerTasks)
  const latestClaimResult = useReviewStore((state) => state.latestClaimResult)
  const error = useReviewStore((state) => state.error)
  const isReviewerTasksLoading = useReviewStore((state) => state.isReviewerTasksLoading)
  const isClaimingSubmissions = useReviewStore((state) => state.isClaimingSubmissions)
  const loadReviewerTasks = useReviewStore((state) => state.loadReviewerTasks)
  const claimReviewerSubmissions = useReviewStore((state) => state.claimReviewerSubmissions)

  useEffect(() => {
    void loadReviewerTasks()
  }, [loadReviewerTasks])

  const claimableTasks = useMemo(
    () =>
      reviewerTasks
        .filter((task) => task.pendingCount > 0 || task.claimed)
        .sort((first, second) => second.pendingCount - first.pendingCount || first.taskId - second.taskId),
    [reviewerTasks],
  )

  const openTask = (task: ReviewerTaskSummary) => {
    navigate(`/app/reviewer/tasks/${task.taskId}`)
  }

  const claimTask = async (event: MouseEvent<HTMLElement>, task: ReviewerTaskSummary) => {
    event.stopPropagation()
    const result = await claimReviewerSubmissions(String(task.taskId))

    if (result) {
      messageApi.success(`Claimed ${result.claimedCount} submissions`)
    } else {
      messageApi.error('Claim failed')
    }
  }

  return (
    <main className={styles.page}>
      {contextHolder}
      <ContentShell>
        <PageHeader
          title="Claim Review Tasks"
          description="Claim submitted work by task, then review items in the task detail page."
          extra={
            <Button icon={<ReloadOutlined />} loading={isReviewerTasksLoading} onClick={() => void loadReviewerTasks()}>
              Refresh
            </Button>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      {latestClaimResult ? (
        <Alert
          className={styles.claimResult}
          message={`Latest claim: task ${latestClaimResult.taskId ?? '-'}, ${latestClaimResult.claimedCount} submissions`}
          showIcon
          type="success"
        />
      ) : null}

      <section className={styles.taskBoard}>
        {claimableTasks.length > 0 ? (
          claimableTasks.map((task) => {
            const percent = getWorkloadPercent(task)

            return (
              <article key={task.taskId} className={styles.taskCard} onClick={() => openTask(task)}>
                <div className={styles.taskCardTopline} />
                <Space direction="vertical" size={14} className={styles.taskCardContent}>
                  <div className={styles.taskCardHeader}>
                    <div>
                      <Typography.Text className={styles.taskEyebrow}>TASK #{task.taskId}</Typography.Text>
                      <Typography.Title level={4}>{getTaskTitle(task)}</Typography.Title>
                    </div>
                    <Tag color={task.claimedByMe ? 'success' : task.claimed ? 'processing' : 'warning'}>
                      {task.claimedByMe ? 'Mine' : task.claimed ? 'Claimed' : 'Open'}
                    </Tag>
                  </div>

                  <div className={styles.taskMetricGrid}>
                    <div className={`${styles.taskMetric} ${styles.taskMetricHot}`}>
                      <span>{task.pendingCount}</span>
                      <small>Open</small>
                    </div>
                    <div className={styles.taskMetric}>
                      <span>{task.myPendingCount}</span>
                      <small>Mine</small>
                    </div>
                    <div className={styles.taskMetric}>
                      <span>{task.totalReviewedCount}</span>
                      <small>Reviewed</small>
                    </div>
                  </div>

                  <Progress percent={percent} size="small" strokeColor="#22c55e" trailColor="#ecfdf5" />

                  <Space wrap className={styles.taskCardActions}>
                    <Button
                      disabled={task.pendingCount <= 0}
                      icon={<CheckCircleOutlined />}
                      loading={isClaimingSubmissions}
                      type="primary"
                      onClick={(event) => void claimTask(event, task)}
                    >
                      Claim
                    </Button>
                    <Button icon={<RightOutlined />} onClick={() => openTask(task)}>
                      View Items
                    </Button>
                  </Space>
                </Space>
              </article>
            )
          })
        ) : (
          <div className={styles.emptyStage}>
            <Empty description={isReviewerTasksLoading ? 'Loading tasks...' : 'No claimable tasks'} />
          </div>
        )}
      </section>
    </main>
  )
}
