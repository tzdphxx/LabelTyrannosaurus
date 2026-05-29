# Template 模板版本接口

## 创建模板

- URL: `/api/v1/tasks/{taskId}/templates`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
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
    "taskId": 1,
    "name": "质检模板",
    "currentVersionNo": 1,
    "currentVersion": {
      "versionId": 200,
      "templateId": 100,
      "taskId": 1,
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
- 保存前必须调用 schema 校验。Task7 当前只保证 schema 是合法 JSON object，完整组件规则由 Task8 补齐。
- Task7 不修改 `tasks.published_template_version_id`。

## 查询任务模板

- URL: `/api/v1/tasks/{taskId}/templates`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

响应体：

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "templateId": 100,
      "taskId": 1,
      "name": "质检模板",
      "currentVersionNo": 2,
      "currentVersion": {
        "versionId": 201,
        "templateId": 100,
        "taskId": 1,
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

- URL: `/api/v1/template-versions/{versionId}`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

响应体为 `TemplateVersionResponse`，字段同创建模板响应中的 `currentVersion`。

说明：
- 版本不存在返回 `400102`。
- 非管理员且不是任务 Owner 返回 `403001`。

## Fork 模板版本

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
  "taskId": 1,
  "versionNo": 1,
  "schemaJson": {"components": []},
  "publishedSnapshot": true
}
```

说明：
- 该内部能力不推进任务状态。
- BE-A 发布任务时可读取版本 schema，并自行冻结 `tasks.published_template_version_id`。

## 错误码

| code | 场景 |
|---|---|
| `400101` | 状态不允许，例如尝试原地修改发布快照 |
| `400102` | 任务、模板或版本不存在；请求参数非法 |
| `401001` | 未登录或 token 失效 |
| `403001` | 非管理员且不是任务 Owner |
| `409301` | schema 校验失败 |
| `500001` | 系统错误 |
