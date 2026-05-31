import { useDraggable } from '@dnd-kit/core'
import { CSS } from '@dnd-kit/utilities'
import { Space, Typography } from 'antd'
import type { DynamicFieldType } from '../../../../types/dynamicForm'
import { dynamicMaterialGroups, dynamicMaterialRegistry, paletteMaterialTypes } from '../../materialRegistry'

function MaterialCard({ type }: { type: DynamicFieldType }) {
  const definition = dynamicMaterialRegistry[type]
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
      <Typography.Text strong>{definition.title}</Typography.Text>
      <Typography.Text type="secondary">{definition.description}</Typography.Text>
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
            <Space direction="vertical" size={8}>
              {materials.map((type) => (
                <MaterialCard key={type} type={type} />
              ))}
            </Space>
          </section>
        )
      })}
    </>
  )
}
