import { Alert, Button, Empty, Progress, Segmented, Space, Tag, Typography, message } from 'antd'
import { CheckCircleOutlined, ReloadOutlined, RightOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import type { MouseEvent } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useReviewStore } from '../../stores/reviewStore'
import type { ReviewerReviewTaskClaimScope, ReviewerTaskSummary } from '../../types/review'
import styles from './ReviewerPages.module.css'

const claimScopeOptions: Array<{ label: string; value: ReviewerReviewTaskClaimScope }> = [
  { label: 'All', value: 'ALL' },
  { label: 'Mine', value: 'MINE' },
  { label: 'Claimed', value: 'CLAIMED' },
  { label: 'Unclaimed', value: 'UNCLAIMED' },
]

const emptyDescriptionMap: Record<ReviewerReviewTaskClaimScope, string> = {
  ALL: 'No review tasks',
  MINE: 'No tasks claimed by you',
  CLAIMED: 'No tasks claimed by others',
  UNCLAIMED: 'No unclaimed tasks',
}

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

function getTaskViewState(task: ReviewerTaskSummary) {
  if (task.claimedByMe) {
    return 'mine'
  }

  if (task.claimed) {
    return 'claimed'
  }

  return 'unclaimed'
}

function getTaskStatusLabel(task: ReviewerTaskSummary) {
  const state = getTaskViewState(task)

  if (state === 'mine') {
    return 'Mine'
  }

  if (state === 'claimed') {
    return 'Claimed by others'
  }

  return task.claimable === false ? 'Unavailable' : 'Open'
}

function getTaskStatusColor(task: ReviewerTaskSummary) {
  const state = getTaskViewState(task)

  if (state === 'mine') {
    return 'success'
  }

  if (state === 'claimed') {
    return 'default'
  }

  return task.claimable === false ? 'default' : 'blue'
}

function canOpenTask(task: ReviewerTaskSummary) {
  return task.claimedByMe
}

function canClaimTask(task: ReviewerTaskSummary) {
  return !task.claimed && task.claimable !== false && task.pendingCount > 0
}

function getTaskCardClassName(task: ReviewerTaskSummary) {
  const state = getTaskViewState(task)
  const classNames = [styles.taskCard]

  if (state === 'mine') {
    classNames.push(styles.taskCardMine)
  } else if (state === 'claimed') {
    classNames.push(styles.taskCardClaimed)
  } else {
    classNames.push(styles.taskCardUnclaimed)
  }

  if (!canOpenTask(task)) {
    classNames.push(styles.taskCardLocked)
  }

  return classNames.join(' ')
}

export function ReviewerClaimPage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const [claimScope, setClaimScope] = useState<ReviewerReviewTaskClaimScope>('ALL')
  const [claimingTaskId, setClaimingTaskId] = useState<string | null>(null)
  const reviewerTasks = useReviewStore((state) => state.reviewerTasks)
  const latestClaimResult = useReviewStore((state) => state.latestClaimResult)
  const error = useReviewStore((state) => state.error)
  const isReviewerTasksLoading = useReviewStore((state) => state.isReviewerTasksLoading)
  const isClaimingSubmissions = useReviewStore((state) => state.isClaimingSubmissions)
  const loadReviewerTasks = useReviewStore((state) => state.loadReviewerTasks)
  const claimReviewerSubmissions = useReviewStore((state) => state.claimReviewerSubmissions)

  useEffect(() => {
    void loadReviewerTasks(claimScope)
  }, [claimScope, loadReviewerTasks])

  const claimableTasks = useMemo(
    () =>
      reviewerTasks
        .sort((first, second) => second.pendingCount - first.pendingCount || first.taskId - second.taskId),
    [reviewerTasks],
  )

  const openTask = (task: ReviewerTaskSummary) => {
    if (!canOpenTask(task)) {
      return
    }

    navigate(`/app/reviewer/tasks/${task.taskId}`)
  }

  const claimTask = async (event: MouseEvent<HTMLElement>, task: ReviewerTaskSummary) => {
    event.stopPropagation()
    const taskId = String(task.taskId)
    setClaimingTaskId(taskId)

    try {
      const result = await claimReviewerSubmissions(taskId, claimScope)

      if (result) {
        messageApi.success(`Claimed ${result.claimedCount} submissions`)
      } else {
        messageApi.error('Claim failed')
      }
    } finally {
      setClaimingTaskId(null)
    }
  }

  return (
    <main className={styles.page}>
      {contextHolder}
      <ContentShell>
        <PageHeader
          title="领取审核任务"
          description="按任务提交领取相关题目，再在任务详情页完成题目审核。"
          extra={
            <Button icon={<ReloadOutlined />} loading={isReviewerTasksLoading} onClick={() => void loadReviewerTasks(claimScope)}>
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

      <div className={styles.taskScopeBar}>
        <Segmented
          className={styles.taskScopeTabs}
          options={claimScopeOptions}
          value={claimScope}
          onChange={(value) => setClaimScope(value as ReviewerReviewTaskClaimScope)}
        />
      </div>

      <section className={styles.taskBoard}>
        {claimableTasks.length > 0 ? (
          claimableTasks.map((task) => {
            const percent = getWorkloadPercent(task)
            const taskCanOpen = canOpenTask(task)
            const taskCanClaim = canClaimTask(task)
            const taskId = String(task.taskId)
            const isCurrentTaskClaiming = isClaimingSubmissions && claimingTaskId === taskId

            return (
              <article
                key={task.taskId}
                aria-disabled={!taskCanOpen}
                className={getTaskCardClassName(task)}
                onClick={taskCanOpen ? () => openTask(task) : undefined}
              >
                <div className={styles.taskCardTopline} />
                <Space direction="vertical" size={14} className={styles.taskCardContent}>
                  <div className={styles.taskCardHeader}>
                    <div>
                      <Typography.Text className={styles.taskEyebrow}>TASK #{task.taskId}</Typography.Text>
                      <Typography.Title level={4}>{getTaskTitle(task)}</Typography.Title>
                    </div>
                    <Tag color={getTaskStatusColor(task)}>{getTaskStatusLabel(task)}</Tag>
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

                  <Progress percent={percent} size="small" strokeColor="#0075de" trailColor="#f6f5f4" />

                  <Space wrap className={styles.taskCardActions}>
                    <Button
                      disabled={!taskCanClaim || (isClaimingSubmissions && !isCurrentTaskClaiming)}
                      icon={<CheckCircleOutlined />}
                      loading={isCurrentTaskClaiming}
                      type="primary"
                      onClick={(event) => void claimTask(event, task)}
                      style={{ color: 'white' }}
                    >
                      Claim
                    </Button>
                    <Button disabled={!taskCanOpen} icon={<RightOutlined />} onClick={() => openTask(task)}>
                      View Items
                    </Button>
                  </Space>
                </Space>
              </article>
            )
          })
        ) : (
          <div className={styles.emptyStage}>
            <Empty description={isReviewerTasksLoading ? 'Loading tasks...' : emptyDescriptionMap[claimScope]} />
          </div>
        )}
      </section>
    </main>
  )
}
