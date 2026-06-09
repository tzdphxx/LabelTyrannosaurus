# Owner Template Designer 问题报告

## 概要

当前 `src/pages/owner/templates` 下的模板设计器存在两类核心问题：

1. 单选物料的选项编辑体验看起来像“只能两个选项”，但实际代码并没有硬限制为两个。
2. 属性面板修改单选选项时，画布会出现整树重渲染，导致输入卡顿、实时映射体感不稳定。

另外，还存在一个明确的隐藏限制：联动选项只支持第一条，后续分支会被忽略。

## 现象

### 1. 单选看起来像只能配置两个选项

在物料默认值里，`radio` 只预置了两个选项：

- `src/features/dynamic-form/materialRegistry.ts`

但属性面板本身使用的是文本区，理论上可以输入任意多行选项：

- `src/features/dynamic-form/components/designer/PropertyPanel.tsx`
- `src/features/dynamic-form/utils/designerFields.ts`

### 2. 属性面板修改后，画布更新有明显卡顿

当在单选的属性面板里修改选项时，页面会出现明显的整体刷新感，而不是只更新当前节点。

### 3. 联动选项存在截断

choice 节点的联动选项只读取第一条：

- `src/features/dynamic-form/components/designer/LinkageRuleEditor.tsx`
- `src/features/dynamic-form/utils/formilySchema.ts`

## 流程梳理

### 输入链路

1. 用户在 `PropertyPanel` 中修改单选选项文本。
2. `onChange` 触发 `onUpdate({ props: { options: ... } })`。
3. `OwnerTemplateDesignerPage` 将其转发给 `useTemplateDesignerStore.updateSelectedNode()`。
4. `updateSelectedNode()` 调用 `updateSchemaNode()` 生成新 `schema`。
5. store 执行 `set({ schema: nextSchema })`。
6. 所有订阅 `schema` 的组件重新渲染：
   - `OwnerTemplateDesignerPage`
   - `DesignerCanvas`
   - `SchemaManagerPanel`
   - 预览区的 `DynamicFormRenderer`

## 根因分析

### 根因 1：`updateSchemaNode()` 会递归重建整棵树

当前实现不是“只更新目标节点”，而是对整棵树进行递归 `map`，并且对每个节点都创建新对象：

- `src/features/dynamic-form/utils/schemaTree.ts`

这意味着即使只改了一个单选选项，整棵 schema 树的节点引用也会全部变化。

结果：

- React 认为整棵树都是新数据
- 画布中的所有节点都重新渲染
- 预览和 schema 面板也会连带刷新

### 根因 2：页面级订阅粒度过粗

`OwnerTemplateDesignerPage` 直接订阅了整份 `schema`，并把它传给：

- 左侧画布
- 中间/右侧属性面板
- 预览页

只要 `schema` 引用变化，整页都会进入一次完整更新。

### 根因 3：画布节点缺少更细粒度的渲染隔离

`DesignerCanvas` 和 `CanvasNodeCard` 目前没有额外的 memo 化隔离机制，且节点树是递归渲染的。

一旦 root `schema` 变化，整棵画布树都会重新走一遍渲染流程。

### 根因 4：预览区还有额外的构建开销

`DynamicFormRenderer` 会根据新的 `schema` 重新生成 Formily schema：

- `src/features/dynamic-form/components/DynamicFormRenderer.tsx`

同时 `SchemaManagerPanel` 会对整个 schema 做 `JSON.stringify`：

- `src/features/dynamic-form/components/designer/SchemaManagerPanel.tsx`

这会进一步放大输入时的卡顿感。

## 其他问题

### 1. 单选默认只有两个选项，不是能力上限

`radio` 的默认值只是两条示例数据，不代表系统只能支持两个选项。

### 2. 联动选项只支持一条

`linkedOptions?.[0]` 是明确限制，如果后续要支持多条件、多分支联动，这里必须改。

### 3. 选项编辑没有做输入节流

当前是输入即更新 store，没有做防抖或分批提交，会放大树重渲染问题。

## 影响范围

- 单选/下拉/多选类节点的属性编辑
- 设计器画布实时预览
- Schema 管理面板
- 预览区表单生成

## 风险判断

这是一个典型的“状态粒度过粗 + 树结构全量重建”问题。

它不会导致数据错误，但会造成：

- 输入卡顿
- 画布闪动
- 大 schema 下明显掉帧
- 联动功能不完整

## 建议修复方向

1. 将 `updateSchemaNode()` 改成结构共享，只修改目标路径，不重建整棵树。
2. 给画布节点增加更细粒度的渲染隔离，减少无关节点重绘。
3. 让属性编辑侧增加输入防抖，避免每个字符都触发全局更新。
4. 把联动选项从单条扩展为多条，避免 `linkedOptions?.[0]` 的单分支限制。

## 结论

单选不是只能支持两个选项，问题出在“默认示例”和“整体重渲染”的体验上。

当前真正的性能瓶颈是：**编辑一个节点时，整棵 schema 树被重新构造，导致画布和预览区一起重渲染**。
