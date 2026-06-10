# LabelHub 前端实施计划总览

本文档目录用于承载 LabelHub 前端项目的 6 个阶段实施计划。每个阶段对应一份独立 Markdown，便于拆任务、评审、执行和验收。

## 阶段列表

1. `01-foundation.md`：基础可跑
2. `02-owner-workbench.md`：Owner 闭环
3. `03-dynamic-form-core.md`：动态表单核心
4. `04-labeler-workflow.md`：标注闭环
5. `05-review-workflow.md`：审核闭环
6. `06-ai-audit-export-qa.md`：AI、审计、导出与总验收

## 总体执行顺序

```text
基础工程 -> Owner 任务流 -> 动态表单核心 -> 标注闭环 -> 审核闭环 -> AI/导出/验收
```

## 总体原则

- 先跑通业务闭环，再做交互优化。
- 先稳定类型和状态模型，再补页面细节。
- 动态表单是项目核心，Designer 和 Renderer 必须共享同一份 schema。
- Mock API 可以先行，但服务层接口语义要接近真实后端。
- 每个阶段完成后都要能独立验收。

