# Owner Template Designer 渲染卡顿优化说明

## 背景

模板设计器在属性面板编辑单选选项时，会出现输入卡顿和画布整体刷新感。

根因是属性面板每次输入都会更新全局 `schema`，而旧的 `updateSchemaNode()` 会递归重建整棵 schema 树，导致画布节点引用全部变化。即使只改一个节点，React 也无法判断哪些节点可以跳过渲染。

本次修复保持原有交互不变：属性面板仍然实时映射到画布，不做输入防抖，不延迟 UI 更新。

## 优化点 1：`updateSchemaNode()` 改为结构共享更新

### 文件与函数

- 文件：`src/features/dynamic-form/utils/schemaTree.ts`
- 函数：`updateSchemaNode()`

### 修改前

`updateSchemaNode()` 会递归 `map` 所有节点，并且对每个未命中的节点也返回新对象：

```ts
return {
  ...node,
  children: node.children ? updateList(node.children) : undefined,
}
```

这会导致：

- 每次编辑一个字段，整棵 schema 树所有节点引用都变化。
- `CanvasNodeCard` 即使后续使用 `memo`，也无法跳过渲染，因为 `node` prop 每次都是新引用。
- `DesignerCanvas` 的递归树会整体重新计算。

### 修改后

`updateSchemaNode()` 现在返回 `{ nodes, changed }`：

- 命中目标节点时才克隆并更新该节点。
- 子节点没有变化时，父节点保持原引用。
- 没有命中任何节点时，直接返回原 `schema`。
- 只有命中路径上的节点会生成新引用。

### 优化理由

这是本次性能优化的基础。React 的 memo 优化依赖引用稳定，如果数据层每次都把整棵树变成新对象，视图层再多 memo 也无法生效。

### 前后对比

| 项目 | 修改前 | 修改后 |
| --- | --- | --- |
| 编辑单个节点 | 重建整棵树 | 只更新命中路径 |
| 未变化兄弟节点引用 | 每次变化 | 保持不变 |
| React 跳过渲染能力 | 基本失效 | 可以基于引用跳过 |
| 无效更新 | 仍返回新 schema | 返回原 schema |

## 优化点 2：`CanvasNodeCard` 增加 memo 渲染隔离

### 文件与函数

- 文件：`src/features/dynamic-form/components/designer/CanvasNodeCard.tsx`
- 函数：
  - `CanvasNodeCardComponent()`
  - `areCanvasNodeCardPropsEqual()`
  - `CanvasNodeCard`

### 修改前

`CanvasNodeCard` 是普通函数组件。

只要父级 `DesignerCanvas` 重新渲染，递归树中的节点组件都会重新执行。

另外，`selectedNodeId` 是传给所有节点的公共 prop。即使只切换一个选中节点，所有节点都会收到新的 `selectedNodeId`，普通浅比较无法区分哪些节点真正需要更新。

### 修改后

`CanvasNodeCard` 被拆成内部组件和 memo 包装：

```ts
export const CanvasNodeCard = memo(CanvasNodeCardComponent, areCanvasNodeCardPropsEqual)
```

自定义比较函数只关注：

- `node` 引用是否变化。
- `parentId` 是否变化。
- `onDelete` / `onSelect` 是否变化。
- 当前节点自己的选中状态是否变化。

重点是选中状态比较：

```ts
const previousSelected = previous.selectedNodeId === previous.node.id
const nextSelected = next.selectedNodeId === next.node.id
```

这样 `selectedNodeId` 从 A 变成 B 时，只有 A 和 B 对应的节点需要更新，其他节点可以跳过。

### 优化理由

直接使用默认 `memo` 浅比较不够，因为 `selectedNodeId` 变化会让所有节点的 prop 都变化。

自定义比较函数可以把“全局选中 id 变化”转换成“当前节点是否受影响”，避免无关节点重渲染。

### 前后对比

| 项目 | 修改前 | 修改后 |
| --- | --- | --- |
| 父组件重渲染 | 所有节点函数重新执行 | 未变化节点跳过 |
| 修改单个节点属性 | 整棵画布跟随刷新 | 命中路径相关节点刷新 |
| 切换选中节点 | 所有节点都受 `selectedNodeId` 影响 | 只有旧选中和新选中节点更新 |
| memo 有效性 | 无 | 有结构共享后可生效 |

## Hook 使用权衡

本次只新增了 `React.memo`，没有额外引入 `useMemo`、`useCallback`、`useDeferredValue` 或防抖 hook。

原因如下：

1. `useMemo` 无法解决根因。旧问题来自节点引用全量变化，单纯缓存计算结果没有意义。
2. `useCallback` 收益有限。当前 `onDelete`、`onSelect` 来自上层 store/page，主要瓶颈不在函数引用。
3. `useDeferredValue` 会改变实时反馈节奏，不适合当前“属性面板和画布实时映射”的诉求。
4. 输入防抖会让画布更新延迟，容易被误认为映射不实时，因此本次不使用。
5. `React.memo` 与结构共享配合最直接，语义清晰，风险最低。

## 未纳入本次修复的点

以下点也可能影响大 schema 场景下的体验，但本次没有同步改动，避免扩大风险：

1. `SchemaManagerPanel` 的 `JSON.stringify(schema, null, 2)` 仍会在 schema 变化时重新计算。
2. `DynamicFormRenderer` 仍会在预览区根据 schema 重新生成 Formily schema。
3. choice 联动选项仍只读取 `linkedOptions?.[0]`，这是功能限制，不属于本次渲染卡顿修复范围。

这些点建议在下一轮独立优化，尤其是 Schema 面板和预览面板可以考虑只在 tab 激活时计算。

## 验证情况

已执行：

```bash
npm ci
npm run build
```

结果：

- `npm ci` 成功。
- `npm run build` 失败，但失败点不是本次修改文件。

当前失败来自已有 TypeScript 类型问题：

- `src/features/dynamic-form/utils/designerDrag.ts`
- `src/pages/owner/templates/OwnerTemplateDesignerPage.tsx`

错误类型为 `Property 'xxx' does not exist on type 'never'`。

本次修改涉及的文件没有出现在构建错误列表中。

## 预期效果

修复后，在属性面板编辑单选选项时：

1. schema 不再整棵树重建。
2. 未变化的画布节点保持引用稳定。
3. 未受影响的 `CanvasNodeCard` 可以跳过渲染。
4. 切换选中节点时，只有相关节点更新选中样式。
5. 输入卡顿应明显降低，尤其是在节点数量较多的模板中。

## 涉及文件清单

| 文件 | 修改内容 |
| --- | --- |
| `src/features/dynamic-form/utils/schemaTree.ts` | 优化 `updateSchemaNode()`，改为结构共享 |
| `src/features/dynamic-form/components/designer/CanvasNodeCard.tsx` | 增加 `memo` 和自定义 props 比较 |

