# 标注员 PAUSED 任务限制变更说明

> 记录时间：2026-06-09  
> 适用范围：`src/pages/labeler` 及相关标注员状态映射  
> 背景：标注员获取任务列表时，如果任务状态为 `PAUSED`，则不能点击“查看”和“进入工作台”相关按钮。

## 1. 需求目标

当后端返回暂停状态：

- 任务级状态：`PAUSED`
- 领取/分配级状态：`PAUSED`

前端需要识别该状态，并在标注员页面阻止用户进入暂停任务。

核心行为：

- “我的领取”列表中，`PAUSED` 行的“查看”按钮不可点击。
- “我的领取”列表中，`PAUSED` 行的“进入工作台”按钮不可点击。
- “任务广场”中，`paused` 任务的操作按钮不可点击，避免从另一个入口进入工作台。
- 状态标签展示为“已暂停”。

## 2. 已修改文件

### `src/types/labeling.ts`

新增状态类型：

```ts
export type LabelerTaskStatus =
  | 'available'
  | 'claimed'
  | 'in_progress'
  | 'paused'
  | 'submitted'
  | 'approved'
  | 'rejected'
  | 'ended'

export type LabelerAssignmentStatus =
  | 'CLAIMED'
  | 'DRAFTING'
  | 'PAUSED'
  | 'SUBMITTED'
  | 'AI_RETURNED'
  | 'RETURNED'
  | 'APPROVED'
  | 'CANCELLED'
```

### `src/utils/labeling.ts`

给任务状态补充展示文案和颜色：

```ts
paused: '已暂停'
paused: 'default'
```

### `src/pages/labeler/LabelerSubmissionsPage.tsx`

这是“我的领取”页面，是本次需求的核心页面。

新增：

```ts
PAUSED: '已暂停'
PAUSED: 'default'
{ label: '已暂停', value: 'PAUSED' }
```

按钮控制：

```ts
function canOpenWorkbench(status: LabelerAssignmentSummary['status']) {
  return status !== 'CANCELLED' && status !== 'PAUSED'
}

function canViewAssignment(status: LabelerAssignmentSummary['status']) {
  return status !== 'PAUSED'
}
```

对应按钮：

```tsx
<Button disabled={!canViewAssignment(assignment.status)}>
  查看
</Button>

<Button disabled={!canOpenWorkbench(assignment.status)}>
  进入工作台
</Button>
```

### `src/pages/labeler/LabelerMarketPage.tsx`

任务广场增加 `paused` 筛选项和按钮禁用兜底：

```ts
{ label: 'Paused', value: 'paused' }
```

暂停任务按钮文案：

```ts
if (status === 'paused') {
  return 'Task paused'
}
```

暂停任务不允许跳转：

```ts
if (task.status === 'paused') {
  messageApi.info('This task is paused')
  return
}
```

按钮禁用：

```tsx
disabled={task.status === 'ended' || task.status === 'paused'}
```

### `src/services/labeler/labelingRealService.ts`

真实接口状态映射补充：

- 市场任务返回 `PAUSED` 时映射为前端 `paused`。
- 已领取任务的 `task.status === 'PAUSED'` 时映射为前端 `paused`。
- 已领取题目中任意 `claimStatus === 'PAUSED'` 时映射为前端 `paused`。
- `paused` 任务状态映射为 assignment 状态 `PAUSED`。

关键逻辑：

```ts
if (status === 'PAUSED') {
  return 'paused'
}

case 'paused':
  return 'PAUSED'

if (items.some((item) => item.claimStatus === 'PAUSED')) {
  return 'paused'
}
```

### `src/services/labeler/labelingService.ts`

Mock 服务中补充任务状态到 assignment 状态的映射：

```ts
case 'paused':
  return 'PAUSED'
```

## 3. 验证结果

已运行：

```bash
npm run build
```

结果：

- TypeScript 构建通过。
- Vite 生产构建通过。
- 构建仍有已有的大 chunk 警告，不影响本次修改。

## 4. 下次继续修改时优先阅读

如果下次需要基于这次变更继续修改，请先阅读：

1. `src/pages/labeler/LabelerSubmissionsPage.tsx`
2. `src/pages/labeler/LabelerMarketPage.tsx`
3. `src/types/labeling.ts`
4. `src/services/labeler/labelingRealService.ts`
5. `src/utils/labeling.ts`

重点检查：

- `PAUSED` 是否来自 assignment 状态、task 状态，还是 claim/item 状态。
- 是否还存在其他能进入 `/app/labeler/workbench/:taskId` 的入口。
- 如果后端后续新增更多暂停相关状态，例如 `TASK_PAUSED`、`SUSPENDED`，应继续在服务层统一映射为 `paused` / `PAUSED`。

## 5. 当前默认约定

- 前端任务级暂停状态使用小写：`paused`。
- 前端 assignment 级暂停状态使用后端枚举风格：`PAUSED`。
- 暂停任务不可查看、不可进入工作台。
- 暂停任务只做展示，不触发领取、草稿、提交等业务动作。
