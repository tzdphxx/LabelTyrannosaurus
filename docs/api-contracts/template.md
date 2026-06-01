# Template OWNER 模板版本接口

## 创建 OWNER 模板

Description: Creates a reusable template and its first draft version for the current OWNER.

- URL: `/api/v1/owner/templates`
- Method: `POST`
- 权限角色: `OWNER`
- Owner 模块: BE-B

请求体：

```json
{
  "name": "质检模板",
  "schemaJson": {
    "components": [
      {"type": "Input", "field": "answer", "label": "答案"}
    ]
  },
  "changeNote": "初始版本"
}
```

响应体：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "templateId": 100,
    "taskId": null,
    "ownerId": 10,
    "name": "质检模板",
    "currentVersionNo": 1,
    "currentVersion": {
      "versionId": 200,
      "templateId": 100,
      "taskId": null,
      "ownerId": 10,
      "versionNo": 1,
      "schemaJson": {
        "components": [
          {"type": "Input", "field": "answer", "label": "答案"}
        ]
      },
      "publishedSnapshot": false,
      "state": "DRAFT",
      "changeNote": "初始版本",
      "createdBy": 10,
      "createdAt": "2026-05-29T10:00:00"
    },
    "createdBy": 10,
    "createdAt": "2026-05-29T10:00:00",
    "updatedAt": "2026-05-29T10:00:00"
  },
  "traceId": null
}
```

说明：
- 创建模板会同步创建 `versionNo=1` 的首个版本。
- 新建模板先于任务存在，因此 `taskId=null`，模板归属由 `ownerId` 决定。
- 保存前必须调用 schema 校验。Task7 当前只保证 schema 是合法 JSON object，完整组件规则由 Task8 补齐。
- Task7 不修改 `tasks.published_template_version_id`。

## 查询 OWNER 模板库

Description: Lists reusable templates and their current versions owned by the current OWNER.

- URL: `/api/v1/owner/templates`
- Method: `GET`
- 权限角色: `OWNER`
- Owner 模块: BE-B

响应体：

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "templateId": 100,
      "taskId": null,
      "ownerId": 10,
      "name": "质检模板",
      "currentVersionNo": 2,
      "currentVersion": {
        "versionId": 201,
        "templateId": 100,
        "taskId": null,
        "ownerId": 10,
        "versionNo": 2,
        "schemaJson": {"components": []},
        "publishedSnapshot": false,
        "state": "DRAFT",
        "changeNote": "调整字段",
        "createdBy": 10,
        "createdAt": "2026-05-29T10:10:00"
      },
      "createdBy": 10,
      "createdAt": "2026-05-29T10:00:00",
      "updatedAt": "2026-05-29T10:10:00"
    }
  ],
  "traceId": null
}
```

## 查询模板版本

Description: Reads one immutable or draft template version by id for rendering or validation.

- URL: `/api/v1/template-versions/{versionId}`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

响应体为 `TemplateVersionResponse`，字段同创建模板响应中的 `currentVersion`。

说明：
- 版本不存在返回 `400102`。
- 非管理员且不是模板 Owner 返回 `403001`。

## Fork 模板版本

Description: Creates a new template version from an existing version or the current template version.

- URL: `/api/v1/templates/{templateId}/fork`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

请求体：

```json
{
  "baseVersionId": 200,
  "schemaJson": {
    "components": [
      {"type": "Input", "field": "answer2", "label": "新答案"}
    ]
  },
  "changeNote": "调整字段"
}
```

说明：
- `baseVersionId` 可为空，默认使用模板当前版本。
- `schemaJson` 可为空，默认复制基准版本 schema。
- fork 只插入新版本，不修改旧版本。
- 新版本号为 `templates.current_version_no + 1`，并同步更新 `templates.current_version_no`。
- `publishedSnapshot=true` 的版本不可原地修改，必须通过 fork 生成新版本。
- fork 后的新版本仍属于原模板 Owner，`taskId` 保持原值；owner 模板库中新建模板的版本 `taskId=null`。

## 兼容任务模板接口

以下旧接口仅用于兼容历史任务内模板，不作为新功能主入口：

```text
POST /api/v1/tasks/{taskId}/templates
GET  /api/v1/tasks/{taskId}/templates
```

说明：
- 旧接口创建的模板会写入 `ownerId=tasks.owner_id`，并保留 `taskId` 作为历史来源。
- 前端 Owner 模板库和新任务创建应使用 `/api/v1/owner/templates`。

## 内部 schema 读取能力

BE-B 提供给 BE-A 的 Java Service 能力：

```text
TemplateVersionService.getTemplateSchema(templateVersionId)
```

返回字段：

```json
{
  "versionId": 200,
  "templateId": 100,
  "ownerId": 10,
  "versionNo": 1,
  "schemaJson": {"components": []},
  "publishedSnapshot": true
}
```

说明：
- 该内部能力不推进任务状态。
- BE-A 创建、编辑或发布任务时必须校验模板版本属于任务 Owner，再冻结 `tasks.published_template_version_id`。

## 错误码

| code | 场景 |
|---|---|
| `400101` | 状态不允许，例如尝试原地修改发布快照 |
| `400102` | 任务、模板或版本不存在；请求参数非法 |
| `401001` | 未登录或 token 失效 |
| `403001` | 非管理员且不是任务 Owner |
| `409301` | schema 校验失败 |
| `500001` | 系统错误 |
