import { Button, Card, Input, Modal, Select, Space, Statistic, Table, Tag, Typography } from 'antd'
import { EyeOutlined, FormOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useLabelingStore } from '../../stores/labelingStore'
import type { LabelingSubmission } from '../../types/labeling'

const statusLabels: Record<LabelingSubmission['status'], string> = {
  submitted: '已提交',
  approved: '已通过',
  rejected: '待修改',
}

const statusColors: Record<LabelingSubmission['status'], string> = {
  submitted: 'processing',
  approved: 'success',
  rejected: 'error',
}

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '已提交', value: 'submitted' },
  { label: '已通过', value: 'approved' },
  { label: '待修改', value: 'rejected' },
]

export function LabelerSubmissionsPage() {
  const navigate = useNavigate()
  const submissionStats = useLabelingStore((state) => state.submissionStats)
  const submissions = useLabelingStore((state) => state.submissions)
  const isSubmissionsLoading = useLabelingStore((state) => state.isSubmissionsLoading)
  const loadSubmissions = useLabelingStore((state) => state.loadSubmissions)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<LabelingSubmission['status'] | 'all'>('all')
  const [previewSubmission, setPreviewSubmission] = useState<LabelingSubmission | null>(null)

  useEffect(() => {
    void loadSubmissions()
  }, [loadSubmissions])

  const filteredSubmissions = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase()

    return submissions.filter((submission) => {
      const matchesStatus = status === 'all' || submission.status === status
      const matchesKeyword =
        normalizedKeyword.length === 0 ||
        submission.taskTitle.toLowerCase().includes(normalizedKeyword) ||
        submission.rejectReason?.toLowerCase().includes(normalizedKeyword) ||
        submission.reviewComment?.toLowerCase().includes(normalizedKeyword)

      return matchesStatus && matchesKeyword
    })
  }, [keyword, status, submissions])

  return (
    <main className="labeler-page">
      <ContentShell className="labeler-hero">
        <PageHeader
          title="我的数据"
          description="查看提交统计、审核结果和历史提交记录。待修改任务可直接回到工作台继续处理。"
          extra={
            <Button icon={<ReloadOutlined />} loading={isSubmissionsLoading} onClick={() => void loadSubmissions()}>
              刷新
            </Button>
          }
        />
      </ContentShell>

      <div className="labeler-stat-grid">
        <Card className="labeler-stat-card">
          <Statistic title="已提交" value={submissionStats?.submitted ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="已通过" value={submissionStats?.approved ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="已打回" value={submissionStats?.rejected ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="待修改" value={submissionStats?.needsRevision ?? 0} />
        </Card>
        <Card className="labeler-stat-card">
          <Statistic title="进行中" value={submissionStats?.inProgress ?? 0} />
        </Card>
      </div>

      <Card className="labeler-table-card">
        <div className="labeler-toolbar">
          <Input.Search
            allowClear
            className="labeler-toolbar__search"
            placeholder="搜索任务标题或审核意见"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={setKeyword}
          />
          <Select className="labeler-toolbar__select" options={statusOptions} value={status} onChange={setStatus} />
        </div>

        <Table<LabelingSubmission>
          columns={[
            {
              title: '任务',
              dataIndex: 'taskTitle',
              render: (_, submission) => (
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>{submission.taskTitle}</Typography.Text>
                  <Typography.Text type="secondary">提交时间：{submission.submittedAt}</Typography.Text>
                  {submission.rejectReason ? <Typography.Text type="danger">{submission.rejectReason}</Typography.Text> : null}
                </Space>
              ),
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 110,
              render: (value: LabelingSubmission['status']) => <Tag color={statusColors[value]}>{statusLabels[value]}</Tag>,
            },
            {
              title: '审核时间',
              dataIndex: 'reviewedAt',
              width: 150,
              render: (value?: string) => value ?? '-',
            },
            {
              title: '操作',
              width: 190,
              render: (_, submission) => (
                <Space wrap>
                  <Button icon={<EyeOutlined />} size="small" onClick={() => setPreviewSubmission(submission)}>
                    查看
                  </Button>
                  {submission.status === 'rejected' ? (
                    <Button
                      icon={<FormOutlined />}
                      size="small"
                      type="primary"
                      onClick={() => navigate(`/app/labeler/workbench/${submission.taskId}`)}
                    >
                      修改
                    </Button>
                  ) : null}
                </Space>
              ),
            },
          ]}
          dataSource={filteredSubmissions}
          loading={isSubmissionsLoading}
          pagination={false}
          rowKey="id"
        />
      </Card>

      <Modal
        footer={null}
        open={Boolean(previewSubmission)}
        title={previewSubmission?.taskTitle}
        width={720}
        onCancel={() => setPreviewSubmission(null)}
      >
        <Space direction="vertical" size={12}>
          <Typography.Text>提交时间：{previewSubmission?.submittedAt}</Typography.Text>
          <Typography.Text>审核结果：{previewSubmission ? statusLabels[previewSubmission.status] : '-'}</Typography.Text>
          {previewSubmission?.reviewComment ? <Typography.Paragraph>{previewSubmission.reviewComment}</Typography.Paragraph> : null}
          <pre className="labeler-history-card__content">{JSON.stringify(previewSubmission?.answers ?? [], null, 2)}</pre>
        </Space>
      </Modal>
    </main>
  )
}
