# Schema Validation 接口

## 校验答案

- URL: `/api/v1/schema/validate-answer`
- Method: `POST`
- 权限角色: `ADMIN`、`OWNER`
- Owner 模块: BE-B

请求体：

```json
{
  "schemaVersionId": 200,
  "answerJson": {
    "answer": "A"
  }
}
```

响应体：

```json
{
  "code": 0,
  "message": "OK",
  "data": [],
  "traceId": null
}
```

校验失败时 `data` 返回字段级错误：

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "path": "/answer",
      "errorCode": 409301,
      "errorMessage": "Required answer is missing"
    }
  ],
  "traceId": null
}
```

说明：

- `path` 使用 JSON Pointer 风格，指向答案字段或 schema 字段。
- `required=true` 时答案字段不能为空。
- `enum` 存在时答案值必须等于枚举数组中的一个 JSON 值。
- `regex` 存在时答案值必须是字符串且匹配正则。
- `ShowItem` 是展示组件，不允许作为答案字段提交。
- `schemaVersionId` 不存在返回 `400102`。
- schema 本身非法或保存 schema 失败使用 `409301`。
