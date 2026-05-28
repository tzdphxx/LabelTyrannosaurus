# template

# Template / Schema API Contract

Owner：BE\-B

## POST /api/v1/tasks/\{taskId\}/templates

权限：OWNER。

请求字段：

```Plaintext
name
schemaJson
```

响应字段：

```Plaintext
templateId
templateVersionId
versionNo
```

规则：

```Plaintext
schemaJson 入库前校验。
已发布版本不可原地修改。
```

## POST /api/v1/templates/\{templateId\}/fork

权限：OWNER。

请求字段：

```Plaintext
baseVersionId
changeNote
schemaJson
```

响应字段：

```Plaintext
newTemplateVersionId
versionNo
```

## POST /api/v1/schema/validate\-answer

调用方：BE\-A。

请求字段：

```Plaintext
schemaVersionId
answerJson
```

响应字段：

```Plaintext
valid
errors[].field
errors[].message
```

