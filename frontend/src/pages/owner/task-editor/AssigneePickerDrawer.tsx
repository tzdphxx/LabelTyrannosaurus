import { Drawer, Empty, Typography } from 'antd'
import styles from '../OwnerTaskEditorPage.module.css'

interface AssigneePickerDrawerProps {
  open: boolean
  onClose: () => void
}

export function AssigneePickerDrawer({ open, onClose }: AssigneePickerDrawerProps) {
  return (
    <Drawer
      className={styles.previewDrawer}
      destroyOnHidden
      open={open}
      placement="right"
      title="选择标注员"
      width="min(520px, 100vw)"
      onClose={onClose}
    >
      <div className={styles.assigneeDrawerBody}>
        <Empty description="标注员列表接口待接入" />
        <Typography.Text type="secondary">
          当前只保留指派策略下的动态展示和侧边栏入口，暂不请求接口，也不写入创建任务请求体。
        </Typography.Text>
      </div>
    </Drawer>
  )
}
