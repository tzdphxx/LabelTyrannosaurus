# 阶段三：动态表单核心

## 目标

完成 LabelHub 的核心动态表单能力，让任务负责人可以搭建模板、保存 schema、预览运行态表单，并确保模板 Designer 和 Schema Renderer 使用同一套 schema。

本阶段技术栈明确为：

- 拖拽编排：dnd-kit
- 表单建模与渲染：Formily
- UI 组件：Ant Design v5
- 状态管理：Zustand
- 数据来源：沿用阶段二 Mock 服务层

本阶段不提前加入虚拟化、缓存、懒加载拆分等性能优化手段。

## 依赖安装

当前 `package.json` 缺少动态表单核心依赖。阶段三采用 Formily 官方 AntD v5 适配，因此需要将 Ant Design 降到 v5。

由用户手动执行：

```bash
npm install antd@^5 @ant-design/icons@^5 @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities @formily/core @formily/react @formily/antd-v5
```

依赖用途：

- `@dnd-kit/core`：拖拽上下文、传感器和拖拽事件。
- `@dnd-kit/sortable`：画布内字段排序和容器内子节点排序。
- `@dnd-kit/utilities`：拖拽位移和样式转换工具。
- `@formily/core`：Formily 表单核心。
- `@formily/react`：Formily React 绑定。
- `@formily/antd-v5`：Ant Design v5 表单物料适配。
- `antd@^5`、`@ant-design/icons@^5`：匹配 Formily AntD v5 适配包。

## 覆盖任务

- 任务 10：Schema 模型与物料注册中心
- 任务 11：模板 Designer
- 任务 12：Schema Renderer

## P0：动态表单最小闭环

### 1. Schema 模型

定义统一 schema 结构，作为 Designer、Renderer、Mock 服务层和后续标注工作台的共同契约。

schema 至少支持：

- 模板 id
- schema 版本
- 节点 id
- 字段 key
- 字段类型
- 字段标题
- 默认值
- 属性配置
- 校验规则
- 显隐条件
- 子节点
- 分组和 Tab

约束：

- schema 必须可序列化。
- schema 中不能保存函数、ReactNode、Formily 实例或拖拽运行时对象。
- Designer 和 Renderer 只能消费同一份 schema 模型，不能各自维护独立结构。
- 字段 key 在同一模板内必须唯一。
- 分组和 Tab 只能通过 `children` 承载子节点。

建议核心类型：

```ts
export type DynamicFieldType =
  | 'input'
  | 'textarea'
  | 'radio'
  | 'checkbox'
  | 'select'
  | 'showItem'
  | 'group'
  | 'tabs'
  | 'tabPane'

export interface DynamicFormSchema {
  id: string
  version: string
  title: string
  nodes: DynamicSchemaNode[]
}

export interface DynamicSchemaNode {
  id: string
  key: string
  type: DynamicFieldType
  title: string
  defaultValue?: unknown
  props: Record<string, unknown>
  rules?: DynamicValidationRule[]
  visibleWhen?: DynamicVisibleRule
  children?: DynamicSchemaNode[]
}
```

### 2. 物料注册中心

建立物料注册表，统一描述物料类型、展示名称、分组、默认 props、默认校验和属性面板配置。

P0 首批物料只实现 8 类：

- 单行输入
- 多行输入
- 单选
- 多选
- 标签选择
- ShowItem 展示项
- 分组容器
- Tab 容器

P0 不实现：

- 富文本
- 文件/图片上传
- JSON 编辑器
- LLM 交互组件

物料注册中心需要提供：

- 根据物料类型生成默认 schema 节点。
- 判断物料是否允许放入某类容器。
- 获取属性面板配置。
- 获取 Renderer 对应的 Formily 组件映射。

### 3. 模板 Designer

新增 Owner 模板设计入口，建议路由：

```text
/app/owner/templates/:templateId/designer
```

Designer 使用三栏结构：

- 左侧物料区
- 中间 schema 画布
- 右侧属性面板

P0 核心能力：

