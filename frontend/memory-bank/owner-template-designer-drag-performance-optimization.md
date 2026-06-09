# Owner Template Designer Drag Performance Optimization

## Background

模板设计器使用 `@dnd-kit` 实现物料拖入和画布节点排序。目标是让拖拽更流畅、减少卡顿、避免物料拖动时出现重影或不跟手。

本次分析范围：

- `src/pages/owner/templates/OwnerTemplateDesignerPage.tsx`
- `src/features/dynamic-form/components/designer/MaterialPalette.tsx`
- `src/features/dynamic-form/components/designer/CanvasNodeCard.tsx`
- `src/features/dynamic-form/components/designer/CanvasDropZone.tsx`
- `src/features/dynamic-form/components/designer/CanvasFieldPreview.tsx`
- `src/pages/owner/templates/OwnerTemplateDesignerPage.module.css`

## Current Drag Flow

1. 左侧物料通过 `useDraggable` 注册拖拽源。
2. 画布节点通过 `useSortable` 注册可排序节点。
3. 每个容器区域通过 `useDroppable` 和 `SortableContext` 支持放置。
4. 拖拽开始时设置 `activeDrag`，用于 `DragOverlay` 渲染拖拽预览。
5. 拖拽过程中不写入 schema store。
6. 拖拽结束后才执行 `addNode` 或 `reorderNodes`。

这个流程整体是合理的，因为没有在 `onDragMove` 中频繁更新 Zustand store。

## Issues Found

### 1. 物料源卡片和 DragOverlay 同时移动

`MaterialPalette` 原来给物料源按钮设置了拖拽 transform，同时页面也使用 `DragOverlay` 渲染预览。

影响：

- 拖动时源卡片和 overlay 同时移动。
- 产生视觉重影。
- 增加一次额外重绘。
- 用户感知上可能不跟手。

### 2. 画布节点 memo 容易失效

`CanvasNodeCard` 已经使用 `memo`，但父组件传入的回调函数在每次 render 时都会重新创建。

影响：

- `onDelete`、`onAddTabPane`、`onSelect` 等引用变化会让 memo 比较失败。
- 选中节点、开始拖拽、结束拖拽时可能造成更多节点重渲染。

### 3. SortableContext items 每次 render 都重新创建

`CanvasDropZone` 原来直接使用：

```tsx
items={nodes.map((node) => node.id)}
```

影响：

- 每次 render 都创建新数组。
- 嵌套容器越多，重复创建越多。
- dnd-kit 上下文更新成本增加。

### 4. 节点预览组件偏重

`CanvasFieldPreview` 会渲染 AntD 的 `Input`、`Radio`、`Select`、`Upload`、`Card`、`Tabs` 等组件。

影响：

- 大模板中拖动排序时，复杂预览会增加渲染成本。
- 富文本、上传、LLM、嵌套容器的节点更明显。

### 5. 自动滚动参数偏激进

原配置：

```tsx
autoScroll={{ enabled: true, threshold: { x: 0.05, y: 0.18 }, acceleration: 12 }}
```

影响：

- 靠近画布边缘时滚动速度较快。
- 容易造成 drop 目标变化频繁。
- 用户感知为抖动或卡顿。

## Changes Applied

### 1. 物料拖拽只使用 DragOverlay 移动

调整 `MaterialPalette`：

- 移除源物料按钮的 transform。
- 使用 `isDragging` 添加 `designer-material--dragging` class。
- 源物料只显示轻量透明状态。

预期效果：

- 避免源卡片和 overlay 双重移动。
- 减少重绘。
- 拖拽视觉更稳定。

### 2. 增加节点拖拽态 class

调整 `CanvasNodeCard`：

- 从 `useSortable` 获取 `isDragging`。
- 拖拽中添加 `designer-node--dragging` class。

预期效果：

- 可以在 CSS 中对拖拽节点做轻量化样式。
- 降低拖拽态阴影和重绘成本。

### 3. 缓存 SortableContext items

调整 `CanvasDropZone`：

- 使用 `useMemo` 缓存 `nodes.map((node) => node.id)`。

预期效果：

- 减少嵌套容器重复创建数组。
- 降低 dnd-kit 上下文刷新成本。

### 4. memo 化字段预览

调整 `CanvasFieldPreview`：

- 将组件包装为 `memo`。

预期效果：

- node 引用未变化时减少重复渲染。
- 降低复杂 AntD 预览组件的渲染压力。

### 5. 稳定设计器页面回调

调整 `OwnerTemplateDesignerPage`：

- `confirmDeleteNode` 使用 `useCallback`。
- `deleteCurrentNode` 使用 `useCallback`。
- `handleDragStart` 使用 `useCallback`。
- `handleDragCancel` 使用 `useCallback`。
- `handleDragEnd` 使用 `useCallback`。

预期效果：

- 传给画布和节点的函数引用更稳定。
- 提高 `CanvasNodeCard.memo` 命中率。
- 减少无意义重渲染。

### 6. 调整自动滚动参数

调整 `DndContext.autoScroll`：

```tsx
autoScroll={{ enabled: true, threshold: { x: 0.05, y: 0.24 }, acceleration: 8 }}
```

预期效果：

- 边缘拖动时滚动更可控。
- 减少因滚动过快造成的目标抖动。

### 7. 拖拽态样式优化

新增 CSS：

- `designer-material--dragging`
- `designer-node--dragging`
- overlay 添加 `will-change: transform`

预期效果：

- 拖拽元素更容易进入合成层。
- 减少 paint/reflow 压力。
- 拖拽态视觉更轻。

## Verification

已执行：

```bash
npm run build
```

结果：

- TypeScript 构建通过。
- Vite 构建通过。
- 仍存在大 chunk 警告，此警告与拖拽优化无关。

## Follow-up Suggestions

如果后续仍有明显卡顿，可以继续做以下优化：

1. 使用 React Profiler 记录大模板拖拽时的 render 次数。
2. 对 `CanvasNodeCard` 的比较函数继续细化，只让选中态变化影响相关节点。
3. 拖拽期间对非活动节点使用更轻的占位预览。
4. 当节点数超过阈值时，隐藏上传、富文本、LLM 等重型预览细节。
5. 评估是否需要虚拟化大型画布，但这会影响 droppable 测量，需要谨慎。
