# LLM Provider API Contract

Owner: BE-A

## Permission Boundary

- **ADMIN**: Creates, updates, enables, disables, and tests LLM Providers with encrypted API Keys.
- **OWNER**: Reads the list of Admin-enabled models for selection in task AI review configuration.
- **LABELER**: No access to provider management endpoints.

## Admin Endpoints

### GET /api/v1/admin/llm-providers

Permission: ADMIN. Returns all providers with full management fields.

Response fields:

```text
id
providerCode
providerName
baseUrl
defaultModel
customHeaders (masked)
enabled
platformRateLimitPerMinute
taskRateLimitPerMinute
userRateLimitPerMinute
supportVision
supportMultiImage
maxImageCount
visionModel
structuredOutputMode
outputSchema
apiKeyConfigured
ownerId
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

### POST /api/v1/admin/llm-providers

Permission: ADMIN.

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
supportVision optional
supportMultiImage optional
maxImageCount optional
visionModel optional
structuredOutputMode optional
outputSchema optional, JSON string
```

Response: same as admin list item.

Rules:

```text
apiKey is encrypted with LABELHUB_LLM_KEY_ENCRYPTION_SECRET before storage.
apiKey is never returned in any response.
createdBy is recorded from the authenticated ADMIN context.
Each provider record represents an Admin-created selectable model.
```

### PUT /api/v1/admin/llm-providers/{id}

Permission: ADMIN.

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
supportVision optional
supportMultiImage optional
maxImageCount optional
visionModel optional
structuredOutputMode optional
outputSchema optional, JSON string
```

Rules:

```text
If apiKey is omitted or blank, the existing encrypted API key is kept.
If apiKey is provided, it replaces the previous key after encryption.
```

### POST /api/v1/admin/llm-providers/{id}/enable

Permission: ADMIN.
Description: Enables a provider so it can be selected and used by AI review and trigger flows.

Status impact:

```text
enabled=false -> enabled=true
```

### POST /api/v1/admin/llm-providers/{id}/disable

Permission: ADMIN.
Description: Disables a provider to prevent new AI review configuration or model execution from using it.

Status impact:

```text
enabled=true -> enabled=false
Disabled providers must not be selectable by AI review config or new AI review scheduling.
```

### POST /api/v1/admin/llm-providers/{id}/test

Permission: ADMIN.
Description: Performs a live compatibility check against the provider using supplied or stored credentials.

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

## Owner Endpoints

### GET /api/v1/llm-providers

Permission: OWNER. Returns only Admin-enabled providers with a reduced set of fields suitable for model selection.

Response fields:

```text
id
providerCode
providerName
defaultModel
supportVision
supportMultiImage
maxImageCount
visionModel
structuredOutputMode
```

Forbidden response fields:

```text
apiKey
encryptedApiKey
baseUrl
customHeaders
apiKeyConfigured
ownerId
createdBy
createdAt
updatedAt
platformRateLimitPerMinute
taskRateLimitPerMinute
userRateLimitPerMinute
```

Rules:

```text
Only enabled providers are returned.
Owner never sees API keys, encrypted or plaintext.
Owner never sees baseUrl, customHeaders, or rate limit values.
```

## Error Codes

```text
400301 LLM provider header name is invalid
404301 LLM provider not found
500301 LLM API key encryption secret is not configured
500302 LLM API key encryption/decryption failed
500303 LLM provider header JSON is invalid
```
