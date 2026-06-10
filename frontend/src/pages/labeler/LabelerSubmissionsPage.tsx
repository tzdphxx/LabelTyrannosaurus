import { Alert, Button, Card, Input, Modal, Select, Space, Statistic, Table, Tag, Typography } from 'antd'
import { EyeOutlined, FormOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useLabelingStore } from '../../stores/labelingStore'
import type { LabelerAssignmentSummary } from '../../types/labeling'

type AssignmentStatusFilter = Extract<LabelerAssignmentSummary['status'], 'CLAIMED' | 'PAUSED' | 'ENDED'>

const statusLabels: Record<LabelerAssignmentSummary['status'], string> = {
  CLAIMED: '已领取',
  DRAFTING: '草稿中',
  PAUSED: '已暂停',
  ENDED: '已结束',
  SUBMITTED: '已提交',
  AI_RETURNED: 'AI 退回',
  RETURNED: '待修改',
  APPROVED: '已通过',
  CANCELLED: '已取消',
}

const statusColors: Record<LabelerAssignmentSummary['status'], string> = {
  CLAIMED: 'warning',
  DRAFTING: 'processing',
  PAUSED: 'default',
  ENDED: 'default',
  SUBMITTED: 'geekblue',
  AI_RETURNED: 'error',
  RETURNED: 'error',
  APPROVED: 'success',
  CANCELLED: 'default',
}

const statusOptions: Array<{ label: string; value: AssignmentStatusFilter }> = [
  { label: '已领取', value: 'CLAIMED' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已结束', value: 'ENDED' },
]

function canOpenWorkbench(status: LabelerAssignmentSummary['status']) {
  return status !== 'CANCELLED' && status !== 'PAUSED' && status !== 'ENDED'
}

function canViewAssignment(status: LabelerAssignmentSummary['status']) {
  return status !== 'PAUSED'
}

export function LabelerSubmissionsPage() {
  const navigate = useNavigate()
  const assignmentStats = useLabelingStore((state) => state.assignmentStats)
  const assignments = useLabelingStore((state) => state.assignments)
  const error = useLabelingStore((state) => state.error)
  const isAssignmentsLoading = useLabelingStore((state) => state.isAssignmentsLoading)
  const loadAssignments = useLabelingStore((state) => state.loadAssignments)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<AssignmentStatusFilter | undefined>()
  const [previewAssignment, setPreviewAssignment] = useState<LabelerAssignmentSummary | null>(null)

  useEffect(() => {
    void loadAssignments({ page: 1, size: 100 })
  }, [loadAssignments])

  const filteredAssignments = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase()

    return assignments.filter((assignment) => {
      if (normalizedKeyword.length === 0) {
        return !status || assignment.status === status
      }

      const matchesKeyword = (
        assignment.taskTitle.toLowerCase().includes(normalizedKeyword) ||
        assignment.taskId.includes(normalizedKeyword)
      )

      return matchesKeyword && (!status || assignment.status === status)
    })
  }, [assignments, keyword, status])

  return (
    <main className="labeler-page">
      <ContentShell className="labeler-hero">
        <PageHeader
          title="我的领取"
          description="查看当前账号已领取的任务，继续未完成草稿或处理被退回的题目。"
          extra={
            <Button icon={<ReloadOutlined />} loading={isAssignmentsLoading} onClick={() => void loadAssignments({ page: 1, size: 100 })}>
              刷新
            </Button>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <div className="labeler-stat-grid">
        <Card className="labeler-stat-card">
          <Statistic title="全部领取" value={assignmentStats?.total ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="草稿中" value={assignmentStats?.drafting ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="已提交" value={assignmentStats?.submitted ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="待修改" value={assignmentStats?.returned ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="已通过" value={assignmentStats?.approved ?? 0} />
        </Card>
      </div>

      <Card className="labeler-table-card">
        <div className="labeler-toolbar">
          <Input.Search
            allowClear
            className="labeler-toolbar__search"
            placeholder="搜索任务标题或任务 ID"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={setKeyword}
          />
          <Select
            allowClear
            className="labeler-toolbar__select"
            options={statusOptions}
            placeholder="状态筛选"
            value={status}
            onChange={setStatus}
          />
        </div>

        <Table<LabelerAssignmentSummary>
          columns={[
            {
              title: '任务',
              dataIndex: 'taskTitle',
              render: (_, assignment) => (
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>{assignment.taskTitle}</Typography.Text>
                  <Typography.Text type="secondary">
                    任务 #{assignment.taskId} / 已领取 {assignment.myClaimedCount ?? 0} 题
                  </Typography.Text>
                </Space>
              ),
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 110,
              render: (value: LabelerAssignmentSummary['status']) => <Tag color={statusColors[value]}>{statusLabels[value]}</Tag>,
            },
            {
              title: '提交/通过',
              width: 120,
              render: (_, assignment) => `${assignment.mySubmittedCount ?? 0} / ${assignment.myApprovedCount ?? 0}`,
            },
            {
              title: '更新时间',
              dataIndex: 'updatedAt',
              width: 150,
            },
            {
              title: '操作',
              width: 190,
              render: (_, assignment) => (
                <Space wrap>
                  <Button
                    disabled={!canViewAssignment(assignment.status)}
                    icon={<EyeOutlined />}
                    size="small"
                    onClick={() => setPreviewAssignment(assignment)}
                  >
                    查看
                  </Button>
                  <Button
                    disabled={!canOpenWorkbench(assignment.status)}
                    icon={<FormOutlined />}
                    size="small"
                    type={assignment.status === 'DRAFTING' || assignment.status === 'RETURNED' ? 'primary' : 'default'}
                    onClick={() => navigate(`/app/labeler/workbench/${assignment.taskId}`)}
                  >
                    进入工作台
                  </Button>
                </Space>
              ),
            },
          ]}
          dataSource={filteredAssignments}
          loading={isAssignmentsLoading}
          pagination={false}
          rowKey="taskId"
        />
      </Card>

      <Modal
        footer={null}
        open={Boolean(previewAssignment)}
        title={previewAssignment?.taskTitle}
        width={640}
        onCancel={() => setPreviewAssignment(null)}
      >
        <Space direction="vertical" size={10}>
          <Typography.Text>任务 ID：{previewAssignment?.taskId}</Typography.Text>
          <Typography.Text>已领取题目数：{previewAssignment?.myClaimedCount ?? 0}</Typography.Text>
          <Typography.Text>已提交题目数：{previewAssignment?.mySubmittedCount ?? 0}</Typography.Text>
          <Typography.Text>已通过题目数：{previewAssignment?.myApprovedCount ?? 0}</Typography.Text>
          <Typography.Text>状态：{previewAssignment ? statusLabels[previewAssignment.status] : '-'}</Typography.Text>
          <Typography.Text>最近领取：{previewAssignment?.claimedAt ?? '-'}</Typography.Text>
          <Typography.Text>更新时间：{previewAssignment?.updatedAt ?? '-'}</Typography.Text>
        </Space>
      </Modal>
    </main>
  )
}
