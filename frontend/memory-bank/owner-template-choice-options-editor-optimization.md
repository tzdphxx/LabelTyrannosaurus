# 单选选项编辑器优化文档

## 背景

在模板设计器的单选物料属性面板中，`选项` 文本框存在两个问题：

1. 按回车无法稳定换行。
2. 输入过程中仍有卡顿。

相关入口：

- `src/features/dynamic-form/components/designer/PropertyPanel.tsx`
- `src/features/dynamic-form/utils/designerFields.ts`
- `src/stores/templateDesignerStore.ts`

## 当前问题链路

### 当前流程

1. 用户在 `PropertyPanel` 的 `Input.TextArea` 中输入。
2. `onChange` 立即调用 `textToOptions(event.target.value)`。
3. 文本被即时解析成 `DynamicFieldOption[]`。
4. `onUpdate({ props: { options } })` 立即写入全局 `schema`。
5. `updateSelectedNode()` 触发 store 更新。
6. 页面、画布、预览、Schema 面板进入更新链路。
7. 下一次渲染时，`optionsToText(node.props.options)` 又把数组格式化回文本框。

### 回车换行失效原因

文件：`src/features/dynamic-form/utils/designerFields.ts`

函数：

- `textToOptions()`
- `optionsToText()`

当前 `textToOptions()` 会：

```ts
value
  .split('\n')
  .map((line) => line.trim())
  .filter(Boolean)
```

当用户按下 Enter 时，文本框中会出现一个临时空行。但该空行马上被 `filter(Boolean)` 删除。

随后 `optionsToText()` 又根据数组重新生成文本，临时空行无法保留，所以表现为“无法回车换行”。

### 卡顿原因

文件：`src/features/dynamic-form/components/designer/PropertyPanel.tsx`

当前 `onChange` 每个字符都会触发：

- 文本解析。
- 全局 schema 更新。
- selected node 重新计算。
- 属性面板重新渲染。
- 画布预览更新。
- 预览/Formily schema 潜在更新。
- Schema JSON 面板潜在重新 stringify。

之前已经优化了 `updateSchemaNode()` 和 `CanvasNodeCard`，降低了画布整树重渲染，但这不能消除“每个按键都写全局 schema”的输入链路开销。

## 优化目标

1. 文本框必须支持正常回车换行。
2. 输入过程不能每个字符都写全局 schema。
3. 画布仍然能够看到选项变化，但允许短延迟同步。
4. 不引入复杂 hook，不牺牲编辑稳定性。
5. 保留原有 `label=value` 的选项格式。

## 优化点 1：新增本地草稿态，拆分编辑态和提交态

### 涉及文件与函数

建议新增：

- `src/features/dynamic-form/components/designer/ChoiceOptionsEditor.tsx`

调整：

- `src/features/dynamic-form/components/designer/PropertyPanel.tsx`

### 修改前

`PropertyPanel` 直接把 textarea 文本转换为 options，并立即写入 store：

```ts
onChange={(event) => onUpdate({ props: { options: textToOptions(event.target.value) } })}
```

### 修改后

使用独立的 `ChoiceOptionsEditor`：

- `textarea` 使用本地 `draftText`。
- 用户输入只更新本地 state。
- 回车产生的空行保留在本地文本里。
- 通过防抖或 blur 再把文本提交为 `options`。

### 前后对比

| 项目 | 优化前 | 优化后 |
| --- | --- | --- |
| 回车换行 | 空行马上被过滤 | 本地草稿保留空行 |
| 每次输入 | 立即写全局 schema | 只更新本地 draft |
| 画布同步 | 每字符同步，容易卡顿 | 短延迟同步，输入流畅 |
| 光标稳定性 | 可能被格式化打断 | 不被 `optionsToText()` 反复覆盖 |

### 优化理由

多行文本编辑天然需要“编辑中间态”。空行、半截输入、未完成的 `label=` 都是合法的编辑过程，但不一定是最终 options 数据。

把编辑态和提交态拆开，可以同时解决换行失效和输入卡顿。

## 优化点 2：提交 options 时做短防抖

### 涉及文件与函数

建议新增组件内函数：

- `scheduleCommit()`
- `commitDraftText()`

### 修改前

每个字符都调用：

- `textToOptions()`
- `onUpdate()`
- `updateSelectedNode()`

### 修改后

输入时：

- 立即更新 `draftText`。
- 使用 `window.setTimeout` 做 trailing debounce。
- 建议延迟：`300ms`。

失焦时：

- 立即 flush 提交，避免用户切换节点或保存时丢数据。

卸载时：

- 清理 timer。

### 前后对比

