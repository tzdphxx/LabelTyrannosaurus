import { Alert, Button, Empty, Progress, Space, Tag, Typography } from 'antd'
import { ReloadOutlined, RightOutlined } from '@ant-design/icons'
import { useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useReviewStore } from '../../stores/reviewStore'
import type { ReviewerTaskSummary } from '../../types/review'
import styles from './ReviewerPages.module.css'

function getTaskTitle(task: ReviewerTaskSummary) {
  return task.taskTitle?.trim() || `任务 ${task.taskId}`
}

function getWorkloadPercent(task: ReviewerTaskSummary) {
  const total = task.myPendingCount + task.totalReviewedCount

  if (total <= 0) {
    return 0
  }

  return Math.round((task.totalReviewedCount / total) * 100)
}

export function ReviewerQueuePage() {
  const navigate = useNavigate()
  const reviewerTasks = useReviewStore((state) => state.reviewerTasks)
  const error = useReviewStore((state) => state.error)
  const isReviewerTasksLoading = useReviewStore((state) => state.isReviewerTasksLoading)
  const loadReviewerTasks = useReviewStore((state) => state.loadReviewerTasks)

  useEffect(() => {
    void loadReviewerTasks('MINE')
  }, [loadReviewerTasks])

  const myTasks = useMemo(
    () =>
      reviewerTasks
        .filter((task) => task.claimedByMe || task.myPendingCount > 0)
        .sort((first, second) => second.myPendingCount - first.myPendingCount || first.taskId - second.taskId),
    [reviewerTasks],
  )

  const openTask = (task: ReviewerTaskSummary) => {
    navigate(`/app/reviewer/tasks/${task.taskId}`)
  }

  return (
    <main className={styles.page}>
      <ContentShell>
        <PageHeader
          title="我的审核队列"
          description="点击任务进入题目复核工作台。"
          extra={
            <Button icon={<ReloadOutlined />} loading={isReviewerTasksLoading} onClick={() => void loadReviewerTasks('MINE')}>
              刷新任务
            </Button>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <section className={styles.taskBoard}>
        {myTasks.length > 0 ? (
          myTasks.map((task) => {
            const percent = getWorkloadPercent(task)

            return (
              <article key={task.taskId} className={`${styles.taskCard} ${styles.taskCardQueue}`} onClick={() => openTask(task)}>
                <div className={styles.taskCardTopline} />
                <Space direction="vertical" size={14} className={styles.taskCardContent}>
                  <div className={styles.taskCardHeader}>
                    <div>
                      <Typography.Text className={styles.taskEyebrow}>REVIEW #{task.taskId}</Typography.Text>
                      <Typography.Title level={4}>{getTaskTitle(task)}</Typography.Title>
                    </div>
                    <Tag color="geekblue">我的任务</Tag>
                  </div>

                  <div className={styles.taskMetricGrid}>
                    <div className={`${styles.taskMetric} ${styles.taskMetricHot}`}>
                      <span>{task.myPendingCount}</span>
                      <small>我的待审</small>
                    </div>
                    <div className={styles.taskMetric}>
                      <span>{task.pendingCount}</span>
                      <small>待领取</small>
                    </div>
                    <div className={styles.taskMetric}>
                      <span>{task.totalReviewedCount}</span>
                      <small>已审核</small>
                    </div>
                  </div>

                  <Progress percent={percent} size="small" strokeColor="#0075de" trailColor="#f6f5f4" />

                  <Button icon={<RightOutlined />} type="primary" onClick={() => openTask(task)}>
                    查看题目
                  </Button>
                </Space>
              </article>
            )
          })
        ) : (
          <div className={styles.emptyStage}>
            <Empty description={isReviewerTasksLoading ? '正在加载任务...' : '暂无已领取任务'} />
          </div>
        )}
      </section>
    </main>
  )
}
