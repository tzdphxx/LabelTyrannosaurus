# 阶段三：动态表单核心

## 目标

完成 LabelHub 的核心能力：模板 Designer、Schema Renderer、字段联动和校验规则。

## 覆盖任务

- 任务 10：Schema 模型与物料注册中心
- 任务 11：模板 Designer
- 任务 12：Schema Renderer

## 实施内容

### 1. Schema 模型

定义统一 schema 结构，至少支持：

- 字段 id
- 字段类型
- 字段标题
- 默认值
- 属性配置
- 校验规则
- 显隐条件
- 子节点
- 分组和 Tab

### 2. 物料注册中心

首批物料：

- 单行输入
- 多行输入
- 单选
- 多选
- 标签选择
- 富文本
- 文件/图片上传
- JSON 编辑器
- LLM 交互组件
- ShowItem
- 分组容器
- Tab 容器

### 3. 模板 Designer

实现三栏结构：

- 左侧物料区
- 中间画布区
- 右侧属性面板

核心能力：

- 拖拽添加字段
- 字段排序
- 字段删除
- 字段属性编辑
- 分组和 Tab 配置
- schema 实时预览

### 4. Schema Renderer

实现：

- 根据 schema 渲染运行态表单
- 字段值收集
- 字段校验
- 条件显隐
- 联动校验
- 提交数据结构输出

## 产出

- 一套可序列化 schema
- 一套物料注册机制
- 一个可用 Designer
- 一个可用 Renderer
- Designer 和 Renderer 使用同一套 schema

## 验收标准

- 在 Designer 里搭建的模板可以直接被 Renderer 渲染
- 至少 8 类基础物料可正常工作
- 条件显隐和必填校验可用
- 提交时能定位字段错误
- schema 可以保存、读取、预览

## 风险

- schema 设计过度复杂会拖慢所有后续模块
- Designer 和 Renderer 如果分裂，会导致维护成本很高
- 自定义校验函数要限制能力边界，避免不可控执行

