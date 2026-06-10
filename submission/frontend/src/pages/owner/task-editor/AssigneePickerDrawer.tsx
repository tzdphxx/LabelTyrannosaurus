import { Button, Drawer, Empty, Input, List, Pagination, Space, Typography, message } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { ownerTaskService } from '../../../services'
import type { OwnerLabelerOption, OwnerLabelerPageResponse } from '../../../types/task'
import styles from '../OwnerTaskEditorPage.module.css'

interface AssigneePickerDrawerProps {
  open: boolean
  selectedLabelerId?: string | null
  onClose: () => void
  onSelect: (labeler: OwnerLabelerOption) => void
}

const DEFAULT_LABELER_PAGE_SIZE = 8

function formatReward(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: 2,
  }).format(value)
}

function formatRate(value: number) {
  return `${Math.round(value * 100)}%`
}

export function AssigneePickerDrawer({ open, selectedLabelerId, onClose, onSelect }: AssigneePickerDrawerProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const [keyword, setKeyword] = useState('')
  const [queryKeyword, setQueryKeyword] = useState('')
  const [page, setPage] = useState(1)
  const [labelerPage, setLabelerPage] = useState<OwnerLabelerPageResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const loadLabelers = useCallback(async () => {
    setIsLoading(true)

    try {
      setLabelerPage(await ownerTaskService.listAssignableLabelers({
        keyword: queryKeyword,
        page,
        size: DEFAULT_LABELER_PAGE_SIZE,
      }))
    } catch {
      messageApi.error('标注员列表加载失败')
      setLabelerPage(null)
    } finally {
      setIsLoading(false)
    }
  }, [messageApi, page, queryKeyword])

  useEffect(() => {
    if (open) {
      void loadLabelers()
    }
  }, [loadLabelers, open])

  const labelers = useMemo(() => labelerPage?.items ?? [], [labelerPage?.items])

  const submitSearch = () => {
    setPage(1)
    setQueryKeyword(keyword.trim())
  }

  return (
    <Drawer
      className={styles.previewDrawer}
      destroyOnHidden
      open={open}
      placement="right"
      title="选择标注员"
      width="min(560px, 100vw)"
      onClose={onClose}
    >
      {contextHolder}
      <div className={styles.assigneeDrawerBody}>
        <Space.Compact className={styles.assigneeSearch}>
          <Input
            allowClear
            placeholder="按用户名模糊搜索"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onPressEnter={submitSearch}
          />
          <Button icon={<SearchOutlined />} type="primary" onClick={submitSearch}>
            搜索
          </Button>
        </Space.Compact>

        <List
          className={styles.assigneeList}
          dataSource={labelers}
          loading={isLoading}
          locale={{ emptyText: <Empty description="暂无可选标注员" /> }}
          renderItem={(labeler) => {
            const isSelected = selectedLabelerId === String(labeler.labelerId)

            return (
              <List.Item
                className={`${styles.assigneeItem} ${isSelected ? styles.assigneeItemSelected : ''}`}
                onClick={() => onSelect(labeler)}
              >
                <div className={styles.assigneeNameBlock}>
                  <Typography.Text strong>{labeler.username}</Typography.Text>
                </div>
                <div className={styles.assigneeMetrics}>
                  <span>
                    <Typography.Text type="secondary">累计奖励</Typography.Text>
                    <Typography.Text strong>{formatReward(labeler.totalReward)}</Typography.Text>
                  </span>
                  <span>
                    <Typography.Text type="secondary">通过率</Typography.Text>
                    <Typography.Text strong>{formatRate(labeler.approvalRate)}</Typography.Text>
                  </span>
                </div>
              </List.Item>
            )
          }}
        />

        <Pagination
          className={styles.assigneePagination}
          current={page}
          pageSize={labelerPage?.pageSize ?? DEFAULT_LABELER_PAGE_SIZE}
          showSizeChanger={false}
          total={labelerPage?.total ?? 0}
          onChange={(nextPage) => setPage(nextPage)}
        />
      </div>
    </Drawer>
  )
}