- 从物料区拖拽添加字段。
- 画布内字段排序。
- 分组和 Tab 内子节点排序。
- 点击字段后在属性面板编辑。
- 删除字段时二次确认。
- schema 实时预览。
- 保存 schema 到 Mock 服务层。
- 从 Mock 服务层读取 schema 并回填 Designer。

属性面板 P0 支持编辑：

- 字段标题
- 字段 key
- 默认值
- 选项列表
- 是否必填
- 帮助说明
- 基础显隐条件

### 4. Schema Renderer

Renderer 使用 Formily 渲染业务 schema。

实现要求：

- 建立 `schemaToFormilySchema` 转换层。
- Renderer 只接收业务 schema，不接收 Designer 内部状态。
- Renderer 可以渲染 P0 8 类基础物料。
- 支持默认值回填。
- 支持必填校验。
- 支持基础条件显隐。
- 支持提交时收集字段值。
- 支持字段级错误定位。

提交输出结构：

```ts
export interface DynamicFormSubmitResult {
  templateId: string
  schemaVersion: string
  values: Record<string, unknown>
}
```

### 5. Mock 服务层

模板 schema 数据进入 `src/mocks/`，不得硬编码在页面组件。

新增或扩展模板服务方法：

- 查询模板详情。
- 查询模板 schema。
- 保存模板 schema。
- 预览模板 schema。

服务方法保持异步形态，继续沿用阶段二 Mock 服务层风格，便于后续替换为真实 API Client。

### 6. 状态管理

新增模板设计 store，承载 Designer 跨组件状态。

状态至少包括：

- 当前 schema
- 当前选中节点 id
- 当前拖拽状态
- 当前预览值
- 保存中状态
- 加载中状态
- 最近一次错误
- 是否存在未保存变更

状态边界：

- Store 负责 schema、选中节点、保存状态和未保存状态。
- 服务层负责异步读取和保存，不保存 UI 状态。
- 页面局部 state 只保留弹窗开关、临时输入等短生命周期状态。

## P1：动态表单增强

### 1. 物料增强

在 P0 闭环稳定后补充以下物料：

- 富文本
- 文件/图片上传
- JSON 编辑器
- LLM 交互组件占位

上传和 LLM 组件在 P1 可以先完成 UI 与 schema 占位，不要求真实上传链路和真实模型调用。

### 2. 显隐条件增强

在 P0 基础显隐条件之上，增加更完整配置：

- 依赖字段
- 操作符
- 比较值
- 多条件组合

P1 仍不支持任意 JS 表达式。

### 3. 联动校验增强

增加字段间联动校验：

- 字段 A 满足条件时字段 B 必填。
- 字段 A 的选择影响字段 B 的可选项。
- 字段 A 为空时隐藏或禁用字段 B。

联动校验必须保持可序列化配置，不允许保存函数。

### 4. Schema 管理增强

增加：

- schema JSON 预览。
- schema 导入入口。
- schema 导出入口。
- 模板版本展示。
- 只读预览模式。

### 5. 后续流程准备

为阶段四标注员工作台预留 Renderer 复用能力：

- Renderer 支持传入初始 values。
- Renderer 支持只读模式。
- Renderer 支持提交回调。

本阶段不实现完整标注流程。

## 技术难点执行方案

### 1. dnd-kit 拖拽添加与排序

物料区和画布节点必须使用不同 id 语义：

- 物料区使用固定 material id。
- 画布节点使用 schema node id。

拖拽结束时区分两类行为：

- 从物料区拖入画布：根据物料默认配置创建新节点。
- 画布内拖拽：只调整同级节点顺序。

容器处理：

- 分组和 Tab 通过 `children` 承载子节点。
- 容器需要判断当前拖入物料是否允许接收。
- Tab 容器下应先生成 Tab Pane，再由 Tab Pane 接收具体字段。

拖拽逻辑只更新 schema tree，不直接操作 DOM。

### 2. Schema tree 更新

所有 schema tree 更新通过纯函数完成，避免组件中散落递归逻辑。

需要提供：

- 根据 id 查找节点。
- 根据 id 更新节点。
- 根据 id 删除节点。
- 根据 parentId 插入节点。
- 在同级 children 中重排节点。
- 校验字段 key 是否重复。