| 项目 | 优化前 | 优化后 |
| --- | --- | --- |
| 连续输入 10 个字符 | 10 次 schema 更新 | 通常 1 次 schema 更新 |
| 回车空行 | 立即丢失 | 编辑期保留 |
| 失焦保存 | 无特殊处理 | 立即提交最后草稿 |
| 卡顿风险 | 高 | 明显降低 |

### 优化理由

这里防抖的目标不是延迟输入框显示，而是降低全局 schema 提交频率。

用户输入体验仍是即时的，因为 textarea 由本地 state 控制；只有画布同步被短延迟处理。

## 优化点 3：只在外部节点变化时同步草稿

### 涉及文件与函数

建议在 `ChoiceOptionsEditor` 中使用：

- `useState`
- `useEffect`
- `useRef`

### 同步规则

当以下内容变化时，才从外部 options 重新生成草稿文本：

- 当前节点 id 变化。
- 外部 `options` 引用变化，且不是本组件刚刚提交产生的变化。

### 前后对比

| 项目 | 优化前 | 优化后 |
| --- | --- | --- |
| 输入中重新渲染 | `optionsToText()` 覆盖 textarea | 保留本地 draft |
| 切换节点 | 直接显示新节点 options | 重新初始化 draft |
| 外部导入 schema | 自动同步最新 options | 自动同步最新 options |

### 优化理由

不能简单地把 `optionsToText(options)` 作为 textarea value，否则每次 schema 更新都会重新格式化文本，回车和光标都会被影响。

但也不能完全不监听外部变化，否则切换节点或导入 schema 后会显示旧文本。

## Hook 使用权衡

### 建议使用

1. `useState`
   - 保存 `draftText`。
   - 这是必要状态，不属于过度优化。

2. `useEffect`
   - 在节点切换、外部 options 改变时同步 draft。
   - 在组件卸载时清理 debounce timer。

3. `useRef`
   - 保存 timer id。
   - 保存最近一次提交的文本，避免刚提交后又被外部格式化覆盖。

4. `useCallback`
   - 可用于 `commitDraftText()` 和 `scheduleCommit()`。
   - 如果这些函数只在组件内部使用，也可以不强制使用，避免依赖数组复杂化。

### 不建议使用

1. `useMemo`
   - 不适合作为主要方案。
   - 当前瓶颈不是计算一个值，而是每个按键都提交全局 schema。

2. `useDeferredValue`
   - 不建议用于 textarea value。
   - 可能造成输入内容和真实状态不同步，增加调试复杂度。

3. 过长 debounce
   - 不建议超过 `500ms`。
   - 延迟过大会让画布映射显得不实时。

## 建议实现方案

### Step 1：新增 `ChoiceOptionsEditor`

组件 props：

```ts
interface ChoiceOptionsEditorProps {
  nodeId: string
  options?: DynamicFieldOption[]
  onCommit: (options: DynamicFieldOption[]) => void
}
```

组件行为：

- 初始化：`optionsToText(options)`。
- 输入：`setDraftText(event.target.value)`。
- 输入后：`300ms` 防抖提交。
- blur：立即提交。
- unmount：清理 timer。

### Step 2：替换 `PropertyPanel` 中的 choice textarea

文件：`src/features/dynamic-form/components/designer/PropertyPanel.tsx`

把原来的 `Input.TextArea` 替换为：

```tsx
<ChoiceOptionsEditor
  nodeId={node.id}
  options={node.props.options}
  onCommit={(options) => onUpdate({ props: { options } })}
/>
```

### Step 3：保留 `textToOptions()` 的最终清洗行为

文件：`src/features/dynamic-form/utils/designerFields.ts`

`textToOptions()` 仍可过滤空行，因为过滤发生在提交阶段，不再发生在每个编辑中间态。

这可以避免最终 schema 中出现空 options。

## 验证场景

1. 单选选项文本框中按 Enter，应能稳定换行。
2. 连续输入多行：

```txt
选项 A=option_a
选项 B=option_b
选项 C=option_c
```

画布应在短延迟后显示三项。

3. 输入过程中光标不应跳到末尾。
4. 输入空行后继续输入，空行编辑态不应马上消失。
5. 失焦后 schema 中不应保存空行。
6. 切换到另一个节点后，文本框应显示新节点 options。
7. 大 schema 下连续输入时，卡顿应明显降低。

## 结论

本问题不适合继续靠画布 memo 解决。

真正应该优化的是单选选项编辑器的状态模型：

- textarea 使用本地草稿态。
- options 作为提交态。
- 输入即时显示。
- schema 短防抖提交。
- blur 立即提交。

这样可以同时解决回车换行失效和输入卡顿问题。

