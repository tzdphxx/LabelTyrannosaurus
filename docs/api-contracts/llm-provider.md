# LLM Provider API Contract

Owner: BE-A

## GET /api/v1/admin/llm-providers

Permission: ADMIN, enforced by BE-B Auth later.

Response fields:

```text
id
providerCode
providerName
baseUrl
defaultModel
customHeaders
enabled
platformRateLimitPerMinute
taskRateLimitPerMinute
userRateLimitPerMinute
apiKeyConfigured
createdBy
createdAt
updatedAt
```

Forbidden response fields:

```text
apiKey
encryptedApiKey
```

Sensitive custom header values such as Authorization, Cookie, token, secret, api-key are masked as `******`.

## POST /api/v1/admin/llm-providers

Permission: ADMIN, enforced by BE-B Auth later.

Request fields:

```text
providerCode required
providerName required
baseUrl required
apiKey required
defaultModel required
customHeaders optional
platformRateLimitPerMinute optional, >= 0
taskRateLimitPerMinute optional, >= 0
userRateLimitPerMinute optional, >= 0
```

Response: same as list item.

Rules:

```text
apiKey is encrypted with LABELHUB_LLM_KEY_ENCRYPTION_SECRET before storage.
apiKey is never returned.
createdBy is recorded from X-User-Id until BE-B Auth supplies unified context.
```

## PUT /api/v1/admin/llm-providers/{id}

Permission: ADMIN, enforced by BE-B Auth later.

Request fields:

```text
providerCode required
providerName required
baseUrl required
apiKey optional
defaultModel required
customHeaders optional
platformRateLimitPerMinute optional, >= 0
taskRateLimitPerMinute optional, >= 0
userRateLimitPerMinute optional, >= 0
```

Rules:

```text
If apiKey is omitted or blank, the existing encrypted API key is kept.
If apiKey is provided, it replaces the previous key after encryption.
```

## POST /api/v1/admin/llm-providers/{id}/enable

Status impact:

```text
enabled=false -> enabled=true
```

## POST /api/v1/admin/llm-providers/{id}/disable

Status impact:

```text
enabled=true -> enabled=false
Disabled providers must not be selectable by AI review config or new AI review scheduling.
```

## POST /api/v1/admin/llm-providers/{id}/test

Request fields:

```text
apiKey optional
modelName optional
customHeaders optional
```

Response fields:

```text
success
latencyMs
message
```

Rules:

```text
The backend sends POST {baseUrl}/chat/completions using the OpenAI-compatible chat completions shape.
If apiKey is omitted, the stored encrypted key is decrypted for the test call.
The test response message must not include API key plaintext.
```

## Error Codes

```text
400301 LLM provider header name is invalid
404301 LLM provider not found
500301 LLM API key encryption secret is not configured
500302 LLM API key encryption/decryption failed
500303 LLM provider header JSON is invalid
```
