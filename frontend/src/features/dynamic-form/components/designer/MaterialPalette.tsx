import {
  AlignLeftOutlined,
  ApartmentOutlined,
  AppstoreOutlined,
  CheckCircleOutlined,
  CheckSquareOutlined,
  CodeOutlined,
  EyeOutlined,
  FileTextOutlined,
  FontSizeOutlined,
  RobotOutlined,
  TagsOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import { useDraggable } from '@dnd-kit/core'
import { CSS } from '@dnd-kit/utilities'
import { Typography } from 'antd'
import type { ReactNode } from 'react'
import type { DynamicFieldType } from '../../../../types/dynamicForm'
import { dynamicMaterialGroups, dynamicMaterialRegistry, paletteMaterialTypes } from '../../materialRegistry'

const materialMeta: Partial<Record<DynamicFieldType, { icon: ReactNode; summary: string }>> = {
  input: { icon: <FontSizeOutlined />, summary: '短文本' },
  textarea: { icon: <AlignLeftOutlined />, summary: '长文本' },
  radio: { icon: <CheckCircleOutlined />, summary: '单选项' },
  checkbox: { icon: <CheckSquareOutlined />, summary: '多选项' },
  select: { icon: <TagsOutlined />, summary: '下拉选择' },
  tagSelect: { icon: <TagsOutlined />, summary: '标签输入' },
  showItem: { icon: <EyeOutlined />, summary: '只读展示' },
  richText: { icon: <FileTextOutlined />, summary: '富文本' },
  fileUpload: { icon: <UploadOutlined />, summary: '上传附件' },
  jsonEditor: { icon: <CodeOutlined />, summary: 'JSON 编辑' },
  llmPrompt: { icon: <RobotOutlined />, summary: 'AI 占位' },
  group: { icon: <AppstoreOutlined />, summary: '字段分组' },
  tabs: { icon: <ApartmentOutlined />, summary: '分页容器' },
}

function MaterialCard({ type }: { type: DynamicFieldType }) {
  const definition = dynamicMaterialRegistry[type]
  const meta = materialMeta[type] ?? { icon: <AppstoreOutlined />, summary: definition.description }
  const { attributes, listeners, setNodeRef, transform } = useDraggable({
    id: `material:${type}`,
    data: {
      type: 'material',
      fieldType: type,
    },
  })

  return (
    <button
      ref={setNodeRef}
      className="designer-material"
      style={{ transform: CSS.Translate.toString(transform) }}
      type="button"
      {...listeners}
      {...attributes}
    >
      <span className="designer-material__icon" aria-hidden="true">
        {meta.icon}
      </span>
      <span className="designer-material__content">
        <Typography.Text className="designer-material__title" ellipsis strong>
          {definition.title}
        </Typography.Text>
        <Typography.Text className="designer-material__summary" ellipsis type="secondary">
          {meta.summary}
        </Typography.Text>
      </span>
    </button>
  )
}

export function MaterialPalette() {
  return (
    <>
      {dynamicMaterialGroups.map((group) => {
        const materials = paletteMaterialTypes.filter((type) => dynamicMaterialRegistry[type].group === group.key)

        return (
          <section className="designer-material-group" key={group.key}>
            <Typography.Text className="designer-material-group__title">{group.title}</Typography.Text>
            <div className="designer-material-list">
              {materials.map((type) => (
                <MaterialCard key={type} type={type} />
              ))}
            </div>
          </section>
        )
      })}
    </>
  )
}