删除当前选中节点后，需要同步清空或切换选中节点，避免属性面板引用已删除节点。

### 3. Formily 转换层

建立独立转换函数：

```ts
schemaToFormilySchema(schema: DynamicFormSchema): ISchema
```

转换原则：

- 业务 schema 是唯一输入。
- 物料注册表提供 Formily 组件名、默认 props 和规则映射。
- 必填规则转换为 Formily 校验规则。
- 条件显隐转换为 Formily reactions。
- Renderer 不直接依赖 Designer store。

这样可以保证 Designer、Renderer 和后续标注工作台共享同一套业务 schema。

### 4. 条件显隐

P0 只支持简单规则：

- 依赖字段 key。
- 操作符：等于、不等于、包含、为空、不为空。
- 比较值。

执行方式：

- Designer 属性面板生成可序列化显隐配置。
- `schemaToFormilySchema` 将显隐配置转换为 Formily reactions。
- Renderer 运行时由 Formily 控制字段 visible。

不允许 P0 支持任意 JS 表达式或自定义函数。

### 5. 校验边界

P0 只做可序列化校验规则：

- 必填
- 最小长度
- 最大长度
- 选项枚举

P0 不做自定义函数校验。

提交时要求：

- 能返回字段级错误。
- 能定位到对应字段。
- 能阻止非法提交。

Designer 保存 schema 前也要做结构校验：

- 字段 key 不重复。
- 字段标题不为空。
- Tab 容器至少包含一个 Tab Pane。
- 选项类字段至少包含一个选项。

## 产出

- 一套可序列化动态表单 schema。
- 一套物料注册机制。
- 一个可用模板 Designer。
- 一个可用 Schema Renderer。
- 一套 schema tree 操作工具。
- 一套 schema 到 Formily schema 的转换层。
- Mock 模板 schema 数据与模板服务方法。
- Designer 和 Renderer 使用同一套 schema。

## 验收标准

- Owner 可以进入模板 Designer 页面。
- Owner 可以从物料区拖拽添加字段。
- Owner 可以调整字段顺序。
- Owner 可以编辑字段属性。
- Owner 可以删除字段并获得二次确认。
- Owner 可以保存 schema。
- 保存后的 schema 可以重新读取并回填 Designer。
- Designer 中搭建的模板可以直接被 Renderer 渲染。
- 至少 8 类 P0 基础物料可正常工作。
- 条件显隐和必填校验可用。
- 提交时能定位字段错误。
- Renderer 输出结构化提交结果。
- Designer 和 Renderer 不维护两套 schema。

## 测试计划

### 单元测试

- schema tree 增删改查。
- 节点排序。
- 物料默认节点生成。
- 字段 key 重复校验。
- schema 到 Formily schema 转换。
- 条件显隐规则转换。

### 组件测试

- Designer 可以添加字段。
- Designer 可以选择字段。
- Designer 可以编辑字段属性。
- Designer 可以删除字段。
- 字段排序后 schema 顺序正确。
- 属性面板修改能同步到画布。
- Renderer 可以渲染 P0 物料。
- 必填错误能定位到字段。

### 集成测试

- Owner 创建模板 schema。
- 保存 schema 到 Mock 服务。
- 从 Mock 服务读取 schema。
- Designer 预览区直接渲染同一份 schema。
- Renderer 提交输出结构化 values。

阶段完成后运行：

```bash
npm run build
npm run lint
```

并将验证结果记录到 `memory-bank/progress.md`。

## 风险与约束

- 当前项目原本使用 Ant Design v6，阶段三选择 Formily 官方 AntD v5 适配后，需要降级到 Ant Design v5。
- schema 设计过度复杂会拖慢后续模块，P0 必须限制物料和规则范围。
- Designer 和 Renderer 如果分裂，会导致维护成本很高，必须共享业务 schema。
- 自定义校验函数会带来不可控执行边界，P0 不支持。
- 上传、富文本、JSON 编辑器和 LLM 组件不进入 P0，避免扩大交付范围。
- 本阶段继续使用 Mock 服务层，不接真实后端接口。
