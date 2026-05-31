import { DndContext, DragOverlay, type DragEndEvent, type DragStartEvent, PointerSensor, useSensor, useSensors } from '@dnd-kit/core'
import { SaveOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Modal, Space, Tabs, Tag, message } from 'antd'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../../components/page/ContentShell'
import { PageHeader } from '../../../components/page/PageHeader'
import {
  DesignerCanvas,
  DragPreview,
  MaterialPalette,
  PropertyPanel,
  type ActiveDragState,
} from '../../../features/dynamic-form/components/designer'
import { DynamicFormRenderer } from '../../../features/dynamic-form/components/DynamicFormRenderer'
import { canAcceptChild } from '../../../features/dynamic-form/materialRegistry'
import { createActiveDragState, type DesignerDragData } from '../../../features/dynamic-form/utils/designerDrag'
import { scrollNodeIntoCanvasView } from '../../../features/dynamic-form/utils/designerScroll'
import { findSchemaNode, getSchemaNodeKeys } from '../../../features/dynamic-form/utils/schemaTree'
import { useTemplateDesignerStore } from '../../../stores/templateDesignerStore'
import type { DynamicFormSubmitResult } from '../../../types/dynamicForm'

export function OwnerTemplateDesignerPage() {
  const navigate = useNavigate()
  const { templateId } = useParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [previewResult, setPreviewResult] = useState<DynamicFormSubmitResult | null>(null)
  const [activeDrag, setActiveDrag] = useState<ActiveDragState | null>(null)
  const canvasScrollRef = useRef<HTMLDivElement | null>(null)
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }))
  const template = useTemplateDesignerStore((state) => state.template)
  const schema = useTemplateDesignerStore((state) => state.schema)
  const selectedNodeId = useTemplateDesignerStore((state) => state.selectedNodeId)
  const isLoading = useTemplateDesignerStore((state) => state.isLoading)
  const isSaving = useTemplateDesignerStore((state) => state.isSaving)
  const hasUnsavedChanges = useTemplateDesignerStore((state) => state.hasUnsavedChanges)
  const error = useTemplateDesignerStore((state) => state.error)
  const loadTemplate = useTemplateDesignerStore((state) => state.loadTemplate)
  const addNode = useTemplateDesignerStore((state) => state.addNode)
  const selectNode = useTemplateDesignerStore((state) => state.selectNode)
  const updateSelectedNode = useTemplateDesignerStore((state) => state.updateSelectedNode)
  const deleteSelectedNode = useTemplateDesignerStore((state) => state.deleteSelectedNode)
  const deleteNode = useTemplateDesignerStore((state) => state.deleteNode)
  const reorderNodes = useTemplateDesignerStore((state) => state.reorderNodes)
  const saveSchema = useTemplateDesignerStore((state) => state.saveSchema)

  useEffect(() => {
    if (templateId) {
      void loadTemplate(templateId)
    }
  }, [loadTemplate, templateId])

  const selectedNode = useMemo(() => {
    if (!schema || !selectedNodeId) {
      return null
    }

    return findSchemaNode(schema, selectedNodeId)
  }, [schema, selectedNodeId])

  const fieldKeys = useMemo(() => {
    if (!schema) {
      return []
    }

    return getSchemaNodeKeys(schema).filter((key) => key !== selectedNode?.key)
  }, [schema, selectedNode])

  async function saveCurrentSchema() {
    const saved = await saveSchema()

    if (saved) {
      messageApi.success('模板 schema 已保存')
    } else {
      messageApi.error('模板 schema 保存失败')
    }
  }

  function confirmDeleteNode(nodeId: string) {
    Modal.confirm({
      title: '删除字段',
      content: '删除后该字段及其子字段会从当前 schema 中移除。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => {
        if (nodeId === selectedNodeId) {
          deleteSelectedNode()
          return
        }

        deleteNode(nodeId)
      },
    })
  }

  function deleteCurrentNode() {
    if (selectedNodeId) {
      confirmDeleteNode(selectedNodeId)
    }
  }

  function handleDragStart(event: DragStartEvent) {
    setActiveDrag(createActiveDragState(schema, event.active.data.current as DesignerDragData | undefined))
  }

  function handleDragEnd(event: DragEndEvent) {
    setActiveDrag(null)

    if (!schema || !event.over) {
      return
    }

    const activeData = event.active.data.current as
      | DesignerDragData
      | undefined
    const overData = event.over.data.current as
      | { type: 'container'; parentId: string | null }
      | { type: 'node'; nodeId: string; parentId: string | null }
      | undefined

    if (!activeData || !overData) {
      return
    }

    if (activeData.type === 'material') {
      const overNode = overData.type === 'node' ? findSchemaNode(schema, overData.nodeId) : null
      const parentId = overNode && canAcceptChild(overNode.type, activeData.fieldType) ? overNode.id : overData.parentId
      const parentType = parentId ? findSchemaNode(schema, parentId)?.type ?? null : null

      if (!canAcceptChild(parentType, activeData.fieldType)) {
        messageApi.warning('该物料不能放入当前区域')
        return
      }

      const insertedNodeId = addNode(activeData.fieldType, parentId)

      if (insertedNodeId) {
        scrollNodeIntoCanvasView(canvasScrollRef.current, insertedNodeId)
      }

      return
    }

    if (activeData.type === 'node' && overData.type === 'node') {
      reorderNodes(activeData.nodeId, overData.nodeId)
    }
  }

  return (
    <main className="owner-page">
      {contextHolder}
      <ContentShell>
        <PageHeader
          title={template?.name ?? '模板 Designer'}
          description="使用 dnd-kit 搭建模板 schema，并通过 Formily Renderer 实时预览运行态表单。"
          extra={
            <>
              {hasUnsavedChanges ? <Tag color="warning">有未保存变更</Tag> : <Tag color="success">已同步</Tag>}
              <Button onClick={() => navigate('/app/owner/templates')}>返回模板列表</Button>
              <Button icon={<SaveOutlined />} loading={isSaving} onClick={() => void saveCurrentSchema()} type="primary">
                保存 schema
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <DndContext
        autoScroll={{ enabled: true, threshold: { x: 0.05, y: 0.18 }, acceleration: 12 }}
        onDragCancel={() => setActiveDrag(null)}
        onDragEnd={handleDragEnd}
        onDragStart={handleDragStart}
        sensors={sensors}
      >
        <div className="designer-shell">
          <Card className="designer-panel designer-panel--materials" loading={isLoading} title="物料">
            <MaterialPalette />
          </Card>

          <Card className="designer-panel designer-panel--canvas" loading={isLoading} title="画布">
            <DesignerCanvas
              schema={schema}
              scrollRef={canvasScrollRef}
              selectedNodeId={selectedNodeId}
              onDelete={confirmDeleteNode}
              onSelect={selectNode}
            />
          </Card>

          <Card className="designer-panel designer-panel--inspector" loading={isLoading} title="属性与预览">
            <Tabs
              items={[
                {
                  key: 'property',
                  label: '属性',
                  children: (
                    <PropertyPanel
                      fieldKeys={fieldKeys}
                      node={selectedNode}
                      onDelete={deleteCurrentNode}
                      onUpdate={updateSelectedNode}
                    />
                  ),
                },
                {
                  key: 'preview',
                  label: '预览',
                  children: schema ? (
                    <Space className="designer-preview" direction="vertical" size={14}>
                      <DynamicFormRenderer schema={schema} onSubmit={setPreviewResult} />
                      {previewResult ? (
                        <pre className="designer-preview__result">{JSON.stringify(previewResult, null, 2)}</pre>
                      ) : null}
                    </Space>
                  ) : null,
                },
              ]}
            />
          </Card>
        </div>
        <DragOverlay dropAnimation={null}>
          <DragPreview activeDrag={activeDrag} />
        </DragOverlay>
      </DndContext>
    </main>
  )
}
