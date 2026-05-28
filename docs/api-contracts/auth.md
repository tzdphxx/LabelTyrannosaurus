# auth

# Auth / Admin User API Contract

Owner：BE\-B

## POST /api/v1/auth/register

权限：公开。

请求字段：

```Plaintext
username
email
password
```

响应字段：

```Plaintext
userId
username
roles = [LABELER]
accessToken
refreshToken
```

错误码：

```Plaintext
400102 参数非法
409101 username/email 已存在
```

## POST /api/v1/auth/login

权限：公开。

请求字段：

```Plaintext
account
password
```

响应字段：

```Plaintext
userId
username
roles
accessToken
refreshToken
```

## GET /api/v1/users/me

权限：登录用户。

响应字段：

```Plaintext
userId
username
email
roles
tokenVersion
```

## PUT /api/v1/admin/users/\{userId\}/roles

权限：ADMIN。

请求字段：

```Plaintext
roles
```

状态影响：

```Plaintext
更新 user_roles。
递增 users.tokenVersion。
旧 token 失效。
```

