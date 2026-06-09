# LabelHub API


**简介**:LabelHub API


**HOST**:http://81.71.143.236:18080


**联系人**:LabelHub Backend


**Version**:v1


**接口路径**:/v3/api-docs


[TOC]






# Labeler 角色数据看板


## Labeler 看板总览


**接口地址**:`/api/v1/labeler/dashboard/overview`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回当前标注员的领取、提交、贡献和奖励数据</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|range||query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLabelerDashboardOverviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LabelerDashboardOverviewResponse|LabelerDashboardOverviewResponse|
|&emsp;&emsp;range||string||
|&emsp;&emsp;kpis||LabelerKpis|LabelerKpis|
|&emsp;&emsp;&emsp;&emsp;claimedCount||integer||
|&emsp;&emsp;&emsp;&emsp;submittedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount||integer||
|&emsp;&emsp;&emsp;&emsp;rejectedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvalRate||number||
|&emsp;&emsp;&emsp;&emsp;periodReward||number||
|&emsp;&emsp;&emsp;&emsp;totalReward||number||
|&emsp;&emsp;&emsp;&emsp;reworkCount||integer||
|&emsp;&emsp;contributionTrend||array|ContributionTrendPoint|
|&emsp;&emsp;&emsp;&emsp;date||string||
|&emsp;&emsp;&emsp;&emsp;submittedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount||integer||
|&emsp;&emsp;&emsp;&emsp;reward||number||
|&emsp;&emsp;taskContributions||array|TaskContribution|
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;taskTitle||string||
|&emsp;&emsp;&emsp;&emsp;submittedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount||integer||
|&emsp;&emsp;&emsp;&emsp;totalReward||number||
|&emsp;&emsp;&emsp;&emsp;targetPath||string||
|&emsp;&emsp;todoSummary||TodoSummary|TodoSummary|
|&emsp;&emsp;&emsp;&emsp;claimedNotSubmittedCount||integer||
|&emsp;&emsp;&emsp;&emsp;rejectedNeedFixCount||integer||
|&emsp;&emsp;&emsp;&emsp;continuableTaskCount||integer||
|&emsp;&emsp;alerts||array|Alert|
|&emsp;&emsp;&emsp;&emsp;type||string||
|&emsp;&emsp;&emsp;&emsp;level|可用值:INFO,WARNING|string||
|&emsp;&emsp;&emsp;&emsp;title||string||
|&emsp;&emsp;&emsp;&emsp;description||string||
|&emsp;&emsp;&emsp;&emsp;targetPath||string||
|&emsp;&emsp;generatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"range": "",
		"kpis": {
			"claimedCount": 0,
			"submittedCount": 0,
			"approvedCount": 0,
			"rejectedCount": 0,
			"approvalRate": 0,
			"periodReward": 0,
			"totalReward": 0,
			"reworkCount": 0
		},
		"contributionTrend": [
			{
				"date": "",
				"submittedCount": 0,
				"approvedCount": 0,
				"reward": 0
			}
		],
		"taskContributions": [
			{
				"taskId": 0,
				"taskTitle": "",
				"submittedCount": 0,
				"approvedCount": 0,
				"totalReward": 0,
				"targetPath": ""
			}
		],
		"todoSummary": {
			"claimedNotSubmittedCount": 0,
			"rejectedNeedFixCount": 0,
			"continuableTaskCount": 0
		},
		"alerts": [
			{
				"type": "",
				"level": "",
				"title": "",
				"description": "",
				"targetPath": ""
			}
		],
		"generatedAt": ""
	},
	"traceId": ""
}
```


# Owner 角色数据看板


## Owner 看板总览


**接口地址**:`/api/v1/owner/dashboard/overview`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回当前任务负责人自有任务的 KPI、趋势和待关注项</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|trendDays||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseOwnerDashboardOverviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||OwnerDashboardOverviewResponse|OwnerDashboardOverviewResponse|
|&emsp;&emsp;trendDays||integer(int32)||
|&emsp;&emsp;kpis||OwnerKpis|OwnerKpis|
|&emsp;&emsp;&emsp;&emsp;totalTaskCount||integer||
|&emsp;&emsp;&emsp;&emsp;runningTaskCount||integer||
|&emsp;&emsp;&emsp;&emsp;claimedItemCount||integer||
|&emsp;&emsp;&emsp;&emsp;submittedItemCount||integer||
|&emsp;&emsp;&emsp;&emsp;pendingReviewCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvalRate||number||
|&emsp;&emsp;&emsp;&emsp;rewardCost||number||
|&emsp;&emsp;taskStatusDistribution||object||
|&emsp;&emsp;deliveryTrend||array|DeliveryTrendPoint|
|&emsp;&emsp;&emsp;&emsp;date||string||
|&emsp;&emsp;&emsp;&emsp;claimedCount||integer||
|&emsp;&emsp;&emsp;&emsp;submittedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount||integer||
|&emsp;&emsp;qualitySummary||QualitySummary|QualitySummary|
|&emsp;&emsp;&emsp;&emsp;approvedCount||integer||
|&emsp;&emsp;&emsp;&emsp;rejectedCount||integer||
|&emsp;&emsp;&emsp;&emsp;rejectionRate||number||
|&emsp;&emsp;rewardSummary||RewardSummary|RewardSummary|
|&emsp;&emsp;&emsp;&emsp;totalRewardCost||number||
|&emsp;&emsp;&emsp;&emsp;visibleTaskCount||integer||
|&emsp;&emsp;attentionTasks||array|AttentionTask|
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;title||string||
|&emsp;&emsp;&emsp;&emsp;type||string||
|&emsp;&emsp;&emsp;&emsp;level|可用值:INFO,WARNING|string||
|&emsp;&emsp;&emsp;&emsp;description||string||
|&emsp;&emsp;&emsp;&emsp;targetPath||string||
|&emsp;&emsp;recentTasks||array|RecentTask|
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;title||string||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;progressRate||number||
|&emsp;&emsp;&emsp;&emsp;pendingReviewCount||integer||
|&emsp;&emsp;&emsp;&emsp;updatedAt||string||
|&emsp;&emsp;generatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"trendDays": 0,
		"kpis": {
			"totalTaskCount": 0,
			"runningTaskCount": 0,
			"claimedItemCount": 0,
			"submittedItemCount": 0,
			"pendingReviewCount": 0,
			"approvalRate": 0,
			"rewardCost": 0
		},
		"taskStatusDistribution": {},
		"deliveryTrend": [
			{
				"date": "",
				"claimedCount": 0,
				"submittedCount": 0,
				"approvedCount": 0
			}
		],
		"qualitySummary": {
			"approvedCount": 0,
			"rejectedCount": 0,
			"rejectionRate": 0
		},
		"rewardSummary": {
			"totalRewardCost": 0,
			"visibleTaskCount": 0
		},
		"attentionTasks": [
			{
				"taskId": 0,
				"title": "",
				"type": "",
				"level": "",
				"description": "",
				"targetPath": ""
			}
		],
		"recentTasks": [
			{
				"taskId": 0,
				"title": "",
				"status": "",
				"progressRate": 0,
				"pendingReviewCount": 0,
				"updatedAt": ""
			}
		],
		"generatedAt": ""
	},
	"traceId": ""
}
```


# AI 审核


## 更新 AI 审核配置


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs/{configId}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>更新指定 AI 审核配置。</p>



**请求示例**:


```javascript
{
  "providerId": 1,
  "modelName": "qwen-plus",
  "promptTemplate": "请评估标注结果的准确性和完整性",
  "scoringDimensions": [
    "准确性",
    "完整性",
    "安全性"
  ],
  "passThreshold": 80,
  "manualReviewThreshold": 60,
  "maxRetry": 3,
  "aiFlowPolicy": "MANUAL_FIRST",
  "allowAiDirectApprove": true,
  "allowAiDirectReject": true,
  "rejectThreshold": 40,
  "confidenceThreshold": 0.85,
  "riskFlagsForceManual": [],
  "multimodalEnabled": true,
  "degradationPenalty": 0.2,
  "visionDetail": "auto",
  "maxImagesPerRequest": 5,
  "allowAiDirectApproveWhenDegraded": true,
  "reviewStrategy": "LIGHTWEIGHT",
  "voteModels": [
    {
      "providerId": 1,
      "modelName": "qwen-plus"
    }
  ],
  "voteMinAgreement": 2,
  "dimensionReviewers": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|configId||path|true|integer(int64)||
|aiReviewConfigRequest|AI 审核配置请求|body|true|AiReviewConfigRequest|AiReviewConfigRequest|
|&emsp;&emsp;providerId|LLM 供应商 ID||true|integer(int64)||
|&emsp;&emsp;modelName|模型名称||true|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板（标注规则说明）||true|string||
|&emsp;&emsp;scoringDimensions|评分维度列表||true|array|string|
|&emsp;&emsp;passThreshold|通过阈值（0-100）||true|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值（0-100，低于此值打回）||true|number||
|&emsp;&emsp;maxRetry|最大重试次数（0-10）||false|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略: MANUAL_FIRST | AI_PASS_ONLY | AI_REJECT_ONLY | AI_PASS_AND_REJECT | ALWAYS_MANUAL||false|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过||false|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回||false|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值（0-100）||false|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值（0.00-1.00）||false|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记列表||false|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态（图片/视频输入）||false|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数（0.00-1.00）||false|number||
|&emsp;&emsp;visionDetail|视觉精度: auto | low | high||false|string||
|&emsp;&emsp;maxImagesPerRequest|单次请求最大图片数（0-20）||false|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否仍允许 AI 直接通过||false|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT(单路,默认) | PARALLEL_VOTE(多模型投票) | DEEP_DIMENSION(维度专项) | AGENT_DEBATE(辩论)||false|string||
|&emsp;&emsp;voteModels|投票模型列表, JSON[{providerId,modelName}]; 仅1个时自动复制满足最低票数||false|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数(1-10), 默认2||false|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射, JSON{dim:[{providerId,modelName}]}||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewConfigResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewConfigResponse|AiReviewConfigResponse|
|&emsp;&emsp;id|配置 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;providerId|LLM 供应商 ID|integer(int64)||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板|string||
|&emsp;&emsp;scoringDimensions|评分维度列表|array|string|
|&emsp;&emsp;passThreshold|通过阈值|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值|number||
|&emsp;&emsp;outputSchema|输出 JSON Schema|object||
|&emsp;&emsp;promptVersion|Prompt 版本号|string||
|&emsp;&emsp;maxRetry|最大重试次数|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数|number||
|&emsp;&emsp;visionDetail|视觉精度|string||
|&emsp;&emsp;maxImagesPerRequest|最大图片数|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否允许 AI 直接通过|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE|string||
|&emsp;&emsp;voteModels|投票模型列表|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射|object||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"taskId": 0,
		"providerId": 0,
		"modelName": "",
		"promptTemplate": "",
		"scoringDimensions": [],
		"passThreshold": 0,
		"manualReviewThreshold": 0,
		"outputSchema": {},
		"promptVersion": "",
		"maxRetry": 0,
		"aiFlowPolicy": "",
		"allowAiDirectApprove": true,
		"allowAiDirectReject": true,
		"rejectThreshold": 0,
		"confidenceThreshold": 0,
		"riskFlagsForceManual": [],
		"multimodalEnabled": true,
		"degradationPenalty": 0,
		"visionDetail": "",
		"maxImagesPerRequest": 0,
		"allowAiDirectApproveWhenDegraded": true,
		"reviewStrategy": "",
		"voteModels": [],
		"voteMinAgreement": 0,
		"dimensionReviewers": {}
	},
	"traceId": ""
}
```


## 获取 AI 审核配置


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询任务当前 AI 审核配置。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewConfigResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewConfigResponse|AiReviewConfigResponse|
|&emsp;&emsp;id|配置 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;providerId|LLM 供应商 ID|integer(int64)||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板|string||
|&emsp;&emsp;scoringDimensions|评分维度列表|array|string|
|&emsp;&emsp;passThreshold|通过阈值|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值|number||
|&emsp;&emsp;outputSchema|输出 JSON Schema|object||
|&emsp;&emsp;promptVersion|Prompt 版本号|string||
|&emsp;&emsp;maxRetry|最大重试次数|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数|number||
|&emsp;&emsp;visionDetail|视觉精度|string||
|&emsp;&emsp;maxImagesPerRequest|最大图片数|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否允许 AI 直接通过|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE|string||
|&emsp;&emsp;voteModels|投票模型列表|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射|object||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"taskId": 0,
		"providerId": 0,
		"modelName": "",
		"promptTemplate": "",
		"scoringDimensions": [],
		"passThreshold": 0,
		"manualReviewThreshold": 0,
		"outputSchema": {},
		"promptVersion": "",
		"maxRetry": 0,
		"aiFlowPolicy": "",
		"allowAiDirectApprove": true,
		"allowAiDirectReject": true,
		"rejectThreshold": 0,
		"confidenceThreshold": 0,
		"riskFlagsForceManual": [],
		"multimodalEnabled": true,
		"degradationPenalty": 0,
		"visionDetail": "",
		"maxImagesPerRequest": 0,
		"allowAiDirectApproveWhenDegraded": true,
		"reviewStrategy": "",
		"voteModels": [],
		"voteMinAgreement": 0,
		"dimensionReviewers": {}
	},
	"traceId": ""
}
```


## 保存 AI 审核配置


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>创建或保存任务 AI 审核配置。</p>



**请求示例**:


```javascript
{
  "providerId": 1,
  "modelName": "qwen-plus",
  "promptTemplate": "请评估标注结果的准确性和完整性",
  "scoringDimensions": [
    "准确性",
    "完整性",
    "安全性"
  ],
  "passThreshold": 80,
  "manualReviewThreshold": 60,
  "maxRetry": 3,
  "aiFlowPolicy": "MANUAL_FIRST",
  "allowAiDirectApprove": true,
  "allowAiDirectReject": true,
  "rejectThreshold": 40,
  "confidenceThreshold": 0.85,
  "riskFlagsForceManual": [],
  "multimodalEnabled": true,
  "degradationPenalty": 0.2,
  "visionDetail": "auto",
  "maxImagesPerRequest": 5,
  "allowAiDirectApproveWhenDegraded": true,
  "reviewStrategy": "LIGHTWEIGHT",
  "voteModels": [
    {
      "providerId": 1,
      "modelName": "qwen-plus"
    }
  ],
  "voteMinAgreement": 2,
  "dimensionReviewers": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|aiReviewConfigRequest|AI 审核配置请求|body|true|AiReviewConfigRequest|AiReviewConfigRequest|
|&emsp;&emsp;providerId|LLM 供应商 ID||true|integer(int64)||
|&emsp;&emsp;modelName|模型名称||true|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板（标注规则说明）||true|string||
|&emsp;&emsp;scoringDimensions|评分维度列表||true|array|string|
|&emsp;&emsp;passThreshold|通过阈值（0-100）||true|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值（0-100，低于此值打回）||true|number||
|&emsp;&emsp;maxRetry|最大重试次数（0-10）||false|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略: MANUAL_FIRST | AI_PASS_ONLY | AI_REJECT_ONLY | AI_PASS_AND_REJECT | ALWAYS_MANUAL||false|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过||false|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回||false|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值（0-100）||false|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值（0.00-1.00）||false|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记列表||false|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态（图片/视频输入）||false|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数（0.00-1.00）||false|number||
|&emsp;&emsp;visionDetail|视觉精度: auto | low | high||false|string||
|&emsp;&emsp;maxImagesPerRequest|单次请求最大图片数（0-20）||false|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否仍允许 AI 直接通过||false|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT(单路,默认) | PARALLEL_VOTE(多模型投票) | DEEP_DIMENSION(维度专项) | AGENT_DEBATE(辩论)||false|string||
|&emsp;&emsp;voteModels|投票模型列表, JSON[{providerId,modelName}]; 仅1个时自动复制满足最低票数||false|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数(1-10), 默认2||false|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射, JSON{dim:[{providerId,modelName}]}||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewConfigResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewConfigResponse|AiReviewConfigResponse|
|&emsp;&emsp;id|配置 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;providerId|LLM 供应商 ID|integer(int64)||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板|string||
|&emsp;&emsp;scoringDimensions|评分维度列表|array|string|
|&emsp;&emsp;passThreshold|通过阈值|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值|number||
|&emsp;&emsp;outputSchema|输出 JSON Schema|object||
|&emsp;&emsp;promptVersion|Prompt 版本号|string||
|&emsp;&emsp;maxRetry|最大重试次数|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数|number||
|&emsp;&emsp;visionDetail|视觉精度|string||
|&emsp;&emsp;maxImagesPerRequest|最大图片数|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否允许 AI 直接通过|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE|string||
|&emsp;&emsp;voteModels|投票模型列表|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射|object||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"taskId": 0,
		"providerId": 0,
		"modelName": "",
		"promptTemplate": "",
		"scoringDimensions": [],
		"passThreshold": 0,
		"manualReviewThreshold": 0,
		"outputSchema": {},
		"promptVersion": "",
		"maxRetry": 0,
		"aiFlowPolicy": "",
		"allowAiDirectApprove": true,
		"allowAiDirectReject": true,
		"rejectThreshold": 0,
		"confidenceThreshold": 0,
		"riskFlagsForceManual": [],
		"multimodalEnabled": true,
		"degradationPenalty": 0,
		"visionDetail": "",
		"maxImagesPerRequest": 0,
		"allowAiDirectApproveWhenDegraded": true,
		"reviewStrategy": "",
		"voteModels": [],
		"voteMinAgreement": 0,
		"dimensionReviewers": {}
	},
	"traceId": ""
}
```


## 测试 AI 审核提示词


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs/{configId}/test`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>用样例输入测试 AI 审核提示词和输出结构。</p>



**请求示例**:


```javascript
{
  "itemSnapshot": {},
  "answerJson": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|configId||path|true|integer(int64)||
|aiReviewPromptTestRequest|AiReviewPromptTestRequest|body|true|AiReviewPromptTestRequest|AiReviewPromptTestRequest|
|&emsp;&emsp;itemSnapshot|||true|object||
|&emsp;&emsp;answerJson|||true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewPromptTestResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewPromptTestResponse|AiReviewPromptTestResponse|
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;status|可用值:SUCCESS,PROVIDER_UNAVAILABLE,PROVIDER_ERROR,RATE_LIMITED,TIMEOUT,INVALID_JSON|string||
|&emsp;&emsp;contentText||string||
|&emsp;&emsp;structuredJson||object||
|&emsp;&emsp;rawResponse||string||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"agentRunId": 0,
		"status": "",
		"contentText": "",
		"structuredJson": {},
		"rawResponse": "",
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


## AI 审核结果


**接口地址**:`/api/v1/submissions/{submissionId}/ai-review-result`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定提交的 AI 审核结果。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewResultResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewResultResponse|AiReviewResultResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;providerId||integer(int64)||
|&emsp;&emsp;modelName||string||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;decision||string||
|&emsp;&emsp;averageScore||string||
|&emsp;&emsp;dimensionScores||object||
|&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;suggestion||string||
|&emsp;&emsp;confidence||string||
|&emsp;&emsp;flowAction||string||
|&emsp;&emsp;promptMode||string||
|&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"submissionId": 0,
		"agentRunId": 0,
		"providerId": 0,
		"modelName": "",
		"status": "",
		"decision": "",
		"averageScore": "",
		"dimensionScores": {},
		"riskFlags": "",
		"suggestion": "",
		"confidence": "",
		"flowAction": "",
		"promptMode": "",
		"degraded": true,
		"limitations": [],
		"errorCode": "",
		"errorMessage": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


# 奖励


## 最新奖励规则


**接口地址**:`/api/v1/tasks/{taskId}/reward-rule`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询任务最新奖励规则。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseRewardRuleResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||RewardRuleResponse|RewardRuleResponse|
|&emsp;&emsp;ruleId|规则记录 ID|integer(int64)||
|&emsp;&emsp;taskId|所属任务 ID|integer(int64)||
|&emsp;&emsp;effectiveVersion|规则版本号，每次保存递增|integer(int32)||
|&emsp;&emsp;rewardMode|奖励模式：APPROVED_ITEM（按通过条目计奖）|string||
|&emsp;&emsp;unitReward|单条奖励金额|number||
|&emsp;&emsp;rewardCurrency|奖励货币类型|string||
|&emsp;&emsp;rewardVisible|奖励是否对标注员可见|boolean||
|&emsp;&emsp;effectiveAt|规则生效时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人用户 ID|integer(int64)||
|&emsp;&emsp;createdAt|规则创建时间|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"ruleId": 100,
		"taskId": 10,
		"effectiveVersion": 3,
		"rewardMode": "APPROVED_ITEM",
		"unitReward": 2.5,
		"rewardCurrency": "POINT",
		"rewardVisible": true,
		"effectiveAt": "",
		"createdBy": 1,
		"createdAt": ""
	},
	"traceId": ""
}
```


## 保存奖励规则


**接口地址**:`/api/v1/tasks/{taskId}/reward-rule`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>保存任务奖励规则的新版本。</p>



**请求示例**:


```javascript
{
  "rewardMode": "APPROVED_ITEM",
  "unitReward": 2.5,
  "rewardCurrency": "POINT",
  "rewardVisible": true
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|rewardRuleRequest|保存奖励规则请求|body|true|RewardRuleRequest|RewardRuleRequest|
|&emsp;&emsp;rewardMode|奖励模式，当前仅支持 APPROVED_ITEM（按通过条目计奖）||false|string||
|&emsp;&emsp;unitReward|单条奖励金额||true|number||
|&emsp;&emsp;rewardCurrency|奖励货币类型，默认 POINT（平台积分）||false|string||
|&emsp;&emsp;rewardVisible|奖励是否对标注员可见||false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseRewardRuleResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||RewardRuleResponse|RewardRuleResponse|
|&emsp;&emsp;ruleId|规则记录 ID|integer(int64)||
|&emsp;&emsp;taskId|所属任务 ID|integer(int64)||
|&emsp;&emsp;effectiveVersion|规则版本号，每次保存递增|integer(int32)||
|&emsp;&emsp;rewardMode|奖励模式：APPROVED_ITEM（按通过条目计奖）|string||
|&emsp;&emsp;unitReward|单条奖励金额|number||
|&emsp;&emsp;rewardCurrency|奖励货币类型|string||
|&emsp;&emsp;rewardVisible|奖励是否对标注员可见|boolean||
|&emsp;&emsp;effectiveAt|规则生效时间|string(date-time)||
|&emsp;&emsp;createdBy|创建人用户 ID|integer(int64)||
|&emsp;&emsp;createdAt|规则创建时间|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"ruleId": 100,
		"taskId": 10,
		"effectiveVersion": 3,
		"rewardMode": "APPROVED_ITEM",
		"unitReward": 2.5,
		"rewardCurrency": "POINT",
		"rewardVisible": true,
		"effectiveAt": "",
		"createdBy": 1,
		"createdAt": ""
	},
	"traceId": ""
}
```


# 审核


## 驳回提交


**接口地址**:`/api/v1/reviewer/submissions/{submissionId}/reject`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>审核驳回指定提交。</p>



**请求示例**:


```javascript
{
  "reason": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId||path|true|integer(int64)||
|rejectRequest|RejectRequest|body|true|RejectRequest|RejectRequest|
|&emsp;&emsp;reason|||true|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewActionResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewActionResponse|ReviewActionResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;reviewRecordId||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"submissionStatus": "",
		"reviewRecordId": 0
	},
	"traceId": ""
}
```


## 通过提交


**接口地址**:`/api/v1/reviewer/submissions/{submissionId}/approve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>审核通过指定提交。</p>



**请求示例**:


```javascript
{
  "reviewComment": "",
  "reviewLevel": 0,
  "revisedAnswerJson": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId||path|true|integer(int64)||
|approveRequest|ApproveRequest|body|true|ApproveRequest|ApproveRequest|
|&emsp;&emsp;reviewComment|||false|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||
|&emsp;&emsp;revisedAnswerJson|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewActionResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewActionResponse|ReviewActionResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;reviewRecordId||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"submissionStatus": "",
		"reviewRecordId": 0
	},
	"traceId": ""
}
```


## 批量驳回


**接口地址**:`/api/v1/reviewer/submissions/batch/reject`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量审核驳回提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reason": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchRejectRequest|BatchRejectRequest|body|true|BatchRejectRequest|BatchRejectRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reason|||true|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量转人工


**接口地址**:`/api/v1/reviewer/submissions/batch/mark-manual`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>将提交批量标记为需要人工处理。</p>



**请求示例**:


```javascript
{
  "submissionIds": []
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchMarkManualRequest|BatchMarkManualRequest|body|true|BatchMarkManualRequest|BatchMarkManualRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量通过


**接口地址**:`/api/v1/reviewer/submissions/batch/approve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量审核通过提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reviewComment": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchApproveRequest|BatchApproveRequest|body|true|BatchApproveRequest|BatchApproveRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reviewComment|||false|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量驳回


**接口地址**:`/api/v1/reviewer/submissions/batch-reject`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>兼容契约路径，批量审核驳回提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reason": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchRejectRequest|BatchRejectRequest|body|true|BatchRejectRequest|BatchRejectRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reason|||true|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量转人工


**接口地址**:`/api/v1/reviewer/submissions/batch-mark-manual`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>兼容契约路径，将提交批量标记为需要人工处理。</p>



**请求示例**:


```javascript
{
  "submissionIds": []
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchMarkManualRequest|BatchMarkManualRequest|body|true|BatchMarkManualRequest|BatchMarkManualRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量通过


**接口地址**:`/api/v1/reviewer/submissions/batch-approve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>兼容契约路径，批量审核通过提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reviewComment": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchApproveRequest|BatchApproveRequest|body|true|BatchApproveRequest|BatchApproveRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reviewComment|||false|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 解决冲突组


**接口地址**:`/api/v1/reviewer/conflict-groups/{groupId}/resolve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>选择最终提交并完成冲突仲裁。</p>



**请求示例**:


```javascript
{
  "goldenSubmissionId": 0,
  "reason": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|groupId||path|true|integer(int64)||
|conflictResolveRequest|ConflictResolveRequest|body|true|ConflictResolveRequest|ConflictResolveRequest|
|&emsp;&emsp;goldenSubmissionId|||true|integer(int64)||
|&emsp;&emsp;reason|||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseConflictResolveResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ConflictResolveResponse|ConflictResolveResponse|
|&emsp;&emsp;groupId||integer(int64)||
|&emsp;&emsp;status|可用值:OPEN,RESOLVED|string||
|&emsp;&emsp;goldenSubmissionId||integer(int64)||
|&emsp;&emsp;reviewRecordId||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"groupId": 0,
		"status": "",
		"goldenSubmissionId": 0,
		"reviewRecordId": 0
	},
	"traceId": ""
}
```


## 待审提交列表


**接口地址**:`/api/v1/reviewer/submissions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询审核员可处理的提交列表，支持按任务、提交状态、AI 结论、冲突状态、审核级别筛选。 scope=CLAIMED 查询已领取的提交，scope=AVAILABLE 查询可领取的提交（任务广场），不传则查询全部。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|按任务 ID 筛选|query|false|integer(int64)||
|submissionStatus|按提交状态筛选|query|false|string||
|aiDecision|按 AI 结论筛选：PASS / REJECT / MANUAL_REVIEW|query|false|string||
|aiReviewStatus|按 AI 审核状态筛选|query|false|string||
|conflictStatus|按冲突状态筛选|query|false|string||
|reviewLevel|按审核级别筛选|query|false|integer(int32)||
|scope|查询范围：CLAIMED-已领取，AVAILABLE-可领取（任务广场），不传查全部|query|false|string||
|page|页码，从 1 开始|query|false|integer(int32)||
|size|每页条数，默认 20，最大 100|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePageResponseReviewerSubmissionListItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponseReviewerSubmissionListItem|PageResponseReviewerSubmissionListItem|
|&emsp;&emsp;items||array|ReviewerSubmissionListItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;datasetItemId||integer||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;&emsp;&emsp;aiReviewStatus|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;&emsp;&emsp;conflictStatus||string||
|&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;assignedReviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;updatedAt||string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"submissionId": 0,
				"taskId": 0,
				"datasetItemId": 0,
				"labelerId": 0,
				"submissionStatus": "",
				"aiReviewStatus": "",
				"aiDecision": "",
				"conflictStatus": "",
				"reviewLevel": 0,
				"assignedReviewerId": 0,
				"createdAt": "",
				"updatedAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


## 提交审核详情


**接口地址**:`/api/v1/reviewer/submissions/{submissionId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定提交的审核详情，包含标注答案、AI 评分、审核历史、冲突信息等。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId|提交 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewerSubmissionDetailResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewerSubmissionDetailResponse|ReviewerSubmissionDetailResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;labelerId||integer(int64)||
|&emsp;&emsp;versionNo||integer(int32)||
|&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;answerJson||string||
|&emsp;&emsp;itemJson||string||
|&emsp;&emsp;templateVersionId||integer(int64)||
|&emsp;&emsp;schemaJson||string||
|&emsp;&emsp;aiReviewResult||AiReviewSummary|AiReviewSummary|
|&emsp;&emsp;&emsp;&emsp;aiReviewResultId||integer||
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;agentRunSummary||AgentRunSummary|AgentRunSummary|
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;agentType||string||
|&emsp;&emsp;&emsp;&emsp;modelName||string||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;startedAt||string||
|&emsp;&emsp;&emsp;&emsp;finishedAt||string||
|&emsp;&emsp;reviewRecords||array|ReviewRecordItem|
|&emsp;&emsp;&emsp;&emsp;reviewRecordId||integer||
|&emsp;&emsp;&emsp;&emsp;reviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;action||string||
|&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;reason||string||
|&emsp;&emsp;&emsp;&emsp;reviewComment||string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;versionHistory||array|VersionHistoryItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;status|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;&emsp;&emsp;answerHash||string||
|&emsp;&emsp;&emsp;&emsp;isGolden||boolean||
|&emsp;&emsp;&emsp;&emsp;submittedAt||string||
|&emsp;&emsp;&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;&emsp;&emsp;aiFlowAction||string||
|&emsp;&emsp;&emsp;&emsp;latestReviewAction||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;creatorName||string||
|&emsp;&emsp;latestPreAnnotation||LatestPreAnnotationSummary|LatestPreAnnotationSummary|
|&emsp;&emsp;&emsp;&emsp;preAnnotationId||integer||
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;suggestedAnswerJson||string||
|&emsp;&emsp;&emsp;&emsp;fieldSuggestions||string||
|&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;overallConfidence||string||
|&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;ignoredFields||string||
|&emsp;&emsp;&emsp;&emsp;mediaUnderstanding||string||
|&emsp;&emsp;&emsp;&emsp;finalDiff||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"taskId": 0,
		"assignmentId": 0,
		"datasetItemId": 0,
		"labelerId": 0,
		"versionNo": 0,
		"submissionStatus": "",
		"answerJson": "",
		"itemJson": "",
		"templateVersionId": 0,
		"schemaJson": "",
		"aiReviewResult": {
			"aiReviewResultId": 0,
			"agentRunId": 0,
			"status": "",
			"decision": "",
			"averageScore": "",
			"riskFlags": "",
			"suggestion": "",
			"errorCode": "",
			"promptMode": "",
			"degraded": true,
			"limitations": ""
		},
		"agentRunSummary": {
			"agentRunId": 0,
			"agentType": "",
			"modelName": "",
			"status": "",
			"startedAt": "",
			"finishedAt": ""
		},
		"reviewRecords": [
			{
				"reviewRecordId": 0,
				"reviewerId": 0,
				"action": "",
				"reviewLevel": 0,
				"reason": "",
				"reviewComment": "",
				"createdAt": ""
			}
		],
		"versionHistory": [
			{
				"submissionId": 0,
				"versionNo": 0,
				"status": "",
				"answerHash": "",
				"isGolden": true,
				"submittedAt": "",
				"aiDecision": "",
				"aiFlowAction": "",
				"latestReviewAction": "",
				"createdBy": 0,
				"creatorName": ""
			}
		],
		"latestPreAnnotation": {
			"preAnnotationId": 0,
			"agentRunId": 0,
			"status": "",
			"suggestedAnswerJson": "",
			"fieldSuggestions": "",
			"riskFlags": "",
			"overallConfidence": "",
			"limitations": "",
			"promptMode": "",
			"degraded": true,
			"ignoredFields": "",
			"mediaUnderstanding": "",
			"finalDiff": ""
		}
	},
	"traceId": ""
}
```


## 冲突组列表


**接口地址**:`/api/v1/reviewer/conflict-groups`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询待解决冲突组。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|limit||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListConflictGroupResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|ConflictGroupResponse|
|&emsp;&emsp;groupId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;status|可用值:OPEN,RESOLVED|string||
|&emsp;&emsp;consensusScore||number||
|&emsp;&emsp;goldenSubmissionId||integer(int64)||
|&emsp;&emsp;candidateSubmissions||array|CandidateSubmissionItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;answerJson||string||
|&emsp;&emsp;&emsp;&emsp;aiReviewSummary||AiReviewSummary|AiReviewSummary|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;aiReviewResultId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;&emsp;&emsp;reviewRecords||array|ReviewRecordItem|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewRecordId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;action||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reason||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewComment||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;resolvedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"groupId": 0,
			"taskId": 0,
			"datasetItemId": 0,
			"status": "",
			"consensusScore": 0,
			"goldenSubmissionId": 0,
			"candidateSubmissions": [
				{
					"submissionId": 0,
					"labelerId": 0,
					"answerJson": "",
					"aiReviewSummary": {
						"aiReviewResultId": 0,
						"agentRunId": 0,
						"status": "",
						"decision": "",
						"averageScore": "",
						"riskFlags": "",
						"suggestion": "",
						"errorCode": "",
						"promptMode": "",
						"degraded": true,
						"limitations": ""
					},
					"reviewRecords": [
						{
							"reviewRecordId": 0,
							"reviewerId": 0,
							"action": "",
							"reviewLevel": 0,
							"reason": "",
							"reviewComment": "",
							"createdAt": ""
						}
					],
					"versionNo": 0
				}
			],
			"createdAt": "",
			"resolvedAt": ""
		}
	],
	"traceId": ""
}
```


## 冲突组详情


**接口地址**:`/api/v1/reviewer/conflict-groups/{groupId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询冲突组详情。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|groupId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseConflictGroupResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ConflictGroupResponse|ConflictGroupResponse|
|&emsp;&emsp;groupId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;status|可用值:OPEN,RESOLVED|string||
|&emsp;&emsp;consensusScore||number||
|&emsp;&emsp;goldenSubmissionId||integer(int64)||
|&emsp;&emsp;candidateSubmissions||array|CandidateSubmissionItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;answerJson||string||
|&emsp;&emsp;&emsp;&emsp;aiReviewSummary||AiReviewSummary|AiReviewSummary|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;aiReviewResultId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;&emsp;&emsp;reviewRecords||array|ReviewRecordItem|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewRecordId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;action||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reason||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewComment||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;resolvedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"groupId": 0,
		"taskId": 0,
		"datasetItemId": 0,
		"status": "",
		"consensusScore": 0,
		"goldenSubmissionId": 0,
		"candidateSubmissions": [
			{
				"submissionId": 0,
				"labelerId": 0,
				"answerJson": "",
				"aiReviewSummary": {
					"aiReviewResultId": 0,
					"agentRunId": 0,
					"status": "",
					"decision": "",
					"averageScore": "",
					"riskFlags": "",
					"suggestion": "",
					"errorCode": "",
					"promptMode": "",
					"degraded": true,
					"limitations": ""
				},
				"reviewRecords": [
					{
						"reviewRecordId": 0,
						"reviewerId": 0,
						"action": "",
						"reviewLevel": 0,
						"reason": "",
						"reviewComment": "",
						"createdAt": ""
					}
				],
				"versionNo": 0
			}
		],
		"createdAt": "",
		"resolvedAt": ""
	},
	"traceId": ""
}
```


# 预标注


## 执行预标注


**接口地址**:`/api/v1/assignments/{assignmentId}/pre-annotations/run`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>触发 AI 为当前 assignment 生成整题建议答案。复用任务的 AI 审核配置中的 Provider 和 Prompt。一个 assignment 同时只能有一个预标注在运行。</p>



**请求示例**:


```javascript
{
  "templateVersionId": 0,
  "datasetItemId": 0,
  "currentAnswerJson": "",
  "mode": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|assignmentId|领取记录 ID|path|true|integer(int64)||
|preAnnotationRunRequest|PreAnnotationRunRequest|body|true|PreAnnotationRunRequest|PreAnnotationRunRequest|
|&emsp;&emsp;templateVersionId|||false|integer(int64)||
|&emsp;&emsp;datasetItemId|||false|integer(int64)||
|&emsp;&emsp;currentAnswerJson|||false|string||
|&emsp;&emsp;mode|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePreAnnotationResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PreAnnotationResponse|PreAnnotationResponse|
|&emsp;&emsp;preAnnotationId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;suggestedAnswerJson||object||
|&emsp;&emsp;fieldSuggestions||array|object|
|&emsp;&emsp;riskFlags||array|string|
|&emsp;&emsp;overallConfidence||number||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;promptMode||string||
|&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;ignoredFields||array|string|
|&emsp;&emsp;mediaUnderstanding||object||
|&emsp;&emsp;finalDiff||object||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"preAnnotationId": 0,
		"assignmentId": 0,
		"agentRunId": 0,
		"status": "",
		"suggestedAnswerJson": {},
		"fieldSuggestions": [],
		"riskFlags": [],
		"overallConfidence": 0,
		"limitations": [],
		"promptMode": "",
		"degraded": true,
		"ignoredFields": [],
		"mediaUnderstanding": {},
		"finalDiff": {},
		"errorCode": "",
		"errorMessage": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 预标注详情


**接口地址**:`/api/v1/pre-annotations/{preAnnotationId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定预标注记录的完整信息。LABELER 只能查看自己 assignment 的预标注，OWNER 和 REVIEWER 可查看任意预标注。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|preAnnotationId|预标注记录 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePreAnnotationResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PreAnnotationResponse|PreAnnotationResponse|
|&emsp;&emsp;preAnnotationId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;suggestedAnswerJson||object||
|&emsp;&emsp;fieldSuggestions||array|object|
|&emsp;&emsp;riskFlags||array|string|
|&emsp;&emsp;overallConfidence||number||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;promptMode||string||
|&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;ignoredFields||array|string|
|&emsp;&emsp;mediaUnderstanding||object||
|&emsp;&emsp;finalDiff||object||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"preAnnotationId": 0,
		"assignmentId": 0,
		"agentRunId": 0,
		"status": "",
		"suggestedAnswerJson": {},
		"fieldSuggestions": [],
		"riskFlags": [],
		"overallConfidence": 0,
		"limitations": [],
		"promptMode": "",
		"degraded": true,
		"ignoredFields": [],
		"mediaUnderstanding": {},
		"finalDiff": {},
		"errorCode": "",
		"errorMessage": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 最新预标注结果


**接口地址**:`/api/v1/assignments/{assignmentId}/pre-annotations/latest`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取当前 assignment 最新一次预标注的结果，包含建议答案、字段级建议、置信度和风险标记。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|assignmentId|领取记录 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePreAnnotationResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PreAnnotationResponse|PreAnnotationResponse|
|&emsp;&emsp;preAnnotationId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;suggestedAnswerJson||object||
|&emsp;&emsp;fieldSuggestions||array|object|
|&emsp;&emsp;riskFlags||array|string|
|&emsp;&emsp;overallConfidence||number||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;promptMode||string||
|&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;ignoredFields||array|string|
|&emsp;&emsp;mediaUnderstanding||object||
|&emsp;&emsp;finalDiff||object||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"preAnnotationId": 0,
		"assignmentId": 0,
		"agentRunId": 0,
		"status": "",
		"suggestedAnswerJson": {},
		"fieldSuggestions": [],
		"riskFlags": [],
		"overallConfidence": 0,
		"limitations": [],
		"promptMode": "",
		"degraded": true,
		"ignoredFields": [],
		"mediaUnderstanding": {},
		"finalDiff": {},
		"errorCode": "",
		"errorMessage": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


# 导出


## 导出任务列表


**接口地址**:`/api/v1/tasks/{taskId}/exports`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询任务导出历史。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|page||query|false|integer(int32)||
|pageSize||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportJobPageResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportJobPageResponse|ExportJobPageResponse|
|&emsp;&emsp;items||array|ExportJobResponse|
|&emsp;&emsp;&emsp;&emsp;exportJobId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;exportFormat||string||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;includeAiReview||boolean||
|&emsp;&emsp;&emsp;&emsp;includeAuditTrail||boolean||
|&emsp;&emsp;&emsp;&emsp;includeReviewComment||boolean||
|&emsp;&emsp;&emsp;&emsp;includeLabelerInfo||boolean||
|&emsp;&emsp;&emsp;&emsp;fieldMappingJson||string||
|&emsp;&emsp;&emsp;&emsp;resultFileId||integer||
|&emsp;&emsp;&emsp;&emsp;downloadUrl||string||
|&emsp;&emsp;&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;&emsp;&emsp;traceId||string||
|&emsp;&emsp;&emsp;&emsp;startedAt||string||
|&emsp;&emsp;&emsp;&emsp;finishedAt||string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"exportJobId": 0,
				"taskId": 0,
				"exportFormat": "",
				"status": "",
				"includeAiReview": true,
				"includeAuditTrail": true,
				"includeReviewComment": true,
				"includeLabelerInfo": true,
				"fieldMappingJson": "",
				"resultFileId": 0,
				"downloadUrl": "",
				"errorMessage": "",
				"traceId": "",
				"startedAt": "",
				"finishedAt": "",
				"createdAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


## 创建导出任务


**接口地址**:`/api/v1/tasks/{taskId}/exports`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>按任务创建异步导出任务。</p>



**请求示例**:


```javascript
{
  "exportFormat": "",
  "includeAiReview": true,
  "includeAuditTrail": true,
  "includeReviewComment": true,
  "includeLabelerInfo": true,
  "fieldMappings": [
    {
      "sourceJsonPath": "",
      "targetName": "",
      "formatter": "",
      "include": true
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|createExportRequest|CreateExportRequest|body|true|CreateExportRequest|CreateExportRequest|
|&emsp;&emsp;exportFormat|可用值:JSON,JSONL,CSV,EXCEL||false|string||
|&emsp;&emsp;includeAiReview|||false|boolean||
|&emsp;&emsp;includeAuditTrail|||false|boolean||
|&emsp;&emsp;includeReviewComment|||false|boolean||
|&emsp;&emsp;includeLabelerInfo|||false|boolean||
|&emsp;&emsp;fieldMappings|||false|array|ExportFieldMapping|
|&emsp;&emsp;&emsp;&emsp;sourceJsonPath|||false|string||
|&emsp;&emsp;&emsp;&emsp;targetName|||false|string||
|&emsp;&emsp;&emsp;&emsp;formatter|||false|string||
|&emsp;&emsp;&emsp;&emsp;include|||false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportJobResponse|ExportJobResponse|
|&emsp;&emsp;exportJobId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;exportFormat||string||
|&emsp;&emsp;status||string||
|&emsp;&emsp;includeAiReview||boolean||
|&emsp;&emsp;includeAuditTrail||boolean||
|&emsp;&emsp;includeReviewComment||boolean||
|&emsp;&emsp;includeLabelerInfo||boolean||
|&emsp;&emsp;fieldMappingJson||string||
|&emsp;&emsp;resultFileId||integer(int64)||
|&emsp;&emsp;downloadUrl||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"exportJobId": 0,
		"taskId": 0,
		"exportFormat": "",
		"status": "",
		"includeAiReview": true,
		"includeAuditTrail": true,
		"includeReviewComment": true,
		"includeLabelerInfo": true,
		"fieldMappingJson": "",
		"resultFileId": 0,
		"downloadUrl": "",
		"errorMessage": "",
		"traceId": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 导出任务详情


**接口地址**:`/api/v1/tasks/{taskId}/exports/{exportJobId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询导出任务状态和下载信息。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|exportJobId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportJobResponse|ExportJobResponse|
|&emsp;&emsp;exportJobId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;exportFormat||string||
|&emsp;&emsp;status||string||
|&emsp;&emsp;includeAiReview||boolean||
|&emsp;&emsp;includeAuditTrail||boolean||
|&emsp;&emsp;includeReviewComment||boolean||
|&emsp;&emsp;includeLabelerInfo||boolean||
|&emsp;&emsp;fieldMappingJson||string||
|&emsp;&emsp;resultFileId||integer(int64)||
|&emsp;&emsp;downloadUrl||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"exportJobId": 0,
		"taskId": 0,
		"exportFormat": "",
		"status": "",
		"includeAiReview": true,
		"includeAuditTrail": true,
		"includeReviewComment": true,
		"includeLabelerInfo": true,
		"fieldMappingJson": "",
		"resultFileId": 0,
		"downloadUrl": "",
		"errorMessage": "",
		"traceId": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 优质提交分页


**接口地址**:`/api/v1/owner/export/golden-submissions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询可导出的优质提交快照。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||query|true|integer(int64)||
|lastId||query|false|integer(int64)||
|limit||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportPageResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportPageResponse|ExportPageResponse|
|&emsp;&emsp;items||array|ExportGoldenItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;datasetItemId||integer||
|&emsp;&emsp;&emsp;&emsp;itemJsonRef||string||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;answerJson||string||
|&emsp;&emsp;&emsp;&emsp;aiDecision|可用值:PASS,REJECT,MANUAL_REVIEW|string||
|&emsp;&emsp;&emsp;&emsp;aiSummary||string||
|&emsp;&emsp;&emsp;&emsp;reviewSummary||string||
|&emsp;&emsp;&emsp;&emsp;auditRef||integer||
|&emsp;&emsp;nextCursor||integer(int64)||
|&emsp;&emsp;hasMore||boolean||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"submissionId": 0,
				"taskId": 0,
				"datasetItemId": 0,
				"itemJsonRef": "",
				"labelerId": 0,
				"versionNo": 0,
				"answerJson": "",
				"aiDecision": "",
				"aiSummary": "",
				"reviewSummary": "",
				"auditRef": 0
			}
		],
		"nextCursor": 0,
		"hasMore": true
	},
	"traceId": ""
}
```


# 指派管理


## 查看指派列表


**接口地址**:`/api/v1/tasks/{taskId}/dispatches`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>Owner 查看任务所有指派记录</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListDispatchEntryResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|DispatchEntryResponse|
|&emsp;&emsp;dispatchId|指派记录 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;datasetItemId|数据集项 ID|integer(int64)||
|&emsp;&emsp;labelerId|标注员用户 ID|integer(int64)||
|&emsp;&emsp;status|指派状态：PENDING（待领取）/ CLAIMED（已领取）/ EXPIRED（已过期）/ REVOKED（已撤销）|string||
|&emsp;&emsp;dispatchedAt|指派时间|string(date-time)||
|&emsp;&emsp;claimedAt|领取时间，未领取时为 null|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"dispatchId": 1,
			"taskId": 10,
			"datasetItemId": 500,
			"labelerId": 100,
			"status": "",
			"dispatchedAt": "",
			"claimedAt": ""
		}
	],
	"traceId": ""
}
```


## 批量指派


**接口地址**:`/api/v1/tasks/{taskId}/dispatches`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>Owner 将指定数据项指派给标注员（仅 ASSIGNED 策略）</p>



**请求示例**:


```javascript
{
  "dispatches": [
    {
      "labelerId": 100,
      "datasetItemId": 500
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|dispatchRequest|批量指派请求，Owner 将指定数据项逐条指派给标注员|body|true|DispatchRequest|DispatchRequest|
|&emsp;&emsp;dispatches|单条指派：将一条数据项指派给一个标注员||true|array|DispatchEntry|
|&emsp;&emsp;&emsp;&emsp;labelerId|标注员用户 ID||true|integer||
|&emsp;&emsp;&emsp;&emsp;datasetItemId|数据集项的 ID，必须属于该任务且未被指派||true|integer||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListDispatchEntryResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|DispatchEntryResponse|
|&emsp;&emsp;dispatchId|指派记录 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;datasetItemId|数据集项 ID|integer(int64)||
|&emsp;&emsp;labelerId|标注员用户 ID|integer(int64)||
|&emsp;&emsp;status|指派状态：PENDING（待领取）/ CLAIMED（已领取）/ EXPIRED（已过期）/ REVOKED（已撤销）|string||
|&emsp;&emsp;dispatchedAt|指派时间|string(date-time)||
|&emsp;&emsp;claimedAt|领取时间，未领取时为 null|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"dispatchId": 1,
			"taskId": 10,
			"datasetItemId": 500,
			"labelerId": 100,
			"status": "",
			"dispatchedAt": "",
			"claimedAt": ""
		}
	],
	"traceId": ""
}
```


## 我的指派


**接口地址**:`/api/v1/tasks/{taskId}/dispatches/my`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>标注员查看自己被指派的任务</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListDispatchEntryResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|DispatchEntryResponse|
|&emsp;&emsp;dispatchId|指派记录 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;datasetItemId|数据集项 ID|integer(int64)||
|&emsp;&emsp;labelerId|标注员用户 ID|integer(int64)||
|&emsp;&emsp;status|指派状态：PENDING（待领取）/ CLAIMED（已领取）/ EXPIRED（已过期）/ REVOKED（已撤销）|string||
|&emsp;&emsp;dispatchedAt|指派时间|string(date-time)||
|&emsp;&emsp;claimedAt|领取时间，未领取时为 null|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"dispatchId": 1,
			"taskId": 10,
			"datasetItemId": 500,
			"labelerId": 100,
			"status": "",
			"dispatchedAt": "",
			"claimedAt": ""
		}
	],
	"traceId": ""
}
```


## 撤销指派


**接口地址**:`/api/v1/tasks/{taskId}/dispatches/{dispatchId}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>Owner 撤销未领取的指派</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|dispatchId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


# 审核员工作台


## 审核员任务列表


**接口地址**:`/api/v1/reviewer/tasks`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查看有待审提交的任务列表，作为审核工作台入口导航。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListReviewerTaskSummary|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|ReviewerTaskSummary|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;taskTitle|任务标题|string||
|&emsp;&emsp;pendingCount|该任务下待审提交总数|integer(int32)||
|&emsp;&emsp;myPendingCount|其中归属当前审核员的待审数|integer(int32)||
|&emsp;&emsp;totalReviewedCount|当前审核员在该任务下累计已审核数|integer(int32)||
|&emsp;&emsp;claimed|该任务一级审核是否已被某审核员领取|boolean||
|&emsp;&emsp;claimedByMe|该任务一级审核是否由当前审核员领取|boolean||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"taskId": 10,
			"taskTitle": "",
			"pendingCount": 0,
			"myPendingCount": 0,
			"totalReviewedCount": 0,
			"claimed": true,
			"claimedByMe": true
		}
	],
	"traceId": ""
}
```


## 审核员任务题目分页详情


**接口地址**:`/api/v1/reviewer/tasks/{taskId}/items`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查看当前审核员已领取或已分配任务下的全部题目及题目审核状态。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|itemStatus||query|false|string||
|submissionStatus||query|false|string||
|aiDecision||query|false|string||
|keyword||query|false|string||
|page||query|false|integer(int32)||
|size||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewerTaskItemPageResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewerTaskItemPageResponse|ReviewerTaskItemPageResponse|
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;taskTitle||string||
|&emsp;&emsp;taskStatus||string||
|&emsp;&emsp;totalItemCount||integer(int64)||
|&emsp;&emsp;statusSummary||ReviewerTaskStatusSummary|ReviewerTaskStatusSummary|
|&emsp;&emsp;&emsp;&emsp;unclaimedCount||integer||
|&emsp;&emsp;&emsp;&emsp;claimedCount||integer||
|&emsp;&emsp;&emsp;&emsp;draftCount||integer||
|&emsp;&emsp;&emsp;&emsp;submittedCount||integer||
|&emsp;&emsp;&emsp;&emsp;returnedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount||integer||
|&emsp;&emsp;page||PageResponseReviewerTaskItemRow|PageResponseReviewerTaskItemRow|
|&emsp;&emsp;&emsp;&emsp;items||array|ReviewerTaskItemRow|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;datasetItemId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;externalId||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;itemJson||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;metadataJson||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;itemStatus||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;assignmentId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;assignmentStatus||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;labelerName||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;latestSubmissionId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;submissionStatus||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;submittedAt||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;aiReviewStatus||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewTaskStatus||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;latestReviewAction||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;latestReviewAt||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;canOpenSubmissionDetail||boolean||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;canReview||boolean||
|&emsp;&emsp;&emsp;&emsp;page||integer||
|&emsp;&emsp;&emsp;&emsp;pageSize||integer||
|&emsp;&emsp;&emsp;&emsp;total||integer||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 0,
		"taskTitle": "",
		"taskStatus": "",
		"totalItemCount": 0,
		"statusSummary": {
			"unclaimedCount": 0,
			"claimedCount": 0,
			"draftCount": 0,
			"submittedCount": 0,
			"returnedCount": 0,
			"approvedCount": 0
		},
		"page": {
			"items": [
				{
					"datasetItemId": 0,
					"externalId": "",
					"itemJson": "",
					"metadataJson": "",
					"itemStatus": "",
					"assignmentId": 0,
					"assignmentStatus": "",
					"labelerId": 0,
					"labelerName": "",
					"latestSubmissionId": 0,
					"versionNo": 0,
					"submissionStatus": "",
					"submittedAt": "",
					"aiReviewStatus": "",
					"aiDecision": "",
					"averageScore": "",
					"riskFlags": "",
					"suggestion": "",
					"reviewTaskStatus": "",
					"reviewLevel": 0,
					"latestReviewAction": "",
					"latestReviewAt": "",
					"canOpenSubmissionDetail": true,
					"canReview": true
				}
			],
			"page": 0,
			"pageSize": 0,
			"total": 0
		}
	},
	"traceId": ""
}
```


## 审核员工作台概览


**接口地址**:`/api/v1/reviewer/dashboard`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回当前审核员的工作统计。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewerDashboardResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewerDashboardResponse|ReviewerDashboardResponse|
|&emsp;&emsp;pendingCount||integer(int32)||
|&emsp;&emsp;todayReviewedCount||integer(int32)||
|&emsp;&emsp;totalApproved||integer(int32)||
|&emsp;&emsp;totalRejected||integer(int32)||
|&emsp;&emsp;approvalRate||number||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"pendingCount": 0,
		"todayReviewedCount": 0,
		"totalApproved": 0,
		"totalRejected": 0,
		"approvalRate": 0
	},
	"traceId": ""
}
```


## 审查员 AI 预审状态列表


**接口地址**:`/api/v1/reviewer/ai-review-status`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取当前审查员所有负责提交的 AI 预审状态，包含评分、决策和分配情况。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListReviewerAiReviewStatusItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|ReviewerAiReviewStatusItem|
|&emsp;&emsp;submissionId|提交ID|integer(int64)||
|&emsp;&emsp;taskId|任务ID|integer(int64)||
|&emsp;&emsp;taskTitle|任务标题|string||
|&emsp;&emsp;submissionStatus|提交状态,可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;aiReviewStatus|AI 审核状态,可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;aiDecision|AI 决策（PASS / REJECT / MANUAL_REVIEW）|string||
|&emsp;&emsp;averageScore|AI 平均评分|string||
|&emsp;&emsp;assignedToMe|是否分配给当前审查员|boolean||
|&emsp;&emsp;submittedAt|提交时间|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"submissionId": 0,
			"taskId": 0,
			"taskTitle": "",
			"submissionStatus": "",
			"aiReviewStatus": "",
			"aiDecision": "",
			"averageScore": "",
			"assignedToMe": true,
			"submittedAt": ""
		}
	],
	"traceId": ""
}
```


# 标注员任务工作台


## 标注员任务详情


**接口地址**:`/api/v1/labeler/tasks/{taskId}/detail`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>读取任务详情，并分页返回当前标注员可领取的题目详情。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|itemPage||query|false|integer(int32)||
|itemSize||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLabelerTaskDetailResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LabelerTaskDetailResponse|LabelerTaskDetailResponse|
|&emsp;&emsp;task|任务摘要|TaskSummaryResponse|TaskSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;taskId|任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;title|任务标题|string||
|&emsp;&emsp;&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;&emsp;&emsp;tags|任务标签|array|string|
|&emsp;&emsp;&emsp;&emsp;quota|任务配额|integer||
|&emsp;&emsp;&emsp;&emsp;claimedCount|已领取数|integer||
|&emsp;&emsp;&emsp;&emsp;overlapCount|每条数据需要的标注份数|integer||
|&emsp;&emsp;&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED|string||
|&emsp;&emsp;&emsp;&emsp;deadlineAt|截止时间|string||
|&emsp;&emsp;&emsp;&emsp;publishedAt|发布时间|string||
|&emsp;&emsp;&emsp;&emsp;endedAt|结束时间|string||
|&emsp;&emsp;&emsp;&emsp;createdAt|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|&emsp;&emsp;description||string||
|&emsp;&emsp;instructionRichText||string||
|&emsp;&emsp;templateVersionId||integer(int64)||
|&emsp;&emsp;availableCount||integer(int32)||
|&emsp;&emsp;currentUserClaimedCount||integer(int32)||
|&emsp;&emsp;rewardSummary||RewardSummaryResponse|RewardSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;rewardMode||string||
|&emsp;&emsp;&emsp;&emsp;unitReward||number||
|&emsp;&emsp;&emsp;&emsp;rewardCurrency||string||
|&emsp;&emsp;items|题目摘要|array|ItemSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;itemId|题目 ID|integer||
|&emsp;&emsp;&emsp;&emsp;externalId|题目业务编号|string||
|&emsp;&emsp;&emsp;&emsp;itemJson|题目内容 JSON|string||
|&emsp;&emsp;&emsp;&emsp;metadataJson|题目元数据 JSON|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"task": {
			"taskId": 100,
			"title": "图像分类标注任务",
			"status": "PUBLISHED",
			"tags": "[\"image\",\"classification\"]",
			"quota": 100,
			"claimedCount": 45,
			"overlapCount": 1,
			"strategy": "FCFS",
			"deadlineAt": "2026-06-30T23:59:59",
			"publishedAt": "",
			"endedAt": "",
			"createdAt": "",
			"updatedAt": ""
		},
		"description": "",
		"instructionRichText": "",
		"templateVersionId": 0,
		"availableCount": 0,
		"currentUserClaimedCount": 0,
		"rewardSummary": {
			"rewardMode": "",
			"unitReward": 0,
			"rewardCurrency": ""
		},
		"items": [
			{
				"itemId": 100,
				"externalId": "q1",
				"itemJson": "",
				"metadataJson": ""
			}
		]
	},
	"traceId": ""
}
```


## 标注员答题模板


**接口地址**:`/api/v1/labeler/tasks/{taskId}/answer-template`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>读取任务当前发布模板的 schemaJson。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLabelerTaskTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LabelerTaskTemplateResponse|LabelerTaskTemplateResponse|
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;templateVersionId||integer(int64)||
|&emsp;&emsp;schemaJson||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 0,
		"templateVersionId": 0,
		"schemaJson": ""
	},
	"traceId": ""
}
```


# 导出


## 导出任务列表


**接口地址**:`/api/v1/tasks/{taskId}/exports`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询任务导出历史。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|page||query|false|integer(int32)||
|pageSize||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportJobPageResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportJobPageResponse|ExportJobPageResponse|
|&emsp;&emsp;items||array|ExportJobResponse|
|&emsp;&emsp;&emsp;&emsp;exportJobId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;exportFormat||string||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;includeAiReview||boolean||
|&emsp;&emsp;&emsp;&emsp;includeAuditTrail||boolean||
|&emsp;&emsp;&emsp;&emsp;includeReviewComment||boolean||
|&emsp;&emsp;&emsp;&emsp;includeLabelerInfo||boolean||
|&emsp;&emsp;&emsp;&emsp;fieldMappingJson||string||
|&emsp;&emsp;&emsp;&emsp;resultFileId||integer||
|&emsp;&emsp;&emsp;&emsp;downloadUrl||string||
|&emsp;&emsp;&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;&emsp;&emsp;traceId||string||
|&emsp;&emsp;&emsp;&emsp;startedAt||string||
|&emsp;&emsp;&emsp;&emsp;finishedAt||string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"exportJobId": 0,
				"taskId": 0,
				"exportFormat": "",
				"status": "",
				"includeAiReview": true,
				"includeAuditTrail": true,
				"includeReviewComment": true,
				"includeLabelerInfo": true,
				"fieldMappingJson": "",
				"resultFileId": 0,
				"downloadUrl": "",
				"errorMessage": "",
				"traceId": "",
				"startedAt": "",
				"finishedAt": "",
				"createdAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


## 创建导出任务


**接口地址**:`/api/v1/tasks/{taskId}/exports`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>按任务创建异步导出任务。</p>



**请求示例**:


```javascript
{
  "exportFormat": "",
  "includeAiReview": true,
  "includeAuditTrail": true,
  "includeReviewComment": true,
  "includeLabelerInfo": true,
  "fieldMappings": [
    {
      "sourceJsonPath": "",
      "targetName": "",
      "formatter": "",
      "include": true
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|createExportRequest|CreateExportRequest|body|true|CreateExportRequest|CreateExportRequest|
|&emsp;&emsp;exportFormat|可用值:JSON,JSONL,CSV,EXCEL||false|string||
|&emsp;&emsp;includeAiReview|||false|boolean||
|&emsp;&emsp;includeAuditTrail|||false|boolean||
|&emsp;&emsp;includeReviewComment|||false|boolean||
|&emsp;&emsp;includeLabelerInfo|||false|boolean||
|&emsp;&emsp;fieldMappings|||false|array|ExportFieldMapping|
|&emsp;&emsp;&emsp;&emsp;sourceJsonPath|||false|string||
|&emsp;&emsp;&emsp;&emsp;targetName|||false|string||
|&emsp;&emsp;&emsp;&emsp;formatter|||false|string||
|&emsp;&emsp;&emsp;&emsp;include|||false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportJobResponse|ExportJobResponse|
|&emsp;&emsp;exportJobId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;exportFormat||string||
|&emsp;&emsp;status||string||
|&emsp;&emsp;includeAiReview||boolean||
|&emsp;&emsp;includeAuditTrail||boolean||
|&emsp;&emsp;includeReviewComment||boolean||
|&emsp;&emsp;includeLabelerInfo||boolean||
|&emsp;&emsp;fieldMappingJson||string||
|&emsp;&emsp;resultFileId||integer(int64)||
|&emsp;&emsp;downloadUrl||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"exportJobId": 0,
		"taskId": 0,
		"exportFormat": "",
		"status": "",
		"includeAiReview": true,
		"includeAuditTrail": true,
		"includeReviewComment": true,
		"includeLabelerInfo": true,
		"fieldMappingJson": "",
		"resultFileId": 0,
		"downloadUrl": "",
		"errorMessage": "",
		"traceId": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 导出任务详情


**接口地址**:`/api/v1/tasks/{taskId}/exports/{exportJobId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询导出任务状态和下载信息。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|exportJobId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportJobResponse|ExportJobResponse|
|&emsp;&emsp;exportJobId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;exportFormat||string||
|&emsp;&emsp;status||string||
|&emsp;&emsp;includeAiReview||boolean||
|&emsp;&emsp;includeAuditTrail||boolean||
|&emsp;&emsp;includeReviewComment||boolean||
|&emsp;&emsp;includeLabelerInfo||boolean||
|&emsp;&emsp;fieldMappingJson||string||
|&emsp;&emsp;resultFileId||integer(int64)||
|&emsp;&emsp;downloadUrl||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"exportJobId": 0,
		"taskId": 0,
		"exportFormat": "",
		"status": "",
		"includeAiReview": true,
		"includeAuditTrail": true,
		"includeReviewComment": true,
		"includeLabelerInfo": true,
		"fieldMappingJson": "",
		"resultFileId": 0,
		"downloadUrl": "",
		"errorMessage": "",
		"traceId": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 优质提交分页


**接口地址**:`/api/v1/owner/export/golden-submissions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询可导出的优质提交快照。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||query|true|integer(int64)||
|lastId||query|false|integer(int64)||
|limit||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseExportPageResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ExportPageResponse|ExportPageResponse|
|&emsp;&emsp;items||array|ExportGoldenItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;datasetItemId||integer||
|&emsp;&emsp;&emsp;&emsp;itemJsonRef||string||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;answerJson||string||
|&emsp;&emsp;&emsp;&emsp;aiDecision|可用值:PASS,REJECT,MANUAL_REVIEW|string||
|&emsp;&emsp;&emsp;&emsp;aiSummary||string||
|&emsp;&emsp;&emsp;&emsp;reviewSummary||string||
|&emsp;&emsp;&emsp;&emsp;auditRef||integer||
|&emsp;&emsp;nextCursor||integer(int64)||
|&emsp;&emsp;hasMore||boolean||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"submissionId": 0,
				"taskId": 0,
				"datasetItemId": 0,
				"itemJsonRef": "",
				"labelerId": 0,
				"versionNo": 0,
				"answerJson": "",
				"aiDecision": "",
				"aiSummary": "",
				"reviewSummary": "",
				"auditRef": 0
			}
		],
		"nextCursor": 0,
		"hasMore": true
	},
	"traceId": ""
}
```


# 领取


## 读取草稿


**接口地址**:`/api/v1/claims/{claimId}/draft`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>读取当前标注题目的草稿内容。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|claimId|领取 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAssignmentDraftResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AssignmentDraftResponse|AssignmentDraftResponse|
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;draftAnswerJson||string||
|&emsp;&emsp;draftVersion||integer(int32)||
|&emsp;&emsp;status|可用值:CLAIMED,DRAFTING,SUBMITTED,AI_RETURNED,RETURNED,APPROVED,CANCELLED|string||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"assignmentId": 0,
		"draftAnswerJson": "",
		"draftVersion": 0,
		"status": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 保存草稿


**接口地址**:`/api/v1/claims/{claimId}/draft`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>保存当前标注题目的答案草稿，支持增量更新。</p>



**请求示例**:


```javascript
{
  "answerJson": "",
  "clientVersion": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|claimId|领取 ID|path|true|integer(int64)||
|assignmentDraftSaveRequest|AssignmentDraftSaveRequest|body|true|AssignmentDraftSaveRequest|AssignmentDraftSaveRequest|
|&emsp;&emsp;answerJson|||true|string||
|&emsp;&emsp;clientVersion|||true|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAssignmentDraftResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AssignmentDraftResponse|AssignmentDraftResponse|
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;draftAnswerJson||string||
|&emsp;&emsp;draftVersion||integer(int32)||
|&emsp;&emsp;status|可用值:CLAIMED,DRAFTING,SUBMITTED,AI_RETURNED,RETURNED,APPROVED,CANCELLED|string||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"assignmentId": 0,
		"draftAnswerJson": "",
		"draftVersion": 0,
		"status": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 领取题目


**接口地址**:`/api/v1/tasks/{taskId}/items/claim`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>当前标注员在指定任务下领取一个可标注的题目。三种领取策略（FCFS / QUOTA_GRAB / ASSIGNED）均通过此入口。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|任务 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAssignmentClaimResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AssignmentClaimResponse|AssignmentClaimResponse|
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;templateVersionId||integer(int64)||
|&emsp;&emsp;schemaJson||string||
|&emsp;&emsp;itemJson||string||
|&emsp;&emsp;draftAnswerJson||string||
|&emsp;&emsp;draftVersion||integer(int32)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"assignmentId": 0,
		"datasetItemId": 0,
		"templateVersionId": 0,
		"schemaJson": "",
		"itemJson": "",
		"draftAnswerJson": "",
		"draftVersion": 0
	},
	"traceId": ""
}
```


## 提交标注答案


**接口地址**:`/api/v1/claims/{claimId}/submit`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>提交当前领取题目的最终标注答案，提交后进入 AI 预审流程。</p>



**请求示例**:


```javascript
{
  "answerJson": "",
  "clientVersion": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|claimId|领取 ID|path|true|integer(int64)||
|submissionSubmitRequest|SubmissionSubmitRequest|body|true|SubmissionSubmitRequest|SubmissionSubmitRequest|
|&emsp;&emsp;answerJson|||true|string||
|&emsp;&emsp;clientVersion|||true|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseSubmissionSubmitResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||SubmissionSubmitResponse|SubmissionSubmitResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;versionNo||integer(int32)||
|&emsp;&emsp;status|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;answerHash||string||
|&emsp;&emsp;agentRunId||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"assignmentId": 0,
		"versionNo": 0,
		"status": "",
		"answerHash": "",
		"agentRunId": 0
	},
	"traceId": ""
}
```


## 我的领取列表


**接口地址**:`/api/v1/claims`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>标注员查看自己领取过的题目列表，按领取时间倒序。支持按任务ID和状态筛选。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|按任务 ID 筛选|query|false|integer(int64)||
|status|按领取状态筛选|query|false|string||
|page|页码，从 1 开始|query|false|integer(int32)||
|size|每页条数，默认 20|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListClaimedTaskResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|ClaimedTaskResponse|
|&emsp;&emsp;task|任务摘要|TaskSummaryResponse|TaskSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;taskId|任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;title|任务标题|string||
|&emsp;&emsp;&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;&emsp;&emsp;tags|任务标签|array|string|
|&emsp;&emsp;&emsp;&emsp;quota|任务配额|integer||
|&emsp;&emsp;&emsp;&emsp;claimedCount|已领取数|integer||
|&emsp;&emsp;&emsp;&emsp;overlapCount|每条数据需要的标注份数|integer||
|&emsp;&emsp;&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED|string||
|&emsp;&emsp;&emsp;&emsp;deadlineAt|截止时间|string||
|&emsp;&emsp;&emsp;&emsp;publishedAt|发布时间|string||
|&emsp;&emsp;&emsp;&emsp;endedAt|结束时间|string||
|&emsp;&emsp;&emsp;&emsp;createdAt|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|&emsp;&emsp;myClaimedCount|当前用户在该任务下的已领取数|integer(int32)||
|&emsp;&emsp;mySubmittedCount|当前用户在该任务下的已提交数|integer(int32)||
|&emsp;&emsp;myApprovedCount|当前用户在该任务下的已通过数|integer(int32)||
|&emsp;&emsp;items|已领取题目|array|ClaimedItemResponse|
|&emsp;&emsp;&emsp;&emsp;claimId|领取 ID|integer||
|&emsp;&emsp;&emsp;&emsp;itemId|题目 ID|integer||
|&emsp;&emsp;&emsp;&emsp;externalId|题目业务编号|string||
|&emsp;&emsp;&emsp;&emsp;claimStatus|领取状态|string||
|&emsp;&emsp;&emsp;&emsp;itemJson|题目内容 JSON|string||
|&emsp;&emsp;&emsp;&emsp;metadataJson|题目元数据 JSON|string||
|&emsp;&emsp;&emsp;&emsp;draftVersion|草稿版本号|integer||
|&emsp;&emsp;&emsp;&emsp;latestSubmissionStatus|最新提交状态|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"task": {
				"taskId": 100,
				"title": "图像分类标注任务",
				"status": "PUBLISHED",
				"tags": "[\"image\",\"classification\"]",
				"quota": 100,
				"claimedCount": 45,
				"overlapCount": 1,
				"strategy": "FCFS",
				"deadlineAt": "2026-06-30T23:59:59",
				"publishedAt": "",
				"endedAt": "",
				"createdAt": "",
				"updatedAt": ""
			},
			"myClaimedCount": 5,
			"mySubmittedCount": 3,
			"myApprovedCount": 2,
			"items": [
				{
					"claimId": 200,
					"itemId": 100,
					"externalId": "",
					"claimStatus": "CLAIMED",
					"itemJson": "",
					"metadataJson": "",
					"draftVersion": 3,
					"latestSubmissionStatus": "",
					"updatedAt": ""
				}
			]
		}
	],
	"traceId": ""
}
```


## 领取详情


**接口地址**:`/api/v1/claims/{claimId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定领取的详情，包含题目数据、模板信息、当前草稿和提交状态。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|claimId|领取 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAssignmentDetailResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AssignmentDetailResponse|AssignmentDetailResponse|
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;templateVersionId||integer(int64)||
|&emsp;&emsp;assignmentStatus|可用值:CLAIMED,DRAFTING,SUBMITTED,AI_RETURNED,RETURNED,APPROVED,CANCELLED|string||
|&emsp;&emsp;schemaJson||string||
|&emsp;&emsp;itemJson||string||
|&emsp;&emsp;draftAnswerJson||string||
|&emsp;&emsp;draftVersion||integer(int32)||
|&emsp;&emsp;latestSubmissionId||integer(int64)||
|&emsp;&emsp;latestSubmissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;returnedReason||string||
|&emsp;&emsp;returnedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"assignmentId": 0,
		"taskId": 0,
		"datasetItemId": 0,
		"templateVersionId": 0,
		"assignmentStatus": "",
		"schemaJson": "",
		"itemJson": "",
		"draftAnswerJson": "",
		"draftVersion": 0,
		"latestSubmissionId": 0,
		"latestSubmissionStatus": "",
		"returnedReason": "",
		"returnedAt": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


# LLM 厂商管理


## 更新 Provider


**接口地址**:`/api/v1/admin/llm-providers/{providerId}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>ADMIN 更新全局 LLM Provider 配置。</p>



**请求示例**:


```javascript
{
  "providerCode": "",
  "providerName": "",
  "baseUrl": "",
  "apiKey": "",
  "defaultModel": "",
  "customHeaders": {},
  "platformRateLimitPerMinute": 0,
  "taskRateLimitPerMinute": 0,
  "userRateLimitPerMinute": 0,
  "supportVision": true,
  "supportMultiImage": true,
  "maxImageCount": 0,
  "visionModel": "",
  "structuredOutputMode": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|providerId||path|true|integer(int64)||
|updateLlmProviderRequest|UpdateLlmProviderRequest|body|true|UpdateLlmProviderRequest|UpdateLlmProviderRequest|
|&emsp;&emsp;providerCode|||true|string||
|&emsp;&emsp;providerName|||true|string||
|&emsp;&emsp;baseUrl|||true|string||
|&emsp;&emsp;apiKey|||false|string||
|&emsp;&emsp;defaultModel|||true|string||
|&emsp;&emsp;customHeaders|||false|object||
|&emsp;&emsp;platformRateLimitPerMinute|||false|integer(int32)||
|&emsp;&emsp;taskRateLimitPerMinute|||false|integer(int32)||
|&emsp;&emsp;userRateLimitPerMinute|||false|integer(int32)||
|&emsp;&emsp;supportVision|||false|boolean||
|&emsp;&emsp;supportMultiImage|||false|boolean||
|&emsp;&emsp;maxImageCount|||false|integer(int32)||
|&emsp;&emsp;visionModel|||false|string||
|&emsp;&emsp;structuredOutputMode|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmProviderResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmProviderResponse|LlmProviderResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;providerCode||string||
|&emsp;&emsp;providerName||string||
|&emsp;&emsp;baseUrl||string||
|&emsp;&emsp;defaultModel||string||
|&emsp;&emsp;customHeaders||object||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;platformRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;taskRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;userRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;supportVision||boolean||
|&emsp;&emsp;supportMultiImage||boolean||
|&emsp;&emsp;maxImageCount||integer(int32)||
|&emsp;&emsp;visionModel||string||
|&emsp;&emsp;structuredOutputMode||string||
|&emsp;&emsp;apiKeyConfigured||boolean||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"providerCode": "",
		"providerName": "",
		"baseUrl": "",
		"defaultModel": "",
		"customHeaders": {},
		"enabled": true,
		"platformRateLimitPerMinute": 0,
		"taskRateLimitPerMinute": 0,
		"userRateLimitPerMinute": 0,
		"supportVision": true,
		"supportMultiImage": true,
		"maxImageCount": 0,
		"visionModel": "",
		"structuredOutputMode": "",
		"apiKeyConfigured": true,
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 创建 Provider


**接口地址**:`/api/v1/admin/llm-providers`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>ADMIN 创建全局 LLM Provider 配置。</p>



**请求示例**:


```javascript
{
  "providerCode": "",
  "providerName": "",
  "baseUrl": "",
  "apiKey": "",
  "defaultModel": "",
  "customHeaders": {},
  "platformRateLimitPerMinute": 0,
  "taskRateLimitPerMinute": 0,
  "userRateLimitPerMinute": 0,
  "supportVision": true,
  "supportMultiImage": true,
  "maxImageCount": 0,
  "visionModel": "",
  "structuredOutputMode": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createLlmProviderRequest|CreateLlmProviderRequest|body|true|CreateLlmProviderRequest|CreateLlmProviderRequest|
|&emsp;&emsp;providerCode|||true|string||
|&emsp;&emsp;providerName|||true|string||
|&emsp;&emsp;baseUrl|||true|string||
|&emsp;&emsp;apiKey|||true|string||
|&emsp;&emsp;defaultModel|||true|string||
|&emsp;&emsp;customHeaders|||false|object||
|&emsp;&emsp;platformRateLimitPerMinute|||false|integer(int32)||
|&emsp;&emsp;taskRateLimitPerMinute|||false|integer(int32)||
|&emsp;&emsp;userRateLimitPerMinute|||false|integer(int32)||
|&emsp;&emsp;supportVision|||false|boolean||
|&emsp;&emsp;supportMultiImage|||false|boolean||
|&emsp;&emsp;maxImageCount|||false|integer(int32)||
|&emsp;&emsp;visionModel|||false|string||
|&emsp;&emsp;structuredOutputMode|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmProviderResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmProviderResponse|LlmProviderResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;providerCode||string||
|&emsp;&emsp;providerName||string||
|&emsp;&emsp;baseUrl||string||
|&emsp;&emsp;defaultModel||string||
|&emsp;&emsp;customHeaders||object||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;platformRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;taskRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;userRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;supportVision||boolean||
|&emsp;&emsp;supportMultiImage||boolean||
|&emsp;&emsp;maxImageCount||integer(int32)||
|&emsp;&emsp;visionModel||string||
|&emsp;&emsp;structuredOutputMode||string||
|&emsp;&emsp;apiKeyConfigured||boolean||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"providerCode": "",
		"providerName": "",
		"baseUrl": "",
		"defaultModel": "",
		"customHeaders": {},
		"enabled": true,
		"platformRateLimitPerMinute": 0,
		"taskRateLimitPerMinute": 0,
		"userRateLimitPerMinute": 0,
		"supportVision": true,
		"supportMultiImage": true,
		"maxImageCount": 0,
		"visionModel": "",
		"structuredOutputMode": "",
		"apiKeyConfigured": true,
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 测试 Provider


**接口地址**:`/api/v1/admin/llm-providers/{providerId}/test`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>ADMIN 测试全局 LLM Provider 连通性。</p>



**请求示例**:


```javascript
{
  "apiKey": "",
  "modelName": "",
  "customHeaders": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|providerId||path|true|integer(int64)||
|testLlmProviderRequest|TestLlmProviderRequest|body|true|TestLlmProviderRequest|TestLlmProviderRequest|
|&emsp;&emsp;apiKey|||false|string||
|&emsp;&emsp;modelName|||false|string||
|&emsp;&emsp;customHeaders|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmProviderTestResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmProviderTestResponse|LlmProviderTestResponse|
|&emsp;&emsp;success||boolean||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;message||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"success": true,
		"latencyMs": 0,
		"message": ""
	},
	"traceId": ""
}
```


## 启用 Provider


**接口地址**:`/api/v1/admin/llm-providers/{providerId}/enable`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>ADMIN 启用全局 LLM Provider。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|providerId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmProviderResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmProviderResponse|LlmProviderResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;providerCode||string||
|&emsp;&emsp;providerName||string||
|&emsp;&emsp;baseUrl||string||
|&emsp;&emsp;defaultModel||string||
|&emsp;&emsp;customHeaders||object||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;platformRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;taskRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;userRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;supportVision||boolean||
|&emsp;&emsp;supportMultiImage||boolean||
|&emsp;&emsp;maxImageCount||integer(int32)||
|&emsp;&emsp;visionModel||string||
|&emsp;&emsp;structuredOutputMode||string||
|&emsp;&emsp;apiKeyConfigured||boolean||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"providerCode": "",
		"providerName": "",
		"baseUrl": "",
		"defaultModel": "",
		"customHeaders": {},
		"enabled": true,
		"platformRateLimitPerMinute": 0,
		"taskRateLimitPerMinute": 0,
		"userRateLimitPerMinute": 0,
		"supportVision": true,
		"supportMultiImage": true,
		"maxImageCount": 0,
		"visionModel": "",
		"structuredOutputMode": "",
		"apiKeyConfigured": true,
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 停用 Provider


**接口地址**:`/api/v1/admin/llm-providers/{providerId}/disable`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>ADMIN 停用全局 LLM Provider。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|providerId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmProviderResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmProviderResponse|LlmProviderResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;providerCode||string||
|&emsp;&emsp;providerName||string||
|&emsp;&emsp;baseUrl||string||
|&emsp;&emsp;defaultModel||string||
|&emsp;&emsp;customHeaders||object||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;platformRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;taskRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;userRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;supportVision||boolean||
|&emsp;&emsp;supportMultiImage||boolean||
|&emsp;&emsp;maxImageCount||integer(int32)||
|&emsp;&emsp;visionModel||string||
|&emsp;&emsp;structuredOutputMode||string||
|&emsp;&emsp;apiKeyConfigured||boolean||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"providerCode": "",
		"providerName": "",
		"baseUrl": "",
		"defaultModel": "",
		"customHeaders": {},
		"enabled": true,
		"platformRateLimitPerMinute": 0,
		"taskRateLimitPerMinute": 0,
		"userRateLimitPerMinute": 0,
		"supportVision": true,
		"supportMultiImage": true,
		"maxImageCount": 0,
		"visionModel": "",
		"structuredOutputMode": "",
		"apiKeyConfigured": true,
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


# LLM 字段触发


## Owner 预览测试 LlmTrigger


**接口地址**:`/api/v1/tasks/{taskId}/llm-triggers/test`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>Owner 搭模板时用指定题目测试 LlmTrigger prompt 效果。</p>



**请求示例**:


```javascript
{
  "providerId": 0,
  "modelName": "",
  "promptTemplate": "",
  "targetFields": [],
  "datasetItemId": 0,
  "componentId": "summary",
  "currentAnswerJson": {},
  "userInstruction": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|llmTriggerRunRequest|LLM 字段触发请求。标注员点击组件后后端构建 LLM 上下文并发起调用。|body|true|LlmTriggerRunRequest|LlmTriggerRunRequest|
|&emsp;&emsp;providerId|旧版厂商 ID（标注员触发时忽略，保留用于旧客户端兼容）||false|integer(int64)||
|&emsp;&emsp;modelName|旧版模型名称（标注员触发时忽略，保留用于旧客户端兼容）||false|string||
|&emsp;&emsp;promptTemplate|旧版 Prompt 模板（标注员触发时忽略，保留用于旧客户端兼容）||false|string||
|&emsp;&emsp;targetFields|旧版目标字段（标注员触发时忽略，保留用于旧客户端兼容）||false|array|string|
|&emsp;&emsp;datasetItemId|题目 ID，Owner 预览测试时指定要测试的题目||false|integer(int64)||
|&emsp;&emsp;componentId|被点击的模板组件 ID||false|string||
|&emsp;&emsp;currentAnswerJson|当前草稿答案 JSON||false|object||
|&emsp;&emsp;userInstruction|标注员或 Owner 的额外补充指令||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmTriggerRunResponse|
|400|请求参数校验失败|ApiResponseLlmTriggerRunResponse|
|401|未认证|ApiResponseLlmTriggerRunResponse|
|403|权限不足|ApiResponseLlmTriggerRunResponse|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-401**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-403**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


## 标注时触发 LlmTrigger


**接口地址**:`/api/v1/assignments/{assignmentId}/llm-triggers`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>标注员在作答过程中点击按钮，前端全量传入模型和 prompt 参数。</p>



**请求示例**:


```javascript
{
  "providerId": 0,
  "modelName": "",
  "promptTemplate": "",
  "targetFields": [],
  "datasetItemId": 0,
  "componentId": "summary",
  "currentAnswerJson": {},
  "userInstruction": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|assignmentId||path|true|integer(int64)||
|llmTriggerRunRequest|LLM 字段触发请求。标注员点击组件后后端构建 LLM 上下文并发起调用。|body|true|LlmTriggerRunRequest|LlmTriggerRunRequest|
|&emsp;&emsp;providerId|旧版厂商 ID（标注员触发时忽略，保留用于旧客户端兼容）||false|integer(int64)||
|&emsp;&emsp;modelName|旧版模型名称（标注员触发时忽略，保留用于旧客户端兼容）||false|string||
|&emsp;&emsp;promptTemplate|旧版 Prompt 模板（标注员触发时忽略，保留用于旧客户端兼容）||false|string||
|&emsp;&emsp;targetFields|旧版目标字段（标注员触发时忽略，保留用于旧客户端兼容）||false|array|string|
|&emsp;&emsp;datasetItemId|题目 ID，Owner 预览测试时指定要测试的题目||false|integer(int64)||
|&emsp;&emsp;componentId|被点击的模板组件 ID||false|string||
|&emsp;&emsp;currentAnswerJson|当前草稿答案 JSON||false|object||
|&emsp;&emsp;userInstruction|标注员或 Owner 的额外补充指令||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmTriggerRunResponse|
|400|请求参数校验失败|ApiResponseLlmTriggerRunResponse|
|401|未认证|ApiResponseLlmTriggerRunResponse|
|403|权限不足|ApiResponseLlmTriggerRunResponse|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-401**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-403**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


## 查询 LlmTrigger 运行结果


**接口地址**:`/api/v1/llm/triggers/runs/{triggerRunId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>轮询异步 LlmTrigger 的运行状态和建议内容。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|triggerRunId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmTriggerRunResponse|
|400|请求参数校验失败|ApiResponseLlmTriggerRunResponse|
|401|未认证|ApiResponseLlmTriggerRunResponse|
|403|权限不足|ApiResponseLlmTriggerRunResponse|


**响应状态码-200**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-400**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-401**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


**响应状态码-403**:


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunResponse|LlmTriggerRunResponse|
|&emsp;&emsp;triggerRunId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;componentId||string||
|&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;patch||object||
|&emsp;&emsp;displayText||string||
|&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;confidence||number||
|&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;status||||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"triggerRunId": 0,
		"agentRunId": 0,
		"componentId": "",
		"suggestionJson": {},
		"patch": {},
		"displayText": "",
		"targetFields": [],
		"rawModelSummary": "",
		"confidence": 0,
		"warnings": [],
		"traceId": "",
		"status": {},
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


# 通知推送


## 标记单条已读


**接口地址**:`/api/v1/notifications/{id}/read`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|id||path|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 全部标记已读


**接口地址**:`/api/v1/notifications/read-all`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 历史通知列表


**接口地址**:`/api/v1/notifications`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|limit||query|false|integer(int32)||
|offset||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListNotificationEvent|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|NotificationEvent|
|&emsp;&emsp;id||string||
|&emsp;&emsp;type||string||
|&emsp;&emsp;userId||integer(int64)||
|&emsp;&emsp;title||string||
|&emsp;&emsp;body||string||
|&emsp;&emsp;data||object||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;read||boolean||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"id": "",
			"type": "",
			"userId": 0,
			"title": "",
			"body": "",
			"data": {},
			"createdAt": "",
			"read": true
		}
	],
	"traceId": ""
}
```


## 未读通知数量


**接口地址**:`/api/v1/notifications/unread-count`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:


**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseMapStringLong|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||object||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## SSE 连接


**接口地址**:`/api/v1/notifications/stream`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>建立 SSE 长连接接收实时通知。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|SseEmitter|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|timeout||integer(int64)|integer(int64)|


**响应示例**:
```javascript
{
	"timeout": 0
}
```


# 任务市场


## 任务市场列表


**接口地址**:`/api/v1/market/tasks`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询当前标注员可领取的已发布任务列表，支持按关键词和标签筛选。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|keyword||query|false|string||
|tag||query|false|string||
|status|可用值:DRAFT,PUBLISHED,PAUSED,ENDED|query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListTaskMarketResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|TaskMarketResponse|
|&emsp;&emsp;task|任务摘要|TaskSummaryResponse|TaskSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;taskId|任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;title|任务标题|string||
|&emsp;&emsp;&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;&emsp;&emsp;tags|任务标签|array|string|
|&emsp;&emsp;&emsp;&emsp;quota|任务配额|integer||
|&emsp;&emsp;&emsp;&emsp;claimedCount|已领取数|integer||
|&emsp;&emsp;&emsp;&emsp;overlapCount|每条数据需要的标注份数|integer||
|&emsp;&emsp;&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED|string||
|&emsp;&emsp;&emsp;&emsp;deadlineAt|截止时间|string||
|&emsp;&emsp;&emsp;&emsp;publishedAt|发布时间|string||
|&emsp;&emsp;&emsp;&emsp;endedAt|结束时间|string||
|&emsp;&emsp;&emsp;&emsp;createdAt|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|&emsp;&emsp;availableCount|当前可领取题目数|integer(int32)||
|&emsp;&emsp;currentUserClaimedCount|当前用户已领取数|integer(int32)||
|&emsp;&emsp;rewardSummary|奖励摘要|RewardSummaryResponse|RewardSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;rewardMode||string||
|&emsp;&emsp;&emsp;&emsp;unitReward||number||
|&emsp;&emsp;&emsp;&emsp;rewardCurrency||string||
|&emsp;&emsp;description|任务描述|string||
|&emsp;&emsp;instructionRichText|富文本标注说明|string||
|&emsp;&emsp;itemsPreview|题目摘要|array|ItemSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;itemId|题目 ID|integer||
|&emsp;&emsp;&emsp;&emsp;externalId|题目业务编号|string||
|&emsp;&emsp;&emsp;&emsp;itemJson|题目内容 JSON|string||
|&emsp;&emsp;&emsp;&emsp;metadataJson|题目元数据 JSON|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"task": {
				"taskId": 100,
				"title": "图像分类标注任务",
				"status": "PUBLISHED",
				"tags": "[\"image\",\"classification\"]",
				"quota": 100,
				"claimedCount": 45,
				"overlapCount": 1,
				"strategy": "FCFS",
				"deadlineAt": "2026-06-30T23:59:59",
				"publishedAt": "",
				"endedAt": "",
				"createdAt": "",
				"updatedAt": ""
			},
			"availableCount": 55,
			"currentUserClaimedCount": 3,
			"rewardSummary": {
				"rewardMode": "",
				"unitReward": 0,
				"rewardCurrency": ""
			},
			"description": "",
			"instructionRichText": "",
			"itemsPreview": [
				{
					"itemId": 100,
					"externalId": "q1",
					"itemJson": "",
					"metadataJson": ""
				}
			]
		}
	],
	"traceId": ""
}
```


## 市场任务详情


**接口地址**:`/api/v1/market/tasks/{taskId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查看已发布任务的详情和可领取题目预览列表。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|itemPage||query|false|integer(int32)||
|itemSize||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskMarketResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskMarketResponse|TaskMarketResponse|
|&emsp;&emsp;task|任务摘要|TaskSummaryResponse|TaskSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;taskId|任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;title|任务标题|string||
|&emsp;&emsp;&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;&emsp;&emsp;tags|任务标签|array|string|
|&emsp;&emsp;&emsp;&emsp;quota|任务配额|integer||
|&emsp;&emsp;&emsp;&emsp;claimedCount|已领取数|integer||
|&emsp;&emsp;&emsp;&emsp;overlapCount|每条数据需要的标注份数|integer||
|&emsp;&emsp;&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED|string||
|&emsp;&emsp;&emsp;&emsp;deadlineAt|截止时间|string||
|&emsp;&emsp;&emsp;&emsp;publishedAt|发布时间|string||
|&emsp;&emsp;&emsp;&emsp;endedAt|结束时间|string||
|&emsp;&emsp;&emsp;&emsp;createdAt|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|&emsp;&emsp;availableCount|当前可领取题目数|integer(int32)||
|&emsp;&emsp;currentUserClaimedCount|当前用户已领取数|integer(int32)||
|&emsp;&emsp;rewardSummary|奖励摘要|RewardSummaryResponse|RewardSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;rewardMode||string||
|&emsp;&emsp;&emsp;&emsp;unitReward||number||
|&emsp;&emsp;&emsp;&emsp;rewardCurrency||string||
|&emsp;&emsp;description|任务描述|string||
|&emsp;&emsp;instructionRichText|富文本标注说明|string||
|&emsp;&emsp;itemsPreview|题目摘要|array|ItemSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;itemId|题目 ID|integer||
|&emsp;&emsp;&emsp;&emsp;externalId|题目业务编号|string||
|&emsp;&emsp;&emsp;&emsp;itemJson|题目内容 JSON|string||
|&emsp;&emsp;&emsp;&emsp;metadataJson|题目元数据 JSON|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"task": {
			"taskId": 100,
			"title": "图像分类标注任务",
			"status": "PUBLISHED",
			"tags": "[\"image\",\"classification\"]",
			"quota": 100,
			"claimedCount": 45,
			"overlapCount": 1,
			"strategy": "FCFS",
			"deadlineAt": "2026-06-30T23:59:59",
			"publishedAt": "",
			"endedAt": "",
			"createdAt": "",
			"updatedAt": ""
		},
		"availableCount": 55,
		"currentUserClaimedCount": 3,
		"rewardSummary": {
			"rewardMode": "",
			"unitReward": 0,
			"rewardCurrency": ""
		},
		"description": "",
		"instructionRichText": "",
		"itemsPreview": [
			{
				"itemId": 100,
				"externalId": "q1",
				"itemJson": "",
				"metadataJson": ""
			}
		]
	},
	"traceId": ""
}
```


# AI 审核日志


## AI 审核日志列表


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-logs`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询指定任务下的所有 AI 审核结果，支持按状态、决策、时间范围筛选。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|任务 ID|path|true|integer(int64)||
|page||query|false|integer(int32)||
|pageSize||query|false|integer(int32)||
|status||query|false|string||
|decision||query|false|string||
|startTime||query|false|string(date-time)||
|endTime||query|false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewResultPageResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewResultPageResponse|AiReviewResultPageResponse|
|&emsp;&emsp;items||array|AiReviewResultResponse|
|&emsp;&emsp;&emsp;&emsp;id||integer||
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;providerId||integer||
|&emsp;&emsp;&emsp;&emsp;modelName||string||
|&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;dimensionScores||object||
|&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;confidence||string||
|&emsp;&emsp;&emsp;&emsp;flowAction||string||
|&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;updatedAt||string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"id": 0,
				"submissionId": 0,
				"agentRunId": 0,
				"providerId": 0,
				"modelName": "",
				"status": "",
				"decision": "",
				"averageScore": "",
				"dimensionScores": {},
				"riskFlags": "",
				"suggestion": "",
				"confidence": "",
				"flowAction": "",
				"promptMode": "",
				"degraded": true,
				"limitations": [],
				"errorCode": "",
				"errorMessage": "",
				"createdAt": "",
				"updatedAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


# 标注员提交记录


## 我的提交列表


**接口地址**:`/api/v1/labeler/submissions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>标注员查看自己的提交记录，支持按任务、提交状态、领取状态筛选，分页返回。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|按任务 ID 筛选|query|false|integer(int64)||
|submissionStatus|按提交状态筛选：AI_REVIEWING / PENDING_FINAL / APPROVED / REJECTED,可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|query|false|string||
|assignmentStatus|按领取状态筛选：CLAIMED / SUBMITTED / RETURNED / APPROVED,可用值:CLAIMED,DRAFTING,SUBMITTED,AI_RETURNED,RETURNED,APPROVED,CANCELLED|query|false|string||
|page|页码，从 1 开始|query|false|integer(int32)||
|size|每页条数，默认 20|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePageResponseLabelerSubmissionListItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponseLabelerSubmissionListItem|PageResponseLabelerSubmissionListItem|
|&emsp;&emsp;items||array|LabelerSubmissionListItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;assignmentId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;datasetItemId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;&emsp;&emsp;assignmentStatus|可用值:CLAIMED,DRAFTING,SUBMITTED,AI_RETURNED,RETURNED,APPROVED,CANCELLED|string||
|&emsp;&emsp;&emsp;&emsp;aiReviewStatus|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;&emsp;&emsp;reviewSummary||string||
|&emsp;&emsp;&emsp;&emsp;rejectReason||string||
|&emsp;&emsp;&emsp;&emsp;isGolden||boolean||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;updatedAt||string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"submissionId": 0,
				"assignmentId": 0,
				"taskId": 0,
				"datasetItemId": 0,
				"versionNo": 0,
				"submissionStatus": "",
				"assignmentStatus": "",
				"aiReviewStatus": "",
				"aiDecision": "",
				"reviewSummary": "",
				"rejectReason": "",
				"isGolden": true,
				"createdAt": "",
				"updatedAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


## 提交详情


**接口地址**:`/api/v1/labeler/submissions/{submissionId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查看单条提交的详细信息，包含答案内容、AI 审核结果、审核状态等。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId|提交 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLabelerSubmissionDetailResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LabelerSubmissionDetailResponse|LabelerSubmissionDetailResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;templateVersionId||integer(int64)||
|&emsp;&emsp;versionNo||integer(int32)||
|&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;assignmentStatus|可用值:CLAIMED,DRAFTING,SUBMITTED,AI_RETURNED,RETURNED,APPROVED,CANCELLED|string||
|&emsp;&emsp;itemJson||string||
|&emsp;&emsp;schemaJson||string||
|&emsp;&emsp;answerJson||string||
|&emsp;&emsp;aiReviewStatus|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;aiSuggestion||string||
|&emsp;&emsp;rejectReason||string||
|&emsp;&emsp;reviewRecords||array|ReviewRecordSummary|
|&emsp;&emsp;&emsp;&emsp;reviewRecordId||integer||
|&emsp;&emsp;&emsp;&emsp;action||string||
|&emsp;&emsp;&emsp;&emsp;reason||string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;versionHistory||array|VersionSummary|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;status|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;creatorName||string||
|&emsp;&emsp;canModify||boolean||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"assignmentId": 0,
		"taskId": 0,
		"datasetItemId": 0,
		"templateVersionId": 0,
		"versionNo": 0,
		"submissionStatus": "",
		"assignmentStatus": "",
		"itemJson": "",
		"schemaJson": "",
		"answerJson": "",
		"aiReviewStatus": "",
		"aiDecision": "",
		"aiSuggestion": "",
		"rejectReason": "",
		"reviewRecords": [
			{
				"reviewRecordId": 0,
				"action": "",
				"reason": "",
				"createdAt": ""
			}
		],
		"versionHistory": [
			{
				"submissionId": 0,
				"versionNo": 0,
				"status": "",
				"createdAt": "",
				"createdBy": 0,
				"creatorName": ""
			}
		],
		"canModify": true
	},
	"traceId": ""
}
```


# LLM 厂商


## 可用模型供应商列表


**接口地址**:`/api/v1/llm-providers`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>Owner 查询 ADMIN 已启用的 LLM Provider，仅返回前端可展示的安全配置信息。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListLlmProviderResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|LlmProviderResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;providerCode||string||
|&emsp;&emsp;providerName||string||
|&emsp;&emsp;baseUrl||string||
|&emsp;&emsp;defaultModel||string||
|&emsp;&emsp;customHeaders||object||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;platformRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;taskRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;userRateLimitPerMinute||integer(int32)||
|&emsp;&emsp;supportVision||boolean||
|&emsp;&emsp;supportMultiImage||boolean||
|&emsp;&emsp;maxImageCount||integer(int32)||
|&emsp;&emsp;visionModel||string||
|&emsp;&emsp;structuredOutputMode||string||
|&emsp;&emsp;apiKeyConfigured||boolean||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"id": 0,
			"providerCode": "",
			"providerName": "",
			"baseUrl": "",
			"defaultModel": "",
			"customHeaders": {},
			"enabled": true,
			"platformRateLimitPerMinute": 0,
			"taskRateLimitPerMinute": 0,
			"userRateLimitPerMinute": 0,
			"supportVision": true,
			"supportMultiImage": true,
			"maxImageCount": 0,
			"visionModel": "",
			"structuredOutputMode": "",
			"apiKeyConfigured": true,
			"createdBy": 0,
			"createdAt": "",
			"updatedAt": ""
		}
	],
	"traceId": ""
}
```


# Agent 运行记录


## Agent 运行详情


**接口地址**:`/api/v1/agent-runs/{agentRunId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据 agentRunId 查询单次 Agent 运行的完整信息，包括输入 Prompt 快照、LLM 输出、状态、耗时等。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|agentRunId|Agent 运行记录 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAgentRunDetailResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AgentRunDetailResponse|AgentRunDetailResponse|
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;agentType||string||
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;providerId||integer(int64)||
|&emsp;&emsp;modelName||string||
|&emsp;&emsp;promptVersion||string||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;inputSnapshot||object||
|&emsp;&emsp;outputSnapshot||object||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;queuedAt||string(date-time)||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;redacted||boolean||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"agentRunId": 0,
		"agentType": "",
		"submissionId": 0,
		"assignmentId": 0,
		"providerId": 0,
		"modelName": "",
		"promptVersion": "",
		"status": "",
		"inputSnapshot": {},
		"outputSnapshot": {},
		"errorMessage": "",
		"traceId": "",
		"latencyMs": 0,
		"queuedAt": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": "",
		"redacted": true
	},
	"traceId": ""
}
```


# 审计


## 审计日志列表


**接口地址**:`/api/v1/audit-logs`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>按业务类型和业务 ID 查询审计时间线。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|bizType||query|true|string||
|bizId||query|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListAuditLogResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|AuditLogResponse|
|&emsp;&emsp;auditLogId||integer(int64)||
|&emsp;&emsp;bizType||string||
|&emsp;&emsp;bizId||integer(int64)||
|&emsp;&emsp;actorType||string||
|&emsp;&emsp;actorId||integer(int64)||
|&emsp;&emsp;action||string||
|&emsp;&emsp;beforeJson||JsonNode|JsonNode|
|&emsp;&emsp;afterJson||JsonNode|JsonNode|
|&emsp;&emsp;traceId||string||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"auditLogId": 0,
			"bizType": "",
			"bizId": 0,
			"actorType": "",
			"actorId": 0,
			"action": "",
			"beforeJson": {},
			"afterJson": {},
			"traceId": "",
			"agentRunId": 0,
			"createdAt": ""
		}
	],
	"traceId": ""
}
```


# LLM 调用日志


## LLM 调用日志列表


**接口地址**:`/api/v1/tasks/{taskId}/llm-trigger-runs`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询指定任务下的所有 LLM 触发器运行记录，支持按状态、组件、时间范围筛选。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|任务 ID|path|true|integer(int64)||
|page||query|false|integer(int32)||
|pageSize||query|false|integer(int32)||
|status||query|false|string||
|componentId||query|false|string||
|startTime||query|false|string(date-time)||
|endTime||query|false|string(date-time)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseLlmTriggerRunPageResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||LlmTriggerRunPageResponse|LlmTriggerRunPageResponse|
|&emsp;&emsp;items||array|LlmTriggerRunResponse|
|&emsp;&emsp;&emsp;&emsp;triggerRunId||integer||
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;componentId||string||
|&emsp;&emsp;&emsp;&emsp;suggestionJson||object||
|&emsp;&emsp;&emsp;&emsp;patch||object||
|&emsp;&emsp;&emsp;&emsp;displayText||string||
|&emsp;&emsp;&emsp;&emsp;targetFields||array|string|
|&emsp;&emsp;&emsp;&emsp;rawModelSummary||string||
|&emsp;&emsp;&emsp;&emsp;confidence||number||
|&emsp;&emsp;&emsp;&emsp;warnings||array|string|
|&emsp;&emsp;&emsp;&emsp;traceId||string||
|&emsp;&emsp;&emsp;&emsp;status||||
|&emsp;&emsp;&emsp;&emsp;latencyMs||integer||
|&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"triggerRunId": 0,
				"agentRunId": 0,
				"componentId": "",
				"suggestionJson": {},
				"patch": {},
				"displayText": "",
				"targetFields": [],
				"rawModelSummary": "",
				"confidence": 0,
				"warnings": [],
				"traceId": "",
				"status": {},
				"latencyMs": 0,
				"errorCode": "",
				"errorMessage": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


# Owner 任务列表


## 我的任务列表（分页）


**接口地址**:`/api/v1/owner/tasks`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询当前 OWNER 用户创建的任务，支持按状态和关键词筛选。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|status|按任务状态筛选：DRAFT / PUBLISHED / PAUSED / ENDED|query|false|string||
|keyword|按标题或描述关键词搜索|query|false|string||
|page|页码，从 1 开始|query|false|integer(int32)||
|size|每页条数，默认 20，最大 100|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePageResponseTaskSummaryResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponseTaskSummaryResponse|PageResponseTaskSummaryResponse|
|&emsp;&emsp;items|任务摘要|array|TaskSummaryResponse|
|&emsp;&emsp;&emsp;&emsp;taskId|任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;title|任务标题|string||
|&emsp;&emsp;&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;&emsp;&emsp;tags|任务标签|array|string|
|&emsp;&emsp;&emsp;&emsp;quota|任务配额|integer||
|&emsp;&emsp;&emsp;&emsp;claimedCount|已领取数|integer||
|&emsp;&emsp;&emsp;&emsp;overlapCount|每条数据需要的标注份数|integer||
|&emsp;&emsp;&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED|string||
|&emsp;&emsp;&emsp;&emsp;deadlineAt|截止时间|string||
|&emsp;&emsp;&emsp;&emsp;publishedAt|发布时间|string||
|&emsp;&emsp;&emsp;&emsp;endedAt|结束时间|string||
|&emsp;&emsp;&emsp;&emsp;createdAt|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"taskId": 100,
				"title": "图像分类标注任务",
				"status": "PUBLISHED",
				"tags": "[\"image\",\"classification\"]",
				"quota": 100,
				"claimedCount": 45,
				"overlapCount": 1,
				"strategy": "FCFS",
				"deadlineAt": "2026-06-30T23:59:59",
				"publishedAt": "",
				"endedAt": "",
				"createdAt": "",
				"updatedAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


# 模板


## Fork 模板


**接口地址**:`/api/v1/templates/{templateId}/fork`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>基于已有模板创建新版本。</p>



**请求示例**:


```javascript
{
  "baseVersionId": 0,
  "schemaJson": {},
  "changeNote": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|templateId||path|true|integer(int64)||
|forkTemplateRequest|ForkTemplateRequest|body|true|ForkTemplateRequest|ForkTemplateRequest|
|&emsp;&emsp;baseVersionId|||false|integer(int64)||
|&emsp;&emsp;schemaJson|||false|object||
|&emsp;&emsp;changeNote|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateResponse|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"name": "",
		"currentVersionNo": 0,
		"currentVersion": {
			"versionId": 0,
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"versionNo": 0,
			"schemaJson": {},
			"publishedSnapshot": true,
			"state": "",
			"changeNote": "",
			"createdBy": 0,
			"createdAt": ""
		},
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 模板列表


**接口地址**:`/api/v1/tasks/{taskId}/templates`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询任务下的模板列表。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"name": "",
			"currentVersionNo": 0,
			"currentVersion": {
				"versionId": 0,
				"templateId": 0,
				"taskId": 0,
				"ownerId": 0,
				"versionNo": 0,
				"schemaJson": {},
				"publishedSnapshot": true,
				"state": "",
				"changeNote": "",
				"createdBy": 0,
				"createdAt": ""
			},
			"createdBy": 0,
			"createdAt": "",
			"updatedAt": ""
		}
	],
	"traceId": ""
}
```


## 创建模板


**接口地址**:`/api/v1/tasks/{taskId}/templates`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>为任务创建模板并生成首个版本。</p>



**请求示例**:


```javascript
{
  "name": "",
  "schemaJson": {},
  "changeNote": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|createTemplateRequest|CreateTemplateRequest|body|true|CreateTemplateRequest|CreateTemplateRequest|
|&emsp;&emsp;name|||true|string||
|&emsp;&emsp;schemaJson|||true|object||
|&emsp;&emsp;changeNote|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateResponse|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"name": "",
		"currentVersionNo": 0,
		"currentVersion": {
			"versionId": 0,
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"versionNo": 0,
			"schemaJson": {},
			"publishedSnapshot": true,
			"state": "",
			"changeNote": "",
			"createdBy": 0,
			"createdAt": ""
		},
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 校验答案 JSON


**接口地址**:`/api/v1/schema/validate-answer`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>按模板版本 Schema 校验答案 JSON，不修改业务数据。</p>



**请求示例**:


```javascript
{
  "schemaVersionId": 0,
  "answerJson": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|validateAnswerRequest|ValidateAnswerRequest|body|true|ValidateAnswerRequest|ValidateAnswerRequest|
|&emsp;&emsp;schemaVersionId|||true|integer(int64)||
|&emsp;&emsp;answerJson|||true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListSchemaValidationError|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|SchemaValidationError|
|&emsp;&emsp;path||string||
|&emsp;&emsp;errorCode||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"path": "",
			"errorCode": 0,
			"errorMessage": ""
		}
	],
	"traceId": ""
}
```


## OWNER 模板列表


**接口地址**:`/api/v1/owner/templates`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询当前 OWNER 的可复用模板列表。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"name": "",
			"currentVersionNo": 0,
			"currentVersion": {
				"versionId": 0,
				"templateId": 0,
				"taskId": 0,
				"ownerId": 0,
				"versionNo": 0,
				"schemaJson": {},
				"publishedSnapshot": true,
				"state": "",
				"changeNote": "",
				"createdBy": 0,
				"createdAt": ""
			},
			"createdBy": 0,
			"createdAt": "",
			"updatedAt": ""
		}
	],
	"traceId": ""
}
```


## 创建 OWNER 模板


**接口地址**:`/api/v1/owner/templates`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>创建当前 OWNER 可复用模板并生成首个版本。</p>



**请求示例**:


```javascript
{
  "name": "",
  "schemaJson": {},
  "changeNote": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createTemplateRequest|CreateTemplateRequest|body|true|CreateTemplateRequest|CreateTemplateRequest|
|&emsp;&emsp;name|||true|string||
|&emsp;&emsp;schemaJson|||true|object||
|&emsp;&emsp;changeNote|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateResponse|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"name": "",
		"currentVersionNo": 0,
		"currentVersion": {
			"versionId": 0,
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"versionNo": 0,
			"schemaJson": {},
			"publishedSnapshot": true,
			"state": "",
			"changeNote": "",
			"createdBy": 0,
			"createdAt": ""
		},
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 模板版本详情


**接口地址**:`/api/v1/template-versions/{versionId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定模板版本详情。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|versionId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateVersionResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;versionId||integer(int64)||
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;versionNo||integer(int32)||
|&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;changeNote||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"versionId": 0,
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"versionNo": 0,
		"schemaJson": {},
		"publishedSnapshot": true,
		"state": "",
		"changeNote": "",
		"createdBy": 0,
		"createdAt": ""
	},
	"traceId": ""
}
```


# 认证


## 更新个人信息


**接口地址**:`/api/v1/users/me/profile`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>更新当前用户的显示名称和邮箱。邮箱需全局唯一。</p>



**请求示例**:


```javascript
{
  "displayName": "",
  "email": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|updateProfileRequest|UpdateProfileRequest|body|true|UpdateProfileRequest|UpdateProfileRequest|
|&emsp;&emsp;displayName|||false|string||
|&emsp;&emsp;email|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 修改密码


**接口地址**:`/api/v1/users/me/password`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>校验旧密码后更新为新密码，成功后旧令牌失效需重新登录。</p>



**请求示例**:


```javascript
{
  "oldPassword": "",
  "newPassword": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|changePasswordRequest|ChangePasswordRequest|body|true|ChangePasswordRequest|ChangePasswordRequest|
|&emsp;&emsp;oldPassword|||true|string||
|&emsp;&emsp;newPassword|||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 用户注册


**接口地址**:`/api/v1/auth/register`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>创建普通用户，按 role 参数授予 LABELER 或 OWNER，并返回 accessToken 和 refreshToken。</p>



**请求示例**:


```javascript
{
  "username": "labeler",
  "email": "labeler@example.com",
  "password": "Password123",
  "role": "LABELER"
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|registerRequest|注册请求|body|true|RegisterRequest|RegisterRequest|
|&emsp;&emsp;username|用户名，最大 64 字符||true|string||
|&emsp;&emsp;email|邮箱地址||true|string||
|&emsp;&emsp;password|登录密码，8 到 128 字符||true|string(password)||
|&emsp;&emsp;role|注册身份，可选 LABELER、OWNER,可用值:LABELER,OWNER||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTokenResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TokenResponse|TokenResponse|
|&emsp;&emsp;accessToken|访问令牌，默认 120 分钟过期|string||
|&emsp;&emsp;refreshToken|刷新令牌，默认 14 天过期|string||
|&emsp;&emsp;tokenVersion|令牌版本，账号状态或角色变化后旧令牌失效|integer(int32)||
|&emsp;&emsp;role|用户角色,可用值:ADMIN,OWNER,LABELER,REVIEWER,SYSTEM_AGENT|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"accessToken": "eyJhbGciOiJIUzI1NiJ9...",
		"refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
		"tokenVersion": 1,
		"role": "LABELER"
	},
	"traceId": ""
}
```


## 刷新令牌


**接口地址**:`/api/v1/auth/refresh`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>使用有效 refreshToken 换取新的 accessToken 和 refreshToken。</p>



**请求示例**:


```javascript
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|refreshRequest|刷新令牌请求|body|true|RefreshRequest|RefreshRequest|
|&emsp;&emsp;refreshToken|登录或刷新接口返回的 refreshToken||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTokenResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TokenResponse|TokenResponse|
|&emsp;&emsp;accessToken|访问令牌，默认 120 分钟过期|string||
|&emsp;&emsp;refreshToken|刷新令牌，默认 14 天过期|string||
|&emsp;&emsp;tokenVersion|令牌版本，账号状态或角色变化后旧令牌失效|integer(int32)||
|&emsp;&emsp;role|用户角色,可用值:ADMIN,OWNER,LABELER,REVIEWER,SYSTEM_AGENT|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"accessToken": "eyJhbGciOiJIUzI1NiJ9...",
		"refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
		"tokenVersion": 1,
		"role": "LABELER"
	},
	"traceId": ""
}
```


## 用户登录


**接口地址**:`/api/v1/auth/login`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>支持用户名或邮箱登录。仅普通且启用登录的用户可以获取令牌。</p>



**请求示例**:


```javascript
{
  "account": "labeler",
  "password": "Password123"
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|loginRequest|登录请求|body|true|LoginRequest|LoginRequest|
|&emsp;&emsp;account|用户名或邮箱||true|string||
|&emsp;&emsp;password|登录密码||true|string(password)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTokenResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TokenResponse|TokenResponse|
|&emsp;&emsp;accessToken|访问令牌，默认 120 分钟过期|string||
|&emsp;&emsp;refreshToken|刷新令牌，默认 14 天过期|string||
|&emsp;&emsp;tokenVersion|令牌版本，账号状态或角色变化后旧令牌失效|integer(int32)||
|&emsp;&emsp;role|用户角色,可用值:ADMIN,OWNER,LABELER,REVIEWER,SYSTEM_AGENT|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"accessToken": "eyJhbGciOiJIUzI1NiJ9...",
		"refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
		"tokenVersion": 1,
		"role": "LABELER"
	},
	"traceId": ""
}
```


## 当前用户信息


**接口地址**:`/api/v1/users/me`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回当前认证用户的最小资料和角色集合。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseUserProfileResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||UserProfileResponse|UserProfileResponse|
|&emsp;&emsp;userId|用户 ID|integer(int64)||
|&emsp;&emsp;username|用户名|string||
|&emsp;&emsp;email|邮箱|string||
|&emsp;&emsp;role|用户角色,可用值:ADMIN,OWNER,LABELER,REVIEWER,SYSTEM_AGENT|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"userId": 10,
		"username": "labeler",
		"email": "labeler@example.com",
		"role": "LABELER"
	},
	"traceId": ""
}
```


# 导入


## 追加导入数据集


**接口地址**:`/api/v1/tasks/{taskId}/imports`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>从已上传文件创建追加导入任务。</p>



**请求示例**:


```javascript
{
  "fileId": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|datasetImportRequest|DatasetImportRequest|body|true|DatasetImportRequest|DatasetImportRequest|
|&emsp;&emsp;fileId|||true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseDatasetImportJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||DatasetImportJobResponse|DatasetImportJobResponse|
|&emsp;&emsp;jobId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;status||string||
|&emsp;&emsp;importMode||string||
|&emsp;&emsp;totalCount||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failedCount||integer(int32)||
|&emsp;&emsp;errorReportFileId||integer(int64)||
|&emsp;&emsp;errorReportUrl||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"jobId": 0,
		"taskId": 0,
		"status": "",
		"importMode": "",
		"totalCount": 0,
		"successCount": 0,
		"failedCount": 0,
		"errorReportFileId": 0,
		"errorReportUrl": "",
		"errorMessage": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 覆盖导入数据集


**接口地址**:`/api/v1/tasks/{taskId}/imports/overwrite`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>从已上传文件创建覆盖导入任务，仅允许草稿任务。</p>



**请求示例**:


```javascript
{
  "fileId": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|datasetImportRequest|DatasetImportRequest|body|true|DatasetImportRequest|DatasetImportRequest|
|&emsp;&emsp;fileId|||true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseDatasetImportJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||DatasetImportJobResponse|DatasetImportJobResponse|
|&emsp;&emsp;jobId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;status||string||
|&emsp;&emsp;importMode||string||
|&emsp;&emsp;totalCount||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failedCount||integer(int32)||
|&emsp;&emsp;errorReportFileId||integer(int64)||
|&emsp;&emsp;errorReportUrl||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"jobId": 0,
		"taskId": 0,
		"status": "",
		"importMode": "",
		"totalCount": 0,
		"successCount": 0,
		"failedCount": 0,
		"errorReportFileId": 0,
		"errorReportUrl": "",
		"errorMessage": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 导入任务详情


**接口地址**:`/api/v1/tasks/{taskId}/imports/{jobId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询导入任务状态和错误报告下载地址。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|jobId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseDatasetImportJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||DatasetImportJobResponse|DatasetImportJobResponse|
|&emsp;&emsp;jobId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;status||string||
|&emsp;&emsp;importMode||string||
|&emsp;&emsp;totalCount||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failedCount||integer(int32)||
|&emsp;&emsp;errorReportFileId||integer(int64)||
|&emsp;&emsp;errorReportUrl||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"jobId": 0,
		"taskId": 0,
		"status": "",
		"importMode": "",
		"totalCount": 0,
		"successCount": 0,
		"failedCount": 0,
		"errorReportFileId": 0,
		"errorReportUrl": "",
		"errorMessage": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


# 贡献与奖励


## 奖励流水


**接口地址**:`/api/v1/labeler/rewards/ledger`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询当前标注员的奖励收支明细。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|limit|Maximum number of rows, default 20|query|false|integer(int32)||
|offset|Pagination offset, default 0|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListRewardLedgerResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|RewardLedgerResponse|
|&emsp;&emsp;ledgerId|流水 ID|integer(int64)||
|&emsp;&emsp;taskId|所属任务 ID|integer(int64)||
|&emsp;&emsp;submissionId|关联提交 ID|integer(int64)||
|&emsp;&emsp;assignmentId|关联分配 ID|integer(int64)||
|&emsp;&emsp;amount|奖励金额|number||
|&emsp;&emsp;direction|资金方向：CREDIT（正向奖励）/ DEBIT（冲正扣除）,可用值:CREDIT,DEBIT|string||
|&emsp;&emsp;reason|操作原因或备注|string||
|&emsp;&emsp;sourceEventId|来源事件 ID，用于幂等去重|string||
|&emsp;&emsp;rewardType|奖励类型：SUBMISSION_APPROVED / REWARD_REVERSED 等|string||
|&emsp;&emsp;createdAt|流水创建时间|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"ledgerId": 500,
			"taskId": 10,
			"submissionId": 200,
			"assignmentId": 150,
			"amount": 2.5,
			"direction": "",
			"reason": "",
			"sourceEventId": "evt-abc123",
			"rewardType": "",
			"createdAt": ""
		}
	],
	"traceId": ""
}
```


## 贡献趋势


**接口地址**:`/api/v1/labeler/contribution/trend`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询当前标注员的每日贡献趋势数据。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|days|Number of days to query, default 7|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListDailyContributionPoint|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|DailyContributionPoint|
|&emsp;&emsp;statDate|统计日期|string(date)||
|&emsp;&emsp;submittedCount|当日提交数|integer(int32)||
|&emsp;&emsp;approvedCount|当日通过数|integer(int32)||
|&emsp;&emsp;rejectedCount|当日驳回数|integer(int32)||
|&emsp;&emsp;rewardAmount|当日获得奖励|number||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"statDate": "",
			"submittedCount": 0,
			"approvedCount": 0,
			"rejectedCount": 0,
			"rewardAmount": 0
		}
	],
	"traceId": ""
}
```


## 任务贡献统计


**接口地址**:`/api/v1/labeler/contribution/tasks`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>按任务查看当前标注员的贡献统计明细。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|limit|Maximum number of rows, default 20|query|false|integer(int32)||
|offset|Pagination offset, default 0|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListTaskContributionResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|TaskContributionResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;taskTitle|任务标题|string||
|&emsp;&emsp;submittedCount|该任务下已提交数|integer(int32)||
|&emsp;&emsp;approvedCount|该任务下已通过数|integer(int32)||
|&emsp;&emsp;rejectedCount|该任务下已驳回数|integer(int32)||
|&emsp;&emsp;totalReward|该任务下累计获得奖励|number||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"taskId": 10,
			"taskTitle": "图像分类标注",
			"submittedCount": 0,
			"approvedCount": 0,
			"rejectedCount": 0,
			"totalReward": 0
		}
	],
	"traceId": ""
}
```


## 贡献总览


**接口地址**:`/api/v1/labeler/contribution/overview`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询当前标注员的贡献统计数据总览。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseContributionOverviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ContributionOverviewResponse|ContributionOverviewResponse|
|&emsp;&emsp;labelerId|标注员用户 ID|integer(int64)||
|&emsp;&emsp;claimedCount|已领取数量|integer(int32)||
|&emsp;&emsp;submittedCount|已提交数量（不含待审核）|integer(int32)||
|&emsp;&emsp;pendingReviewCount|待审核数量|integer(int32)||
|&emsp;&emsp;approvedCount|已通过数量|integer(int32)||
|&emsp;&emsp;rejectedCount|已驳回数量|integer(int32)||
|&emsp;&emsp;totalReward|累计获得奖励|number||
|&emsp;&emsp;approvalRate|通过率|number||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"labelerId": 100,
		"claimedCount": 0,
		"submittedCount": 0,
		"pendingReviewCount": 0,
		"approvedCount": 0,
		"rejectedCount": 0,
		"totalReward": 0,
		"approvalRate": 0
	},
	"traceId": ""
}
```


# 数据集


## 批量更新数据项


**接口地址**:`/api/v1/tasks/{taskId}/dataset/items/batch-update`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量更新任务数据项内容。</p>



**请求示例**:


```javascript
{
  "items": [
    {
      "itemId": 0,
      "itemJson": {},
      "metadataJson": {}
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|batchUpdateItemsRequest|BatchUpdateItemsRequest|body|true|BatchUpdateItemsRequest|BatchUpdateItemsRequest|
|&emsp;&emsp;items|||true|array|DatasetItemUpdateRequest|
|&emsp;&emsp;&emsp;&emsp;itemId|||true|integer||
|&emsp;&emsp;&emsp;&emsp;itemJson|||true|object||
|&emsp;&emsp;&emsp;&emsp;metadataJson|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListBatchItemResult|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|BatchItemResult|
|&emsp;&emsp;itemId||integer(int64)||
|&emsp;&emsp;externalId||string||
|&emsp;&emsp;success||boolean||
|&emsp;&emsp;errorCode||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"itemId": 0,
			"externalId": "",
			"success": true,
			"errorCode": 0,
			"errorMessage": ""
		}
	],
	"traceId": ""
}
```


## 批量删除数据项


**接口地址**:`/api/v1/tasks/{taskId}/dataset/items/batch-delete`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量软删除任务数据项。</p>



**请求示例**:


```javascript
{
  "itemIds": []
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|batchDeleteItemsRequest|BatchDeleteItemsRequest|body|true|BatchDeleteItemsRequest|BatchDeleteItemsRequest|
|&emsp;&emsp;itemIds|||true|array|integer(int64)|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListBatchItemResult|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|BatchItemResult|
|&emsp;&emsp;itemId||integer(int64)||
|&emsp;&emsp;externalId||string||
|&emsp;&emsp;success||boolean||
|&emsp;&emsp;errorCode||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"itemId": 0,
			"externalId": "",
			"success": true,
			"errorCode": 0,
			"errorMessage": ""
		}
	],
	"traceId": ""
}
```


## 批量追加数据项


**接口地址**:`/api/v1/tasks/{taskId}/dataset/items/batch-append`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>向任务数据集追加多个数据项。</p>



**请求示例**:


```javascript
{
  "items": [
    {
      "externalId": "",
      "itemJson": {},
      "metadataJson": {}
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|batchAppendItemsRequest|BatchAppendItemsRequest|body|true|BatchAppendItemsRequest|BatchAppendItemsRequest|
|&emsp;&emsp;items|||true|array|DatasetItemAppendRequest|
|&emsp;&emsp;&emsp;&emsp;externalId|||true|string||
|&emsp;&emsp;&emsp;&emsp;itemJson|||true|object||
|&emsp;&emsp;&emsp;&emsp;metadataJson|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListBatchItemResult|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|BatchItemResult|
|&emsp;&emsp;itemId||integer(int64)||
|&emsp;&emsp;externalId||string||
|&emsp;&emsp;success||boolean||
|&emsp;&emsp;errorCode||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"itemId": 0,
			"externalId": "",
			"success": true,
			"errorCode": 0,
			"errorMessage": ""
		}
	],
	"traceId": ""
}
```


## 批量追加 JSON 数据项


**接口地址**:`/api/v1/tasks/{taskId}/dataset/items/batch-append-json`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>前端直接提交 externalId、itemJson 和 metadataJson，并追加到任务数据集。</p>



**请求示例**:


```javascript
{
  "items": [
    {
      "externalId": "",
      "itemJson": {},
      "metadataJson": {}
    }
  ]
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|batchAppendJsonItemsRequest|BatchAppendJsonItemsRequest|body|true|BatchAppendJsonItemsRequest|BatchAppendJsonItemsRequest|
|&emsp;&emsp;items|||true|array|DatasetItemAppendRequest|
|&emsp;&emsp;&emsp;&emsp;externalId|||true|string||
|&emsp;&emsp;&emsp;&emsp;itemJson|||true|object||
|&emsp;&emsp;&emsp;&emsp;metadataJson|||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListBatchItemResult|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|BatchItemResult|
|&emsp;&emsp;itemId||integer(int64)||
|&emsp;&emsp;externalId||string||
|&emsp;&emsp;success||boolean||
|&emsp;&emsp;errorCode||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"itemId": 0,
			"externalId": "",
			"success": true,
			"errorCode": 0,
			"errorMessage": ""
		}
	],
	"traceId": ""
}
```


## 数据项列表


**接口地址**:`/api/v1/tasks/{taskId}/dataset/items`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询任务下未删除的数据项。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|page||query|false|integer(int32)||
|pageSize||query|false|integer(int32)||
|externalId||query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePageResponseItemResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponseItemResponse|PageResponseItemResponse|
|&emsp;&emsp;items|题目详情|array|ItemResponse|
|&emsp;&emsp;&emsp;&emsp;itemId|题目 ID|integer||
|&emsp;&emsp;&emsp;&emsp;taskId|所属任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;externalId|题目业务编号|string||
|&emsp;&emsp;&emsp;&emsp;itemJson|题目内容 JSON|JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;metadataJson|题目元数据 JSON|JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;assignedCount|已分配数|integer||
|&emsp;&emsp;&emsp;&emsp;submittedCount|已提交数|integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount|已通过数|integer||
|&emsp;&emsp;&emsp;&emsp;itemStatus|题目状态,可用值:UNCLAIMED,CLAIMED,DRAFT,SUBMITTED,RETURNED,APPROVED|string||
|&emsp;&emsp;&emsp;&emsp;labelerId|当前有效标注员 ID|integer||
|&emsp;&emsp;&emsp;&emsp;createdAt|创建时间|string||
|&emsp;&emsp;&emsp;&emsp;updatedAt|更新时间|string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"itemId": 100,
				"taskId": 10,
				"externalId": "q1",
				"itemJson": {},
				"metadataJson": {},
				"assignedCount": 1,
				"submittedCount": 1,
				"approvedCount": 1,
				"itemStatus": "UNCLAIMED",
				"labelerId": 0,
				"createdAt": "",
				"updatedAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


# Reviewer 角色数据看板


## Reviewer 看板总览


**接口地址**:`/api/v1/reviewer/dashboard/overview`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回当前审核员可见审核队列、趋势和 AI 复核摘要</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|range||query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewerDashboardOverviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewerDashboardOverviewResponse|ReviewerDashboardOverviewResponse|
|&emsp;&emsp;range||string||
|&emsp;&emsp;queueSummary||QueueSummary|QueueSummary|
|&emsp;&emsp;&emsp;&emsp;pendingCount||integer||
|&emsp;&emsp;&emsp;&emsp;overduePendingCount||integer||
|&emsp;&emsp;&emsp;&emsp;manualRequiredCount||integer||
|&emsp;&emsp;&emsp;&emsp;conflictRequiredCount||integer||
|&emsp;&emsp;kpis||ReviewerKpis|ReviewerKpis|
|&emsp;&emsp;&emsp;&emsp;todayReviewedCount||integer||
|&emsp;&emsp;&emsp;&emsp;totalApproved||integer||
|&emsp;&emsp;&emsp;&emsp;totalRejected||integer||
|&emsp;&emsp;&emsp;&emsp;approvalRate||number||
|&emsp;&emsp;&emsp;&emsp;aiAttentionCount||integer||
|&emsp;&emsp;reviewTrend||array|ReviewTrendPoint|
|&emsp;&emsp;&emsp;&emsp;date||string||
|&emsp;&emsp;&emsp;&emsp;reviewedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount||integer||
|&emsp;&emsp;&emsp;&emsp;rejectedCount||integer||
|&emsp;&emsp;aiReviewSummary||AiReviewSummary|AiReviewSummary|
|&emsp;&emsp;&emsp;&emsp;aiReviewResultId||integer||
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;attentionItems||array|AttentionItem|
|&emsp;&emsp;&emsp;&emsp;reviewId||integer||
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;taskTitle||string||
|&emsp;&emsp;&emsp;&emsp;type||string||
|&emsp;&emsp;&emsp;&emsp;level|可用值:INFO,WARNING|string||
|&emsp;&emsp;&emsp;&emsp;description||string||
|&emsp;&emsp;&emsp;&emsp;targetPath||string||
|&emsp;&emsp;recentReviewed||array|RecentReviewed|
|&emsp;&emsp;&emsp;&emsp;reviewId||integer||
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;taskTitle||string||
|&emsp;&emsp;&emsp;&emsp;labelerName||string||
|&emsp;&emsp;&emsp;&emsp;result||string||
|&emsp;&emsp;&emsp;&emsp;reviewedAt||string||
|&emsp;&emsp;generatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"range": "",
		"queueSummary": {
			"pendingCount": 0,
			"overduePendingCount": 0,
			"manualRequiredCount": 0,
			"conflictRequiredCount": 0
		},
		"kpis": {
			"todayReviewedCount": 0,
			"totalApproved": 0,
			"totalRejected": 0,
			"approvalRate": 0,
			"aiAttentionCount": 0
		},
		"reviewTrend": [
			{
				"date": "",
				"reviewedCount": 0,
				"approvedCount": 0,
				"rejectedCount": 0
			}
		],
		"aiReviewSummary": {
			"aiReviewResultId": 0,
			"agentRunId": 0,
			"status": "",
			"decision": "",
			"averageScore": "",
			"riskFlags": "",
			"suggestion": "",
			"errorCode": "",
			"promptMode": "",
			"degraded": true,
			"limitations": ""
		},
		"attentionItems": [
			{
				"reviewId": 0,
				"submissionId": 0,
				"taskId": 0,
				"taskTitle": "",
				"type": "",
				"level": "",
				"description": "",
				"targetPath": ""
			}
		],
		"recentReviewed": [
			{
				"reviewId": 0,
				"submissionId": 0,
				"taskTitle": "",
				"labelerName": "",
				"result": "",
				"reviewedAt": ""
			}
		],
		"generatedAt": ""
	},
	"traceId": ""
}
```


# 模板


## Fork 模板


**接口地址**:`/api/v1/templates/{templateId}/fork`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>基于已有模板创建新版本。</p>



**请求示例**:


```javascript
{
  "baseVersionId": 0,
  "schemaJson": {},
  "changeNote": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|templateId||path|true|integer(int64)||
|forkTemplateRequest|ForkTemplateRequest|body|true|ForkTemplateRequest|ForkTemplateRequest|
|&emsp;&emsp;baseVersionId|||false|integer(int64)||
|&emsp;&emsp;schemaJson|||false|object||
|&emsp;&emsp;changeNote|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateResponse|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"name": "",
		"currentVersionNo": 0,
		"currentVersion": {
			"versionId": 0,
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"versionNo": 0,
			"schemaJson": {},
			"publishedSnapshot": true,
			"state": "",
			"changeNote": "",
			"createdBy": 0,
			"createdAt": ""
		},
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 模板列表


**接口地址**:`/api/v1/tasks/{taskId}/templates`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询任务下的模板列表。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"name": "",
			"currentVersionNo": 0,
			"currentVersion": {
				"versionId": 0,
				"templateId": 0,
				"taskId": 0,
				"ownerId": 0,
				"versionNo": 0,
				"schemaJson": {},
				"publishedSnapshot": true,
				"state": "",
				"changeNote": "",
				"createdBy": 0,
				"createdAt": ""
			},
			"createdBy": 0,
			"createdAt": "",
			"updatedAt": ""
		}
	],
	"traceId": ""
}
```


## 创建模板


**接口地址**:`/api/v1/tasks/{taskId}/templates`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>为任务创建模板并生成首个版本。</p>



**请求示例**:


```javascript
{
  "name": "",
  "schemaJson": {},
  "changeNote": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|createTemplateRequest|CreateTemplateRequest|body|true|CreateTemplateRequest|CreateTemplateRequest|
|&emsp;&emsp;name|||true|string||
|&emsp;&emsp;schemaJson|||true|object||
|&emsp;&emsp;changeNote|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateResponse|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"name": "",
		"currentVersionNo": 0,
		"currentVersion": {
			"versionId": 0,
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"versionNo": 0,
			"schemaJson": {},
			"publishedSnapshot": true,
			"state": "",
			"changeNote": "",
			"createdBy": 0,
			"createdAt": ""
		},
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 校验答案 JSON


**接口地址**:`/api/v1/schema/validate-answer`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>按模板版本 Schema 校验答案 JSON，不修改业务数据。</p>



**请求示例**:


```javascript
{
  "schemaVersionId": 0,
  "answerJson": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|validateAnswerRequest|ValidateAnswerRequest|body|true|ValidateAnswerRequest|ValidateAnswerRequest|
|&emsp;&emsp;schemaVersionId|||true|integer(int64)||
|&emsp;&emsp;answerJson|||true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListSchemaValidationError|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|SchemaValidationError|
|&emsp;&emsp;path||string||
|&emsp;&emsp;errorCode||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"path": "",
			"errorCode": 0,
			"errorMessage": ""
		}
	],
	"traceId": ""
}
```


## OWNER 模板列表


**接口地址**:`/api/v1/owner/templates`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询当前 OWNER 的可复用模板列表。</p>



**请求参数**:


暂无


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"name": "",
			"currentVersionNo": 0,
			"currentVersion": {
				"versionId": 0,
				"templateId": 0,
				"taskId": 0,
				"ownerId": 0,
				"versionNo": 0,
				"schemaJson": {},
				"publishedSnapshot": true,
				"state": "",
				"changeNote": "",
				"createdBy": 0,
				"createdAt": ""
			},
			"createdBy": 0,
			"createdAt": "",
			"updatedAt": ""
		}
	],
	"traceId": ""
}
```


## 创建 OWNER 模板


**接口地址**:`/api/v1/owner/templates`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>创建当前 OWNER 可复用模板并生成首个版本。</p>



**请求示例**:


```javascript
{
  "name": "",
  "schemaJson": {},
  "changeNote": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createTemplateRequest|CreateTemplateRequest|body|true|CreateTemplateRequest|CreateTemplateRequest|
|&emsp;&emsp;name|||true|string||
|&emsp;&emsp;schemaJson|||true|object||
|&emsp;&emsp;changeNote|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateResponse|TemplateResponse|
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;name||string||
|&emsp;&emsp;currentVersionNo||integer(int32)||
|&emsp;&emsp;currentVersion||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;&emsp;&emsp;versionId||integer||
|&emsp;&emsp;&emsp;&emsp;templateId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;ownerId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;&emsp;&emsp;changeNote||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"name": "",
		"currentVersionNo": 0,
		"currentVersion": {
			"versionId": 0,
			"templateId": 0,
			"taskId": 0,
			"ownerId": 0,
			"versionNo": 0,
			"schemaJson": {},
			"publishedSnapshot": true,
			"state": "",
			"changeNote": "",
			"createdBy": 0,
			"createdAt": ""
		},
		"createdBy": 0,
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 模板版本详情


**接口地址**:`/api/v1/template-versions/{versionId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定模板版本详情。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|versionId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTemplateVersionResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TemplateVersionResponse|TemplateVersionResponse|
|&emsp;&emsp;versionId||integer(int64)||
|&emsp;&emsp;templateId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;ownerId||integer(int64)||
|&emsp;&emsp;versionNo||integer(int32)||
|&emsp;&emsp;schemaJson||JsonNode|JsonNode|
|&emsp;&emsp;publishedSnapshot||boolean||
|&emsp;&emsp;state|可用值:DRAFT,PUBLISHED_SNAPSHOT|string||
|&emsp;&emsp;changeNote||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"versionId": 0,
		"templateId": 0,
		"taskId": 0,
		"ownerId": 0,
		"versionNo": 0,
		"schemaJson": {},
		"publishedSnapshot": true,
		"state": "",
		"changeNote": "",
		"createdBy": 0,
		"createdAt": ""
	},
	"traceId": ""
}
```


# 审核


## 驳回提交


**接口地址**:`/api/v1/reviewer/submissions/{submissionId}/reject`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>审核驳回指定提交。</p>



**请求示例**:


```javascript
{
  "reason": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId||path|true|integer(int64)||
|rejectRequest|RejectRequest|body|true|RejectRequest|RejectRequest|
|&emsp;&emsp;reason|||true|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewActionResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewActionResponse|ReviewActionResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;reviewRecordId||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"submissionStatus": "",
		"reviewRecordId": 0
	},
	"traceId": ""
}
```


## 通过提交


**接口地址**:`/api/v1/reviewer/submissions/{submissionId}/approve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>审核通过指定提交。</p>



**请求示例**:


```javascript
{
  "reviewComment": "",
  "reviewLevel": 0,
  "revisedAnswerJson": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId||path|true|integer(int64)||
|approveRequest|ApproveRequest|body|true|ApproveRequest|ApproveRequest|
|&emsp;&emsp;reviewComment|||false|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||
|&emsp;&emsp;revisedAnswerJson|||false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewActionResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewActionResponse|ReviewActionResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;reviewRecordId||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"submissionStatus": "",
		"reviewRecordId": 0
	},
	"traceId": ""
}
```


## 批量驳回


**接口地址**:`/api/v1/reviewer/submissions/batch/reject`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量审核驳回提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reason": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchRejectRequest|BatchRejectRequest|body|true|BatchRejectRequest|BatchRejectRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reason|||true|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量转人工


**接口地址**:`/api/v1/reviewer/submissions/batch/mark-manual`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>将提交批量标记为需要人工处理。</p>



**请求示例**:


```javascript
{
  "submissionIds": []
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchMarkManualRequest|BatchMarkManualRequest|body|true|BatchMarkManualRequest|BatchMarkManualRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量通过


**接口地址**:`/api/v1/reviewer/submissions/batch/approve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>批量审核通过提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reviewComment": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchApproveRequest|BatchApproveRequest|body|true|BatchApproveRequest|BatchApproveRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reviewComment|||false|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量驳回


**接口地址**:`/api/v1/reviewer/submissions/batch-reject`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>兼容契约路径，批量审核驳回提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reason": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchRejectRequest|BatchRejectRequest|body|true|BatchRejectRequest|BatchRejectRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reason|||true|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量转人工


**接口地址**:`/api/v1/reviewer/submissions/batch-mark-manual`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>兼容契约路径，将提交批量标记为需要人工处理。</p>



**请求示例**:


```javascript
{
  "submissionIds": []
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchMarkManualRequest|BatchMarkManualRequest|body|true|BatchMarkManualRequest|BatchMarkManualRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 批量通过


**接口地址**:`/api/v1/reviewer/submissions/batch-approve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>兼容契约路径，批量审核通过提交。</p>



**请求示例**:


```javascript
{
  "submissionIds": [],
  "reviewComment": "",
  "reviewLevel": 0
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|batchApproveRequest|BatchApproveRequest|body|true|BatchApproveRequest|BatchApproveRequest|
|&emsp;&emsp;submissionIds|||true|array|integer(int64)|
|&emsp;&emsp;reviewComment|||false|string||
|&emsp;&emsp;reviewLevel|||false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseBatchReviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||BatchReviewResponse|BatchReviewResponse|
|&emsp;&emsp;total||integer(int32)||
|&emsp;&emsp;successCount||integer(int32)||
|&emsp;&emsp;failCount||integer(int32)||
|&emsp;&emsp;results||array|BatchReviewItemResult|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;success||boolean||
|&emsp;&emsp;&emsp;&emsp;error||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"total": 0,
		"successCount": 0,
		"failCount": 0,
		"results": [
			{
				"submissionId": 0,
				"success": true,
				"error": ""
			}
		]
	},
	"traceId": ""
}
```


## 解决冲突组


**接口地址**:`/api/v1/reviewer/conflict-groups/{groupId}/resolve`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>选择最终提交并完成冲突仲裁。</p>



**请求示例**:


```javascript
{
  "goldenSubmissionId": 0,
  "reason": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|groupId||path|true|integer(int64)||
|conflictResolveRequest|ConflictResolveRequest|body|true|ConflictResolveRequest|ConflictResolveRequest|
|&emsp;&emsp;goldenSubmissionId|||true|integer(int64)||
|&emsp;&emsp;reason|||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseConflictResolveResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ConflictResolveResponse|ConflictResolveResponse|
|&emsp;&emsp;groupId||integer(int64)||
|&emsp;&emsp;status|可用值:OPEN,RESOLVED|string||
|&emsp;&emsp;goldenSubmissionId||integer(int64)||
|&emsp;&emsp;reviewRecordId||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"groupId": 0,
		"status": "",
		"goldenSubmissionId": 0,
		"reviewRecordId": 0
	},
	"traceId": ""
}
```


## 待审提交列表


**接口地址**:`/api/v1/reviewer/submissions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询审核员可处理的提交列表，支持按任务、提交状态、AI 结论、冲突状态、审核级别筛选。 scope=CLAIMED 查询已领取的提交，scope=AVAILABLE 查询可领取的提交（任务广场），不传则查询全部。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|按任务 ID 筛选|query|false|integer(int64)||
|submissionStatus|按提交状态筛选|query|false|string||
|aiDecision|按 AI 结论筛选：PASS / REJECT / MANUAL_REVIEW|query|false|string||
|aiReviewStatus|按 AI 审核状态筛选|query|false|string||
|conflictStatus|按冲突状态筛选|query|false|string||
|reviewLevel|按审核级别筛选|query|false|integer(int32)||
|scope|查询范围：CLAIMED-已领取，AVAILABLE-可领取（任务广场），不传查全部|query|false|string||
|page|页码，从 1 开始|query|false|integer(int32)||
|size|每页条数，默认 20，最大 100|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePageResponseReviewerSubmissionListItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponseReviewerSubmissionListItem|PageResponseReviewerSubmissionListItem|
|&emsp;&emsp;items||array|ReviewerSubmissionListItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;datasetItemId||integer||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;&emsp;&emsp;aiReviewStatus|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;&emsp;&emsp;conflictStatus||string||
|&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;assignedReviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;updatedAt||string||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"submissionId": 0,
				"taskId": 0,
				"datasetItemId": 0,
				"labelerId": 0,
				"submissionStatus": "",
				"aiReviewStatus": "",
				"aiDecision": "",
				"conflictStatus": "",
				"reviewLevel": 0,
				"assignedReviewerId": 0,
				"createdAt": "",
				"updatedAt": ""
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


## 提交审核详情


**接口地址**:`/api/v1/reviewer/submissions/{submissionId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定提交的审核详情，包含标注答案、AI 评分、审核历史、冲突信息等。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId|提交 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewerSubmissionDetailResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewerSubmissionDetailResponse|ReviewerSubmissionDetailResponse|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;labelerId||integer(int64)||
|&emsp;&emsp;versionNo||integer(int32)||
|&emsp;&emsp;submissionStatus|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;answerJson||string||
|&emsp;&emsp;itemJson||string||
|&emsp;&emsp;templateVersionId||integer(int64)||
|&emsp;&emsp;schemaJson||string||
|&emsp;&emsp;aiReviewResult||AiReviewSummary|AiReviewSummary|
|&emsp;&emsp;&emsp;&emsp;aiReviewResultId||integer||
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;agentRunSummary||AgentRunSummary|AgentRunSummary|
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;agentType||string||
|&emsp;&emsp;&emsp;&emsp;modelName||string||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;startedAt||string||
|&emsp;&emsp;&emsp;&emsp;finishedAt||string||
|&emsp;&emsp;reviewRecords||array|ReviewRecordItem|
|&emsp;&emsp;&emsp;&emsp;reviewRecordId||integer||
|&emsp;&emsp;&emsp;&emsp;reviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;action||string||
|&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;reason||string||
|&emsp;&emsp;&emsp;&emsp;reviewComment||string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;versionHistory||array|VersionHistoryItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;status|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;&emsp;&emsp;answerHash||string||
|&emsp;&emsp;&emsp;&emsp;isGolden||boolean||
|&emsp;&emsp;&emsp;&emsp;submittedAt||string||
|&emsp;&emsp;&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;&emsp;&emsp;aiFlowAction||string||
|&emsp;&emsp;&emsp;&emsp;latestReviewAction||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;creatorName||string||
|&emsp;&emsp;latestPreAnnotation||LatestPreAnnotationSummary|LatestPreAnnotationSummary|
|&emsp;&emsp;&emsp;&emsp;preAnnotationId||integer||
|&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;suggestedAnswerJson||string||
|&emsp;&emsp;&emsp;&emsp;fieldSuggestions||string||
|&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;overallConfidence||string||
|&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;ignoredFields||string||
|&emsp;&emsp;&emsp;&emsp;mediaUnderstanding||string||
|&emsp;&emsp;&emsp;&emsp;finalDiff||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"submissionId": 0,
		"taskId": 0,
		"assignmentId": 0,
		"datasetItemId": 0,
		"labelerId": 0,
		"versionNo": 0,
		"submissionStatus": "",
		"answerJson": "",
		"itemJson": "",
		"templateVersionId": 0,
		"schemaJson": "",
		"aiReviewResult": {
			"aiReviewResultId": 0,
			"agentRunId": 0,
			"status": "",
			"decision": "",
			"averageScore": "",
			"riskFlags": "",
			"suggestion": "",
			"errorCode": "",
			"promptMode": "",
			"degraded": true,
			"limitations": ""
		},
		"agentRunSummary": {
			"agentRunId": 0,
			"agentType": "",
			"modelName": "",
			"status": "",
			"startedAt": "",
			"finishedAt": ""
		},
		"reviewRecords": [
			{
				"reviewRecordId": 0,
				"reviewerId": 0,
				"action": "",
				"reviewLevel": 0,
				"reason": "",
				"reviewComment": "",
				"createdAt": ""
			}
		],
		"versionHistory": [
			{
				"submissionId": 0,
				"versionNo": 0,
				"status": "",
				"answerHash": "",
				"isGolden": true,
				"submittedAt": "",
				"aiDecision": "",
				"aiFlowAction": "",
				"latestReviewAction": "",
				"createdBy": 0,
				"creatorName": ""
			}
		],
		"latestPreAnnotation": {
			"preAnnotationId": 0,
			"agentRunId": 0,
			"status": "",
			"suggestedAnswerJson": "",
			"fieldSuggestions": "",
			"riskFlags": "",
			"overallConfidence": "",
			"limitations": "",
			"promptMode": "",
			"degraded": true,
			"ignoredFields": "",
			"mediaUnderstanding": "",
			"finalDiff": ""
		}
	},
	"traceId": ""
}
```


## 冲突组列表


**接口地址**:`/api/v1/reviewer/conflict-groups`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询待解决冲突组。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|limit||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListConflictGroupResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|ConflictGroupResponse|
|&emsp;&emsp;groupId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;status|可用值:OPEN,RESOLVED|string||
|&emsp;&emsp;consensusScore||number||
|&emsp;&emsp;goldenSubmissionId||integer(int64)||
|&emsp;&emsp;candidateSubmissions||array|CandidateSubmissionItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;answerJson||string||
|&emsp;&emsp;&emsp;&emsp;aiReviewSummary||AiReviewSummary|AiReviewSummary|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;aiReviewResultId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;&emsp;&emsp;reviewRecords||array|ReviewRecordItem|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewRecordId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;action||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reason||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewComment||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;resolvedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"groupId": 0,
			"taskId": 0,
			"datasetItemId": 0,
			"status": "",
			"consensusScore": 0,
			"goldenSubmissionId": 0,
			"candidateSubmissions": [
				{
					"submissionId": 0,
					"labelerId": 0,
					"answerJson": "",
					"aiReviewSummary": {
						"aiReviewResultId": 0,
						"agentRunId": 0,
						"status": "",
						"decision": "",
						"averageScore": "",
						"riskFlags": "",
						"suggestion": "",
						"errorCode": "",
						"promptMode": "",
						"degraded": true,
						"limitations": ""
					},
					"reviewRecords": [
						{
							"reviewRecordId": 0,
							"reviewerId": 0,
							"action": "",
							"reviewLevel": 0,
							"reason": "",
							"reviewComment": "",
							"createdAt": ""
						}
					],
					"versionNo": 0
				}
			],
			"createdAt": "",
			"resolvedAt": ""
		}
	],
	"traceId": ""
}
```


## 冲突组详情


**接口地址**:`/api/v1/reviewer/conflict-groups/{groupId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询冲突组详情。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|groupId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseConflictGroupResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ConflictGroupResponse|ConflictGroupResponse|
|&emsp;&emsp;groupId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;status|可用值:OPEN,RESOLVED|string||
|&emsp;&emsp;consensusScore||number||
|&emsp;&emsp;goldenSubmissionId||integer(int64)||
|&emsp;&emsp;candidateSubmissions||array|CandidateSubmissionItem|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;labelerId||integer||
|&emsp;&emsp;&emsp;&emsp;answerJson||string||
|&emsp;&emsp;&emsp;&emsp;aiReviewSummary||AiReviewSummary|AiReviewSummary|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;aiReviewResultId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;agentRunId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;decision||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;averageScore||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;suggestion||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;errorCode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;promptMode||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;limitations||string||
|&emsp;&emsp;&emsp;&emsp;reviewRecords||array|ReviewRecordItem|
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewRecordId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;action||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reason||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;reviewComment||string||
|&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;resolvedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"groupId": 0,
		"taskId": 0,
		"datasetItemId": 0,
		"status": "",
		"consensusScore": 0,
		"goldenSubmissionId": 0,
		"candidateSubmissions": [
			{
				"submissionId": 0,
				"labelerId": 0,
				"answerJson": "",
				"aiReviewSummary": {
					"aiReviewResultId": 0,
					"agentRunId": 0,
					"status": "",
					"decision": "",
					"averageScore": "",
					"riskFlags": "",
					"suggestion": "",
					"errorCode": "",
					"promptMode": "",
					"degraded": true,
					"limitations": ""
				},
				"reviewRecords": [
					{
						"reviewRecordId": 0,
						"reviewerId": 0,
						"action": "",
						"reviewLevel": 0,
						"reason": "",
						"reviewComment": "",
						"createdAt": ""
					}
				],
				"versionNo": 0
			}
		],
		"createdAt": "",
		"resolvedAt": ""
	},
	"traceId": ""
}
```


# 提交追溯


## 版本历史


**接口地址**:`/api/v1/submissions/{submissionId}/versions`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定提交所属 assignment 的所有提交版本列表，按版本号排序。OWNER、REVIEWER、LABELER 均可查看。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId|提交 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListVersionHistoryItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|VersionHistoryItem|
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;versionNo||integer(int32)||
|&emsp;&emsp;status|可用值:SUBMITTED,AI_REVIEWING,PENDING_FINAL,APPROVED,REJECTED,SUPERSEDED|string||
|&emsp;&emsp;answerHash||string||
|&emsp;&emsp;isGolden||boolean||
|&emsp;&emsp;submittedAt||string(date-time)||
|&emsp;&emsp;aiDecision||string||
|&emsp;&emsp;aiFlowAction||string||
|&emsp;&emsp;latestReviewAction||string||
|&emsp;&emsp;createdBy||integer(int64)||
|&emsp;&emsp;creatorName||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"submissionId": 0,
			"versionNo": 0,
			"status": "",
			"answerHash": "",
			"isGolden": true,
			"submittedAt": "",
			"aiDecision": "",
			"aiFlowAction": "",
			"latestReviewAction": "",
			"createdBy": 0,
			"creatorName": ""
		}
	],
	"traceId": ""
}
```


## 答案 Diff 对比


**接口地址**:`/api/v1/submissions/{submissionId}/diff`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>对比指定提交与基准版本之间的答案差异，返回字段级别的变更详情。仅 OWNER 和 REVIEWER 可用。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId|提交 ID|path|true|integer(int64)||
|baseVersionNo|基准版本号|query|true|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAnswerDiffResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AnswerDiffResponse|AnswerDiffResponse|
|&emsp;&emsp;baseSubmissionId||integer(int64)||
|&emsp;&emsp;baseVersionNo||integer(int32)||
|&emsp;&emsp;targetSubmissionId||integer(int64)||
|&emsp;&emsp;targetVersionNo||integer(int32)||
|&emsp;&emsp;diffs||array|FieldDiff|
|&emsp;&emsp;&emsp;&emsp;field||string||
|&emsp;&emsp;&emsp;&emsp;before||||
|&emsp;&emsp;&emsp;&emsp;after||||
|&emsp;&emsp;&emsp;&emsp;changeType|可用值:ADDED,MODIFIED,REMOVED|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"baseSubmissionId": 0,
		"baseVersionNo": 0,
		"targetSubmissionId": 0,
		"targetVersionNo": 0,
		"diffs": [
			{
				"field": "",
				"before": {},
				"after": {},
				"changeType": ""
			}
		]
	},
	"traceId": ""
}
```


## 多版本对比


**接口地址**:`/api/v1/submissions/compare`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>传入多个提交 ID，返回按版本的字段级并排对比。所有 ID 必须属于同一 assignment。OWNER、REVIEWER、LABELER 可用。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|ids|提交 ID 列表，逗号分隔，例如 101,102,103|query|true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseMultiVersionCompareResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||MultiVersionCompareResponse|MultiVersionCompareResponse|
|&emsp;&emsp;versions||array|VersionInfo|
|&emsp;&emsp;&emsp;&emsp;submissionId||integer||
|&emsp;&emsp;&emsp;&emsp;versionNo||integer||
|&emsp;&emsp;&emsp;&emsp;submittedAt||string||
|&emsp;&emsp;&emsp;&emsp;createdBy||integer||
|&emsp;&emsp;&emsp;&emsp;creatorName||string||
|&emsp;&emsp;fields||array|FieldComparison|
|&emsp;&emsp;&emsp;&emsp;fieldPath||string||
|&emsp;&emsp;&emsp;&emsp;valuesByVersion||object||
|&emsp;&emsp;&emsp;&emsp;hasDifference||boolean||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"versions": [
			{
				"submissionId": 0,
				"versionNo": 0,
				"submittedAt": "",
				"createdBy": 0,
				"creatorName": ""
			}
		],
		"fields": [
			{
				"fieldPath": "",
				"valuesByVersion": {},
				"hasDifference": true
			}
		]
	},
	"traceId": ""
}
```


# AI 审核结果


## 手动重试 AI 审核


**接口地址**:`/api/v1/submissions/{submissionId}/ai-review/retry`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>审核员手动触发 AI 预审重试，适用于 AI 审核失败或需要重新评估的场景。每次重试产生新的 AgentRun 记录。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId|提交 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewResultResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewResultResponse|AiReviewResultResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;providerId||integer(int64)||
|&emsp;&emsp;modelName||string||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;decision||string||
|&emsp;&emsp;averageScore||string||
|&emsp;&emsp;dimensionScores||object||
|&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;suggestion||string||
|&emsp;&emsp;confidence||string||
|&emsp;&emsp;flowAction||string||
|&emsp;&emsp;promptMode||string||
|&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"submissionId": 0,
		"agentRunId": 0,
		"providerId": 0,
		"modelName": "",
		"status": "",
		"decision": "",
		"averageScore": "",
		"dimensionScores": {},
		"riskFlags": "",
		"suggestion": "",
		"confidence": "",
		"flowAction": "",
		"promptMode": "",
		"degraded": true,
		"limitations": [],
		"errorCode": "",
		"errorMessage": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


## 查询 AI 审核结果


**接口地址**:`/api/v1/submissions/{submissionId}/ai-review`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>获取指定提交的 AI 预审结果，包含各维度评分、结论、置信度、风险标记和原始 Prompt/响应。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId|提交 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewResultResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewResultResponse|AiReviewResultResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;providerId||integer(int64)||
|&emsp;&emsp;modelName||string||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;decision||string||
|&emsp;&emsp;averageScore||string||
|&emsp;&emsp;dimensionScores||object||
|&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;suggestion||string||
|&emsp;&emsp;confidence||string||
|&emsp;&emsp;flowAction||string||
|&emsp;&emsp;promptMode||string||
|&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"submissionId": 0,
		"agentRunId": 0,
		"providerId": 0,
		"modelName": "",
		"status": "",
		"decision": "",
		"averageScore": "",
		"dimensionScores": {},
		"riskFlags": "",
		"suggestion": "",
		"confidence": "",
		"flowAction": "",
		"promptMode": "",
		"degraded": true,
		"limitations": [],
		"errorCode": "",
		"errorMessage": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```


# 媒体处理


## 触发媒体处理


**接口地址**:`/api/v1/dataset-items/{itemId}/media/process`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>Owner 或管理员触发指定数据项的多媒体素材解析，生成可供 AI 审核和标注预览使用的媒体上下文。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|itemId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseMediaProcessingJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||MediaProcessingJobResponse|MediaProcessingJobResponse|
|&emsp;&emsp;jobId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;status||string||
|&emsp;&emsp;totalAssets||integer(int32)||
|&emsp;&emsp;processedAssets||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"jobId": 0,
		"datasetItemId": 0,
		"taskId": 0,
		"status": "",
		"totalAssets": 0,
		"processedAssets": 0,
		"errorMessage": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 查询媒体处理任务


**接口地址**:`/api/v1/media-processing/jobs/{jobId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询媒体处理任务的执行状态、错误信息和产物信息。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|jobId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseMediaProcessingJobResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||MediaProcessingJobResponse|MediaProcessingJobResponse|
|&emsp;&emsp;jobId||integer(int64)||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;status||string||
|&emsp;&emsp;totalAssets||integer(int32)||
|&emsp;&emsp;processedAssets||integer(int32)||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;startedAt||string(date-time)||
|&emsp;&emsp;finishedAt||string(date-time)||
|&emsp;&emsp;createdAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"jobId": 0,
		"datasetItemId": 0,
		"taskId": 0,
		"status": "",
		"totalAssets": 0,
		"processedAssets": 0,
		"errorMessage": "",
		"startedAt": "",
		"finishedAt": "",
		"createdAt": ""
	},
	"traceId": ""
}
```


## 查询媒体上下文


**接口地址**:`/api/v1/dataset-items/{itemId}/media-context`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定数据项已生成的多媒体上下文，标注员、审核员和 Owner 均可按权限查看。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|itemId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseMediaContextResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||MediaContextResponse|MediaContextResponse|
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;mediaType||string||
|&emsp;&emsp;processingStatus||string||
|&emsp;&emsp;context||object||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"datasetItemId": 0,
		"taskId": 0,
		"mediaType": "",
		"processingStatus": "",
		"context": {},
		"limitations": [],
		"updatedAt": ""
	},
	"traceId": ""
}
```


# Admin 审核分配


## 可分配审核任务


**接口地址**:`/api/v1/admin/review/tasks/assignable`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>按任务和审核级别聚合待终审提交，默认只返回尚未被整任务认领的任务级别。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||query|false|integer(int64)||
|keyword||query|false|string||
|reviewLevel||query|false|integer(int32)||
|includeClaimed||query|false|boolean||
|page||query|false|integer(int32)||
|size||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePageResponseAssignableReviewTaskResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponseAssignableReviewTaskResponse|PageResponseAssignableReviewTaskResponse|
|&emsp;&emsp;items||array|AssignableReviewTaskResponse|
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;title||string||
|&emsp;&emsp;&emsp;&emsp;status|可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;&emsp;&emsp;deadlineAt||string||
|&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;pendingCount||integer||
|&emsp;&emsp;&emsp;&emsp;claimed||boolean||
|&emsp;&emsp;&emsp;&emsp;claimedReviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;claimedReviewerName||string||
|&emsp;&emsp;&emsp;&emsp;available||boolean||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"taskId": 0,
				"title": "",
				"status": "",
				"deadlineAt": "",
				"reviewLevel": 0,
				"pendingCount": 0,
				"claimed": true,
				"claimedReviewerId": 0,
				"claimedReviewerName": "",
				"available": true
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


## 人工审核员任务进度


**接口地址**:`/api/v1/admin/review/reviewers/progress`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回审核员待审、今日已审、历史通过率和已认领任务。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|keyword||query|false|string||
|enabledOnly||query|false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListReviewerProgressResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|ReviewerProgressResponse|
|&emsp;&emsp;reviewerId||integer(int64)||
|&emsp;&emsp;username||string||
|&emsp;&emsp;email||string||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;loginEnabled||boolean||
|&emsp;&emsp;pendingCount||integer(int64)||
|&emsp;&emsp;todayReviewedCount||integer(int64)||
|&emsp;&emsp;totalReviewedCount||integer(int64)||
|&emsp;&emsp;approvalRate||number||
|&emsp;&emsp;claimedTaskCount||integer(int64)||
|&emsp;&emsp;claimedTasks||array|ClaimedReviewTaskResponse|
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;title||string||
|&emsp;&emsp;&emsp;&emsp;reviewLevel||integer||
|&emsp;&emsp;&emsp;&emsp;pendingCount||integer||
|&emsp;&emsp;&emsp;&emsp;claimedAt||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"reviewerId": 0,
			"username": "",
			"email": "",
			"enabled": true,
			"loginEnabled": true,
			"pendingCount": 0,
			"todayReviewedCount": 0,
			"totalReviewedCount": 0,
			"approvalRate": 0,
			"claimedTaskCount": 0,
			"claimedTasks": [
				{
					"taskId": 0,
					"title": "",
					"reviewLevel": 0,
					"pendingCount": 0,
					"claimedAt": ""
				}
			]
		}
	],
	"traceId": ""
}
```


## 可分配人工审核员


**接口地址**:`/api/v1/admin/review/reviewers/assignable`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回具备 REVIEWER 角色的人工审核员及其当前负载。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|keyword||query|false|string||
|enabledOnly||query|false|boolean||
|page||query|false|integer(int32)||
|size||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponsePageResponseAssignableReviewerResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||PageResponseAssignableReviewerResponse|PageResponseAssignableReviewerResponse|
|&emsp;&emsp;items||array|AssignableReviewerResponse|
|&emsp;&emsp;&emsp;&emsp;reviewerId||integer||
|&emsp;&emsp;&emsp;&emsp;username||string||
|&emsp;&emsp;&emsp;&emsp;email||string||
|&emsp;&emsp;&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;&emsp;&emsp;loginEnabled||boolean||
|&emsp;&emsp;&emsp;&emsp;pendingCount||integer||
|&emsp;&emsp;&emsp;&emsp;todayReviewedCount||integer||
|&emsp;&emsp;&emsp;&emsp;totalApprovedCount||integer||
|&emsp;&emsp;&emsp;&emsp;totalRejectedCount||integer||
|&emsp;&emsp;&emsp;&emsp;approvalRate||number||
|&emsp;&emsp;page||integer(int32)||
|&emsp;&emsp;pageSize||integer(int32)||
|&emsp;&emsp;total||integer(int64)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"items": [
			{
				"reviewerId": 0,
				"username": "",
				"email": "",
				"enabled": true,
				"loginEnabled": true,
				"pendingCount": 0,
				"todayReviewedCount": 0,
				"totalApprovedCount": 0,
				"totalRejectedCount": 0,
				"approvalRate": 0
			}
		],
		"page": 0,
		"pageSize": 0,
		"total": 0
	},
	"traceId": ""
}
```


# 管理端数据看板


## 管理端看板总览


**接口地址**:`/api/v1/admin/dashboard/overview`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回 ADMIN 首页需要的 KPI、趋势、排行和异常提醒</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|range||query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAdminDashboardOverviewResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AdminDashboardOverviewResponse|AdminDashboardOverviewResponse|
|&emsp;&emsp;range|统计周期编码，7d 表示近 7 天，30d 表示近 30 天|string||
|&emsp;&emsp;kpis|管理端看板 KPI 指标|AdminDashboardKpis|AdminDashboardKpis|
|&emsp;&emsp;&emsp;&emsp;activeTaskCount|活跃任务数，包含当前已发布任务以及周期内产生领取或提交的任务|integer||
|&emsp;&emsp;&emsp;&emsp;claimedCount|周期内标注员领取次数|integer||
|&emsp;&emsp;&emsp;&emsp;submittedCount|周期内有效提交数，不包含 SUPERSEDED 提交|integer||
|&emsp;&emsp;&emsp;&emsp;pendingReviewCount|当前待终审提交数|integer||
|&emsp;&emsp;&emsp;&emsp;approvalRate|周期内审核通过率，approved / (approved + rejected)，分母为 0 时返回 0|number||
|&emsp;&emsp;&emsp;&emsp;rejectionRate|周期内审核打回率，rejected / (approved + rejected)，分母为 0 时返回 0|number||
|&emsp;&emsp;&emsp;&emsp;rewardAmount|周期内正向奖励支出汇总金额|number||
|&emsp;&emsp;userSummary|管理端看板用户摘要|AdminDashboardUserSummary|AdminDashboardUserSummary|
|&emsp;&emsp;&emsp;&emsp;totalUserCount|非 SYSTEM 用户总数|integer||
|&emsp;&emsp;&emsp;&emsp;roleCounts|角色人数分布，固定包含 ADMIN、OWNER、LABELER、REVIEWER 四个 key|object||
|&emsp;&emsp;&emsp;&emsp;disabledUserCount|被禁用或禁止登录的非 SYSTEM 用户数|integer||
|&emsp;&emsp;&emsp;&emsp;newUserCount|统计周期内新增的非 SYSTEM 用户数|integer||
|&emsp;&emsp;trend|管理端看板单日趋势点|array|AdminDashboardTrendPoint|
|&emsp;&emsp;&emsp;&emsp;date|自然日日期|string||
|&emsp;&emsp;&emsp;&emsp;submittedCount|该自然日有效提交数|integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount|该自然日审核通过数|integer||
|&emsp;&emsp;&emsp;&emsp;rejectedCount|该自然日审核打回数|integer||
|&emsp;&emsp;&emsp;&emsp;rewardAmount|该自然日正向奖励支出金额|number||
|&emsp;&emsp;taskStatusDistribution|任务状态分布，固定包含 DRAFT、PUBLISHED、PAUSED、ENDED 四个 key|object||
|&emsp;&emsp;topLabelers|管理端看板标注员排行榜条目|array|AdminDashboardTopLabeler|
|&emsp;&emsp;&emsp;&emsp;labelerId|标注员用户 ID|integer||
|&emsp;&emsp;&emsp;&emsp;displayName|标注员展示名称，优先 displayName，缺失时使用 username|string||
|&emsp;&emsp;&emsp;&emsp;submittedCount|周期内该标注员有效提交数|integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount|周期内该标注员审核通过数|integer||
|&emsp;&emsp;&emsp;&emsp;rewardAmount|周期内该标注员获得的正向奖励金额|number||
|&emsp;&emsp;topTasks|管理端看板任务排行榜条目|array|AdminDashboardTopTask|
|&emsp;&emsp;&emsp;&emsp;taskId|任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;title|任务标题|string||
|&emsp;&emsp;&emsp;&emsp;submittedCount|周期内该任务有效提交数|integer||
|&emsp;&emsp;&emsp;&emsp;approvedCount|周期内该任务审核通过数|integer||
|&emsp;&emsp;&emsp;&emsp;rejectedCount|周期内该任务审核打回数|integer||
|&emsp;&emsp;alerts|管理端看板异常提醒|array|AdminDashboardAlert|
|&emsp;&emsp;&emsp;&emsp;type|提醒类型，用于前端区分提醒来源和展示图标,可用值:REVIEW_BACKLOG,HIGH_REJECTION_RATE_TASK,ZERO_SUBMISSION_ACTIVE_TASK,DISABLED_USER|string||
|&emsp;&emsp;&emsp;&emsp;level|提醒级别，用于前端区分展示强度,可用值:INFO,WARNING,CRITICAL|string||
|&emsp;&emsp;&emsp;&emsp;title|提醒标题|string||
|&emsp;&emsp;&emsp;&emsp;description|提醒说明文案|string||
|&emsp;&emsp;&emsp;&emsp;targetPath|前端可跳转的目标路径|string||
|&emsp;&emsp;generatedAt|本次看板数据生成时间|string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"range": "7d",
		"kpis": {
			"activeTaskCount": 12,
			"claimedCount": 340,
			"submittedCount": 286,
			"pendingReviewCount": 31,
			"approvalRate": 0.82,
			"rejectionRate": 0.18,
			"rewardAmount": 1280.5
		},
		"userSummary": {
			"totalUserCount": 126,
			"roleCounts": {},
			"disabledUserCount": 3,
			"newUserCount": 8
		},
		"trend": [
			{
				"date": "2026-06-01",
				"submittedCount": 42,
				"approvedCount": 35,
				"rejectedCount": 7,
				"rewardAmount": 188
			}
		],
		"taskStatusDistribution": {},
		"topLabelers": [
			{
				"labelerId": 20,
				"displayName": "labeler-a",
				"submittedCount": 46,
				"approvedCount": 39,
				"rewardAmount": 210
			}
		],
		"topTasks": [
			{
				"taskId": 1001,
				"title": "商品质检任务",
				"submittedCount": 120,
				"approvedCount": 98,
				"rejectedCount": 22
			}
		],
		"alerts": [
			{
				"type": "REVIEW_BACKLOG",
				"level": "WARNING",
				"title": "审核积压",
				"description": "当前有 31 条提交待审核",
				"targetPath": "/app/reviewer/queue"
			}
		],
		"generatedAt": "2026-06-03T21:30:00"
	},
	"traceId": ""
}
```


# 标注员工作台


## 放弃领取


**接口地址**:`/api/v1/labeler/assignments/{assignmentId}/cancel`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>标注员放弃已领取的 assignment，释放数据项回市场池。仅 CLAIMED/DRAFTING/RETURNED 状态可放弃。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|assignmentId|领取记录 ID|path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 我的 Assignment 列表


**接口地址**:`/api/v1/labeler/assignments`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>分页查询当前标注员的所有 assignment，支持按任务和状态筛选。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||query|false|integer(int64)||
|status||query|false|string||
|page||query|false|integer(int32)||
|size||query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListLabelerAssignmentListItem|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|LabelerAssignmentListItem|
|&emsp;&emsp;assignmentId||integer(int64)||
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;taskTitle||string||
|&emsp;&emsp;datasetItemId||integer(int64)||
|&emsp;&emsp;status|可用值:CLAIMED,DRAFTING,SUBMITTED,AI_RETURNED,RETURNED,APPROVED,CANCELLED|string||
|&emsp;&emsp;draftVersion||integer(int32)||
|&emsp;&emsp;claimedAt||string(date-time)||
|&emsp;&emsp;returnedAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"assignmentId": 0,
			"taskId": 0,
			"taskTitle": "",
			"datasetItemId": 0,
			"status": "",
			"draftVersion": 0,
			"claimedAt": "",
			"returnedAt": "",
			"updatedAt": ""
		}
	],
	"traceId": ""
}
```


# 审核领取


## 领取整任务


**接口地址**:`/api/v1/reviewer/tasks/{taskId}/claim`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>审核员领取某任务某审核级别的全部待审题目。一个 (任务,级别) 只能被一名审核员领取，领取后该级别下当前及后续进入待审池的提交都会自动归属给该审核员。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|任务 ID|path|true|integer(int64)||
|reviewLevel|审核级别，默认 1；多级审核任务可领取更高级别|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseReviewTaskClaimResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||ReviewTaskClaimResponse|ReviewTaskClaimResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;reviewLevel|审核级别|integer(int32)||
|&emsp;&emsp;claimedSubmissionCount|本次领取归属到该审核员名下的待审提交数|integer(int32)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 10,
		"reviewLevel": 1,
		"claimedSubmissionCount": 25
	},
	"traceId": ""
}
```


## 释放整任务领取


**接口地址**:`/api/v1/reviewer/tasks/{taskId}/claim`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>审核员释放已领取的任务级别。名下该级别仍待审的提交会回到未分配状态，可被重新领取。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId|任务 ID|path|true|integer(int64)||
|reviewLevel|审核级别，默认 1|query|false|integer(int32)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


# 用户管理


## 更新用户角色


**接口地址**:`/api/v1/admin/users/{userId}/roles`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>替换用户的单一角色并递增 tokenVersion，使已有令牌失效。</p>



**请求示例**:


```javascript
{
  "role": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|userId||path|true|integer(int64)||
|updateUserRolesRequest|UpdateUserRolesRequest|body|true|UpdateUserRolesRequest|UpdateUserRolesRequest|
|&emsp;&emsp;role|可用值:ADMIN,OWNER,LABELER,REVIEWER,SYSTEM_AGENT||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 启用用户


**接口地址**:`/api/v1/admin/users/{userId}/enable`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>启用账号并递增 tokenVersion。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|userId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 禁用用户


**接口地址**:`/api/v1/admin/users/{userId}/disable`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>禁用账号并递增 tokenVersion，使已有令牌失效。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|userId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## createReviewer


**接口地址**:`/api/v1/admin/users/reviewers`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:


**请求示例**:


```javascript
{
  "username": "",
  "email": "",
  "password": ""
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createReviewerRequest|CreateReviewerRequest|body|true|CreateReviewerRequest|CreateReviewerRequest|
|&emsp;&emsp;username|||true|string||
|&emsp;&emsp;email|||true|string||
|&emsp;&emsp;password|||true|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAdminUserResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AdminUserResponse|AdminUserResponse|
|&emsp;&emsp;userId||integer(int64)||
|&emsp;&emsp;username||string||
|&emsp;&emsp;email||string||
|&emsp;&emsp;userType|可用值:USER,SYSTEM|string||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;loginEnabled||boolean||
|&emsp;&emsp;tokenVersion||integer(int32)||
|&emsp;&emsp;role|可用值:ADMIN,OWNER,LABELER,REVIEWER,SYSTEM_AGENT|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"userId": 0,
		"username": "",
		"email": "",
		"userType": "",
		"enabled": true,
		"loginEnabled": true,
		"tokenVersion": 0,
		"role": ""
	},
	"traceId": ""
}
```


## 用户列表


**接口地址**:`/api/v1/admin/users`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询后台用户列表，默认排除系统用户。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|includeSystem||query|false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListAdminUserResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|AdminUserResponse|
|&emsp;&emsp;userId||integer(int64)||
|&emsp;&emsp;username||string||
|&emsp;&emsp;email||string||
|&emsp;&emsp;userType|可用值:USER,SYSTEM|string||
|&emsp;&emsp;enabled||boolean||
|&emsp;&emsp;loginEnabled||boolean||
|&emsp;&emsp;tokenVersion||integer(int32)||
|&emsp;&emsp;role|可用值:ADMIN,OWNER,LABELER,REVIEWER,SYSTEM_AGENT|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"userId": 0,
			"username": "",
			"email": "",
			"userType": "",
			"enabled": true,
			"loginEnabled": true,
			"tokenVersion": 0,
			"role": ""
		}
	],
	"traceId": ""
}
```


# 任务


## 任务详情


**接口地址**:`/api/v1/tasks/{taskId}`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询当前用户拥有的任务详情。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskResponse|TaskResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;title|任务标题|string||
|&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;tags|任务标签|array|string|
|&emsp;&emsp;quota|任务配额|integer(int32)||
|&emsp;&emsp;claimedCount|已领取数|integer(int32)||
|&emsp;&emsp;overlapCount|每条数据需要的标注份数|integer(int32)||
|&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED|string||
|&emsp;&emsp;deadlineAt|截止时间|string(date-time)||
|&emsp;&emsp;publishedAt|发布时间|string(date-time)||
|&emsp;&emsp;endedAt|结束时间|string(date-time)||
|&emsp;&emsp;createdAt|创建时间|string(date-time)||
|&emsp;&emsp;updatedAt|更新时间|string(date-time)||
|&emsp;&emsp;ownerId|任务所有者 ID|integer(int64)||
|&emsp;&emsp;description|任务描述|string||
|&emsp;&emsp;instructionRichText|富文本标注说明|string||
|&emsp;&emsp;maxClaimsPerLabeler|单人并发未完成上限（仅 QUOTA_GRAB 有效）|integer(int32)||
|&emsp;&emsp;publishedTemplateVersionId|已发布模板版本 ID|integer(int64)||
|&emsp;&emsp;aiReviewConfigId|AI 审核配置 ID|integer(int64)||
|&emsp;&emsp;reviewLevelCount|审核级别数|integer(int32)||
|&emsp;&emsp;rewardVisible|奖励是否可见|boolean||
|&emsp;&emsp;rewardRule|奖励规则响应|RewardRuleResponse|RewardRuleResponse|
|&emsp;&emsp;&emsp;&emsp;ruleId|规则记录 ID|integer||
|&emsp;&emsp;&emsp;&emsp;taskId|所属任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;effectiveVersion|规则版本号，每次保存递增|integer||
|&emsp;&emsp;&emsp;&emsp;rewardMode|奖励模式：APPROVED_ITEM（按通过条目计奖）|string||
|&emsp;&emsp;&emsp;&emsp;unitReward|单条奖励金额|number||
|&emsp;&emsp;&emsp;&emsp;rewardCurrency|奖励货币类型|string||
|&emsp;&emsp;&emsp;&emsp;rewardVisible|奖励是否对标注员可见|boolean||
|&emsp;&emsp;&emsp;&emsp;effectiveAt|规则生效时间|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人用户 ID|integer||
|&emsp;&emsp;&emsp;&emsp;createdAt|规则创建时间|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 100,
		"title": "图像分类标注任务",
		"status": "PUBLISHED",
		"tags": "[\"image\",\"classification\"]",
		"quota": 100,
		"claimedCount": 45,
		"overlapCount": 1,
		"strategy": "FCFS",
		"deadlineAt": "2026-06-30T23:59:59",
		"publishedAt": "",
		"endedAt": "",
		"createdAt": "",
		"updatedAt": "",
		"ownerId": 10,
		"description": "对商品图片进行类别标注",
		"instructionRichText": "",
		"maxClaimsPerLabeler": 10,
		"publishedTemplateVersionId": 20,
		"aiReviewConfigId": 30,
		"reviewLevelCount": 3,
		"rewardVisible": true,
		"rewardRule": {
			"ruleId": 100,
			"taskId": 10,
			"effectiveVersion": 3,
			"rewardMode": "APPROVED_ITEM",
			"unitReward": 2.5,
			"rewardCurrency": "POINT",
			"rewardVisible": true,
			"effectiveAt": "",
			"createdBy": 1,
			"createdAt": ""
		}
	},
	"traceId": ""
}
```


## 编辑草稿任务


**接口地址**:`/api/v1/tasks/{taskId}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>仅允许编辑 DRAFT 状态任务。</p>



**请求示例**:


```javascript
{
  "title": "图像分类标注任务",
  "description": "对商品图片进行类别标注",
  "instructionRichText": "",
  "tags": [
    "image",
    "classification"
  ],
  "quota": 100,
  "deadlineAt": "2026-06-30T23:59:59",
  "overlapCount": 1,
  "publishedTemplateVersionId": 20,
  "aiReviewConfigId": 30,
  "reviewLevelCount": 3,
  "strategy": "FCFS",
  "maxClaimsPerLabeler": 10,
  "rewardRule": {
    "rewardMode": "APPROVED_ITEM",
    "unitReward": 2.5,
    "rewardCurrency": "POINT",
    "rewardVisible": true
  }
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|updateTaskRequest|更新草稿任务请求|body|true|UpdateTaskRequest|UpdateTaskRequest|
|&emsp;&emsp;title|任务标题||true|string||
|&emsp;&emsp;description|任务描述||false|string||
|&emsp;&emsp;instructionRichText|富文本标注说明||false|string||
|&emsp;&emsp;tags|任务标签||false|array|string|
|&emsp;&emsp;quota|任务配额（FCFS/QUOTA_GRAB必填，ASSIGNED自动推导）||false|integer(int32)||
|&emsp;&emsp;deadlineAt|截止时间||true|string(date-time)||
|&emsp;&emsp;overlapCount|每条数据需要的标注份数，当前固定为 1||true|integer(int32)||
|&emsp;&emsp;publishedTemplateVersionId|已发布模板版本 ID||false|integer(int64)||
|&emsp;&emsp;aiReviewConfigId|AI 审核配置 ID||false|integer(int64)||
|&emsp;&emsp;reviewLevelCount|审核级别数（1=单级审核，2=初审+终审，3=初审+复审+终审）||false|integer(int32)||
|&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED||false|string||
|&emsp;&emsp;maxClaimsPerLabeler|单人并发未完成上限（仅 QUOTA_GRAB 有效）||false|integer(int32)||
|&emsp;&emsp;rewardRule|保存奖励规则请求||false|RewardRuleRequest|RewardRuleRequest|
|&emsp;&emsp;&emsp;&emsp;rewardMode|奖励模式，当前仅支持 APPROVED_ITEM（按通过条目计奖）||false|string||
|&emsp;&emsp;&emsp;&emsp;unitReward|单条奖励金额||true|number||
|&emsp;&emsp;&emsp;&emsp;rewardCurrency|奖励货币类型，默认 POINT（平台积分）||false|string||
|&emsp;&emsp;&emsp;&emsp;rewardVisible|奖励是否对标注员可见||false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskStatusResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskStatusResponse|TaskStatusResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 100,
		"status": "DRAFT"
	},
	"traceId": ""
}
```


## 删除草稿任务


**接口地址**:`/api/v1/tasks/{taskId}`


**请求方式**:`DELETE`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>仅允许删除 DRAFT 状态任务，已发布任务不可删除。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseVoid|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {},
	"traceId": ""
}
```


## 创建任务


**接口地址**:`/api/v1/tasks`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>创建草稿任务，归属当前 OWNER 用户。</p>



**请求示例**:


```javascript
{
  "title": "图像分类标注任务",
  "description": "对商品图片进行类别标注",
  "instructionRichText": "",
  "tags": [
    "image",
    "classification"
  ],
  "quota": 100,
  "deadlineAt": "2026-06-30T23:59:59",
  "overlapCount": 1,
  "publishedTemplateVersionId": 20,
  "aiReviewConfigId": 30,
  "aiProviderId": 0,
  "aiModelName": "",
  "aiPrompt": "",
  "aiScoringDimensions": [],
  "aiPassThreshold": 80,
  "aiManualReviewThreshold": 60,
  "aiReviewStrategy": "LIGHTWEIGHT",
  "strategy": "FCFS",
  "maxClaimsPerLabeler": 10,
  "reviewLevelCount": 3,
  "datasetFileId": 99,
  "rewardRule": {
    "rewardMode": "APPROVED_ITEM",
    "unitReward": 2.5,
    "rewardCurrency": "POINT",
    "rewardVisible": true
  }
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|createTaskRequest|创建任务请求|body|true|CreateTaskRequest|CreateTaskRequest|
|&emsp;&emsp;title|任务标题||true|string||
|&emsp;&emsp;description|任务描述||false|string||
|&emsp;&emsp;instructionRichText|富文本标注说明||false|string||
|&emsp;&emsp;tags|任务标签||false|array|string|
|&emsp;&emsp;quota|任务配额（FCFS/QUOTA_GRAB必填，ASSIGNED自动推导）||false|integer(int32)||
|&emsp;&emsp;deadlineAt|截止时间||true|string(date-time)||
|&emsp;&emsp;overlapCount|每条数据需要的标注份数，当前固定为 1||true|integer(int32)||
|&emsp;&emsp;publishedTemplateVersionId|已发布模板版本 ID||false|integer(int64)||
|&emsp;&emsp;aiReviewConfigId|AI 审核配置 ID（引用已创建的配置，与内联 aiPrompt 互斥）||false|integer(int64)||
|&emsp;&emsp;aiProviderId|AI 模型供应商 ID（内联创建 AI 配置时必填）||false|integer(int64)||
|&emsp;&emsp;aiModelName|AI 模型名称（可选，如提供则必须匹配 Provider defaultModel）||false|string||
|&emsp;&emsp;aiPrompt|AI 审核 Prompt 模板（内联创建 AI 配置时必填）||false|string||
|&emsp;&emsp;aiScoringDimensions|AI 评分维度列表（内联创建 AI 配置时必填）||false|array|string|
|&emsp;&emsp;aiPassThreshold|AI 通过阈值（0-100）||false|number||
|&emsp;&emsp;aiManualReviewThreshold|AI 人工复核阈值（0-100）||false|number||
|&emsp;&emsp;aiReviewStrategy|AI 审核策略:\nLIGHTWEIGHT — 单路 LLM（默认，兼容存量）\nPARALLEL_VOTE — 多模型并行投票（系统自动用当前模型复制满足票数）\nDEEP_DIMENSION — 维度专项模型 + 维度内投票\nAGENT_DEBATE — 多 Agent 辩论||false|string||
|&emsp;&emsp;strategy|领取策略,可用值:FCFS,QUOTA_GRAB,ASSIGNED||false|string||
|&emsp;&emsp;maxClaimsPerLabeler|单人并发未完成上限（仅 QUOTA_GRAB 有效）||false|integer(int32)||
|&emsp;&emsp;reviewLevelCount|审核级别数（1=单级审核，2=初审+终审，3=初审+复审+终审）||false|integer(int32)||
|&emsp;&emsp;datasetFileId|通过 /api/v1/files/upload 上传的数据集文件 ID||false|integer(int64)||
|&emsp;&emsp;rewardRule|保存奖励规则请求||false|RewardRuleRequest|RewardRuleRequest|
|&emsp;&emsp;&emsp;&emsp;rewardMode|奖励模式，当前仅支持 APPROVED_ITEM（按通过条目计奖）||false|string||
|&emsp;&emsp;&emsp;&emsp;unitReward|单条奖励金额||true|number||
|&emsp;&emsp;&emsp;&emsp;rewardCurrency|奖励货币类型，默认 POINT（平台积分）||false|string||
|&emsp;&emsp;&emsp;&emsp;rewardVisible|奖励是否对标注员可见||false|boolean||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseCreateTaskResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||CreateTaskResponse|CreateTaskResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|&emsp;&emsp;datasetImportJob|随任务创建的导入作业（未传 datasetFileId 时为 null）|DatasetImportJobResponse|DatasetImportJobResponse|
|&emsp;&emsp;&emsp;&emsp;jobId||integer||
|&emsp;&emsp;&emsp;&emsp;taskId||integer||
|&emsp;&emsp;&emsp;&emsp;status||string||
|&emsp;&emsp;&emsp;&emsp;importMode||string||
|&emsp;&emsp;&emsp;&emsp;totalCount||integer||
|&emsp;&emsp;&emsp;&emsp;successCount||integer||
|&emsp;&emsp;&emsp;&emsp;failedCount||integer||
|&emsp;&emsp;&emsp;&emsp;errorReportFileId||integer||
|&emsp;&emsp;&emsp;&emsp;errorReportUrl||string||
|&emsp;&emsp;&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;&emsp;&emsp;startedAt||string||
|&emsp;&emsp;&emsp;&emsp;finishedAt||string||
|&emsp;&emsp;&emsp;&emsp;createdAt||string||
|&emsp;&emsp;rewardRule|奖励规则响应|RewardRuleResponse|RewardRuleResponse|
|&emsp;&emsp;&emsp;&emsp;ruleId|规则记录 ID|integer||
|&emsp;&emsp;&emsp;&emsp;taskId|所属任务 ID|integer||
|&emsp;&emsp;&emsp;&emsp;effectiveVersion|规则版本号，每次保存递增|integer||
|&emsp;&emsp;&emsp;&emsp;rewardMode|奖励模式：APPROVED_ITEM（按通过条目计奖）|string||
|&emsp;&emsp;&emsp;&emsp;unitReward|单条奖励金额|number||
|&emsp;&emsp;&emsp;&emsp;rewardCurrency|奖励货币类型|string||
|&emsp;&emsp;&emsp;&emsp;rewardVisible|奖励是否对标注员可见|boolean||
|&emsp;&emsp;&emsp;&emsp;effectiveAt|规则生效时间|string||
|&emsp;&emsp;&emsp;&emsp;createdBy|创建人用户 ID|integer||
|&emsp;&emsp;&emsp;&emsp;createdAt|规则创建时间|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 100,
		"status": "DRAFT",
		"datasetImportJob": {
			"jobId": 0,
			"taskId": 0,
			"status": "",
			"importMode": "",
			"totalCount": 0,
			"successCount": 0,
			"failedCount": 0,
			"errorReportFileId": 0,
			"errorReportUrl": "",
			"errorMessage": "",
			"startedAt": "",
			"finishedAt": "",
			"createdAt": ""
		},
		"rewardRule": {
			"ruleId": 100,
			"taskId": 10,
			"effectiveVersion": 3,
			"rewardMode": "APPROVED_ITEM",
			"unitReward": 2.5,
			"rewardCurrency": "POINT",
			"rewardVisible": true,
			"effectiveAt": "",
			"createdBy": 1,
			"createdAt": ""
		}
	},
	"traceId": ""
}
```


## 恢复任务


**接口地址**:`/api/v1/tasks/{taskId}/resume`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>恢复已暂停的任务，标注员可重新领取标注工作。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskStatusResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskStatusResponse|TaskStatusResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 100,
		"status": "DRAFT"
	},
	"traceId": ""
}
```


## 发布任务


**接口地址**:`/api/v1/tasks/{taskId}/publish`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>发布草稿任务。校验所有前置条件（数据集、模板、AI配置、奖励规则）后，将任务状态从 DRAFT 转为 PUBLISHED。策略和配额发布后即冻结不可更改。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskStatusResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskStatusResponse|TaskStatusResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 100,
		"status": "DRAFT"
	},
	"traceId": ""
}
```


## 暂停任务


**接口地址**:`/api/v1/tasks/{taskId}/pause`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>暂停已发布的任务，标注员暂时无法继续领取新的标注工作。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskStatusResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskStatusResponse|TaskStatusResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 100,
		"status": "DRAFT"
	},
	"traceId": ""
}
```


## 结束任务


**接口地址**:`/api/v1/tasks/{taskId}/end`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>永久结束任务，从活跃分发和审核流程中移除。结束后标注员无法继续领取或提交。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskStatusResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskStatusResponse|TaskStatusResponse|
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;status|任务状态,可用值:DRAFT,PUBLISHED,PAUSED,ENDED|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 100,
		"status": "DRAFT"
	},
	"traceId": ""
}
```


## 任务统计


**接口地址**:`/api/v1/tasks/{taskId}/statistics`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询任务的提交统计数据，包含总题目数、已领取、已提交、通过、驳回、待审核数量和通过率。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseTaskStatisticsResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||TaskStatisticsResponse|TaskStatisticsResponse|
|&emsp;&emsp;taskId||integer(int64)||
|&emsp;&emsp;totalItems||integer(int32)||
|&emsp;&emsp;claimedCount||integer(int32)||
|&emsp;&emsp;submittedCount||integer(int32)||
|&emsp;&emsp;approvedCount||integer(int32)||
|&emsp;&emsp;rejectedCount||integer(int32)||
|&emsp;&emsp;pendingReviewCount||integer(int32)||
|&emsp;&emsp;passRate||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"taskId": 0,
		"totalItems": 0,
		"claimedCount": 0,
		"submittedCount": 0,
		"approvedCount": 0,
		"rejectedCount": 0,
		"pendingReviewCount": 0,
		"passRate": ""
	},
	"traceId": ""
}
```


## 任务标注员列表


**接口地址**:`/api/v1/tasks/{taskId}/labelers`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询任务下参与的标注员列表及其进度统计。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseListTaskLabelerResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||array|TaskLabelerResponse|
|&emsp;&emsp;labelerId||integer(int64)||
|&emsp;&emsp;username||string||
|&emsp;&emsp;displayName||string||
|&emsp;&emsp;claimedCount||integer(int32)||
|&emsp;&emsp;submittedCount||integer(int32)||
|&emsp;&emsp;approvedCount||integer(int32)||
|&emsp;&emsp;rejectedCount||integer(int32)||
|&emsp;&emsp;cancelledCount||integer(int32)||
|&emsp;&emsp;firstClaimedAt||string(date-time)||
|&emsp;&emsp;lastActivityAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": [
		{
			"labelerId": 0,
			"username": "",
			"displayName": "",
			"claimedCount": 0,
			"submittedCount": 0,
			"approvedCount": 0,
			"rejectedCount": 0,
			"cancelledCount": 0,
			"firstClaimedAt": "",
			"lastActivityAt": ""
		}
	],
	"traceId": ""
}
```


# 文件


## 上传文件


**接口地址**:`/api/v1/files/upload`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,multipart/form-data`


**响应数据类型**:`*/*`


**接口描述**:<p>上传文件到对象存储，并记录当前用户归属的文件元数据。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|businessType||query|true|string||
|file||query|true|file||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseFileUploadResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||FileUploadResponse|FileUploadResponse|
|&emsp;&emsp;fileId|文件 ID|integer(int64)||
|&emsp;&emsp;originalFilename|原始文件名|string||
|&emsp;&emsp;contentType|文件 MIME 类型|string||
|&emsp;&emsp;fileSize|文件大小，单位字节|integer(int64)||
|&emsp;&emsp;objectKey|对象存储路径|string||
|&emsp;&emsp;checksum|SHA-256 校验值|string||
|&emsp;&emsp;downloadUrl|下载地址或短期签名地址|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"fileId": 99,
		"originalFilename": "dataset.jsonl",
		"contentType": "application/x-ndjson",
		"fileSize": 1024,
		"objectKey": "uploads/dataset/file.jsonl",
		"checksum": "",
		"downloadUrl": ""
	},
	"traceId": ""
}
```


## 获取签名下载地址


**接口地址**:`/api/v1/files/{fileId}/signed-url`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>根据文件权限生成短期有效的下载地址。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|fileId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseSignedUrlResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||SignedUrlResponse|SignedUrlResponse|
|&emsp;&emsp;fileId|文件 ID|integer(int64)||
|&emsp;&emsp;downloadUrl|短期有效下载地址|string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"fileId": 99,
		"downloadUrl": ""
	},
	"traceId": ""
}
```


# AI 审核


## 更新 AI 审核配置


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs/{configId}`


**请求方式**:`PUT`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>更新指定 AI 审核配置。</p>



**请求示例**:


```javascript
{
  "providerId": 1,
  "modelName": "qwen-plus",
  "promptTemplate": "请评估标注结果的准确性和完整性",
  "scoringDimensions": [
    "准确性",
    "完整性",
    "安全性"
  ],
  "passThreshold": 80,
  "manualReviewThreshold": 60,
  "maxRetry": 3,
  "aiFlowPolicy": "MANUAL_FIRST",
  "allowAiDirectApprove": true,
  "allowAiDirectReject": true,
  "rejectThreshold": 40,
  "confidenceThreshold": 0.85,
  "riskFlagsForceManual": [],
  "multimodalEnabled": true,
  "degradationPenalty": 0.2,
  "visionDetail": "auto",
  "maxImagesPerRequest": 5,
  "allowAiDirectApproveWhenDegraded": true,
  "reviewStrategy": "LIGHTWEIGHT",
  "voteModels": [
    {
      "providerId": 1,
      "modelName": "qwen-plus"
    }
  ],
  "voteMinAgreement": 2,
  "dimensionReviewers": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|configId||path|true|integer(int64)||
|aiReviewConfigRequest|AI 审核配置请求|body|true|AiReviewConfigRequest|AiReviewConfigRequest|
|&emsp;&emsp;providerId|LLM 供应商 ID||true|integer(int64)||
|&emsp;&emsp;modelName|模型名称||true|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板（标注规则说明）||true|string||
|&emsp;&emsp;scoringDimensions|评分维度列表||true|array|string|
|&emsp;&emsp;passThreshold|通过阈值（0-100）||true|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值（0-100，低于此值打回）||true|number||
|&emsp;&emsp;maxRetry|最大重试次数（0-10）||false|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略: MANUAL_FIRST | AI_PASS_ONLY | AI_REJECT_ONLY | AI_PASS_AND_REJECT | ALWAYS_MANUAL||false|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过||false|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回||false|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值（0-100）||false|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值（0.00-1.00）||false|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记列表||false|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态（图片/视频输入）||false|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数（0.00-1.00）||false|number||
|&emsp;&emsp;visionDetail|视觉精度: auto | low | high||false|string||
|&emsp;&emsp;maxImagesPerRequest|单次请求最大图片数（0-20）||false|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否仍允许 AI 直接通过||false|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT(单路,默认) | PARALLEL_VOTE(多模型投票) | DEEP_DIMENSION(维度专项) | AGENT_DEBATE(辩论)||false|string||
|&emsp;&emsp;voteModels|投票模型列表, JSON[{providerId,modelName}]; 仅1个时自动复制满足最低票数||false|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数(1-10), 默认2||false|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射, JSON{dim:[{providerId,modelName}]}||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewConfigResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewConfigResponse|AiReviewConfigResponse|
|&emsp;&emsp;id|配置 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;providerId|LLM 供应商 ID|integer(int64)||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板|string||
|&emsp;&emsp;scoringDimensions|评分维度列表|array|string|
|&emsp;&emsp;passThreshold|通过阈值|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值|number||
|&emsp;&emsp;outputSchema|输出 JSON Schema|object||
|&emsp;&emsp;promptVersion|Prompt 版本号|string||
|&emsp;&emsp;maxRetry|最大重试次数|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数|number||
|&emsp;&emsp;visionDetail|视觉精度|string||
|&emsp;&emsp;maxImagesPerRequest|最大图片数|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否允许 AI 直接通过|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE|string||
|&emsp;&emsp;voteModels|投票模型列表|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射|object||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"taskId": 0,
		"providerId": 0,
		"modelName": "",
		"promptTemplate": "",
		"scoringDimensions": [],
		"passThreshold": 0,
		"manualReviewThreshold": 0,
		"outputSchema": {},
		"promptVersion": "",
		"maxRetry": 0,
		"aiFlowPolicy": "",
		"allowAiDirectApprove": true,
		"allowAiDirectReject": true,
		"rejectThreshold": 0,
		"confidenceThreshold": 0,
		"riskFlagsForceManual": [],
		"multimodalEnabled": true,
		"degradationPenalty": 0,
		"visionDetail": "",
		"maxImagesPerRequest": 0,
		"allowAiDirectApproveWhenDegraded": true,
		"reviewStrategy": "",
		"voteModels": [],
		"voteMinAgreement": 0,
		"dimensionReviewers": {}
	},
	"traceId": ""
}
```


## 获取 AI 审核配置


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询任务当前 AI 审核配置。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewConfigResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewConfigResponse|AiReviewConfigResponse|
|&emsp;&emsp;id|配置 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;providerId|LLM 供应商 ID|integer(int64)||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板|string||
|&emsp;&emsp;scoringDimensions|评分维度列表|array|string|
|&emsp;&emsp;passThreshold|通过阈值|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值|number||
|&emsp;&emsp;outputSchema|输出 JSON Schema|object||
|&emsp;&emsp;promptVersion|Prompt 版本号|string||
|&emsp;&emsp;maxRetry|最大重试次数|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数|number||
|&emsp;&emsp;visionDetail|视觉精度|string||
|&emsp;&emsp;maxImagesPerRequest|最大图片数|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否允许 AI 直接通过|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE|string||
|&emsp;&emsp;voteModels|投票模型列表|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射|object||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"taskId": 0,
		"providerId": 0,
		"modelName": "",
		"promptTemplate": "",
		"scoringDimensions": [],
		"passThreshold": 0,
		"manualReviewThreshold": 0,
		"outputSchema": {},
		"promptVersion": "",
		"maxRetry": 0,
		"aiFlowPolicy": "",
		"allowAiDirectApprove": true,
		"allowAiDirectReject": true,
		"rejectThreshold": 0,
		"confidenceThreshold": 0,
		"riskFlagsForceManual": [],
		"multimodalEnabled": true,
		"degradationPenalty": 0,
		"visionDetail": "",
		"maxImagesPerRequest": 0,
		"allowAiDirectApproveWhenDegraded": true,
		"reviewStrategy": "",
		"voteModels": [],
		"voteMinAgreement": 0,
		"dimensionReviewers": {}
	},
	"traceId": ""
}
```


## 保存 AI 审核配置


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>创建或保存任务 AI 审核配置。</p>



**请求示例**:


```javascript
{
  "providerId": 1,
  "modelName": "qwen-plus",
  "promptTemplate": "请评估标注结果的准确性和完整性",
  "scoringDimensions": [
    "准确性",
    "完整性",
    "安全性"
  ],
  "passThreshold": 80,
  "manualReviewThreshold": 60,
  "maxRetry": 3,
  "aiFlowPolicy": "MANUAL_FIRST",
  "allowAiDirectApprove": true,
  "allowAiDirectReject": true,
  "rejectThreshold": 40,
  "confidenceThreshold": 0.85,
  "riskFlagsForceManual": [],
  "multimodalEnabled": true,
  "degradationPenalty": 0.2,
  "visionDetail": "auto",
  "maxImagesPerRequest": 5,
  "allowAiDirectApproveWhenDegraded": true,
  "reviewStrategy": "LIGHTWEIGHT",
  "voteModels": [
    {
      "providerId": 1,
      "modelName": "qwen-plus"
    }
  ],
  "voteMinAgreement": 2,
  "dimensionReviewers": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|aiReviewConfigRequest|AI 审核配置请求|body|true|AiReviewConfigRequest|AiReviewConfigRequest|
|&emsp;&emsp;providerId|LLM 供应商 ID||true|integer(int64)||
|&emsp;&emsp;modelName|模型名称||true|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板（标注规则说明）||true|string||
|&emsp;&emsp;scoringDimensions|评分维度列表||true|array|string|
|&emsp;&emsp;passThreshold|通过阈值（0-100）||true|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值（0-100，低于此值打回）||true|number||
|&emsp;&emsp;maxRetry|最大重试次数（0-10）||false|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略: MANUAL_FIRST | AI_PASS_ONLY | AI_REJECT_ONLY | AI_PASS_AND_REJECT | ALWAYS_MANUAL||false|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过||false|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回||false|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值（0-100）||false|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值（0.00-1.00）||false|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记列表||false|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态（图片/视频输入）||false|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数（0.00-1.00）||false|number||
|&emsp;&emsp;visionDetail|视觉精度: auto | low | high||false|string||
|&emsp;&emsp;maxImagesPerRequest|单次请求最大图片数（0-20）||false|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否仍允许 AI 直接通过||false|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT(单路,默认) | PARALLEL_VOTE(多模型投票) | DEEP_DIMENSION(维度专项) | AGENT_DEBATE(辩论)||false|string||
|&emsp;&emsp;voteModels|投票模型列表, JSON[{providerId,modelName}]; 仅1个时自动复制满足最低票数||false|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数(1-10), 默认2||false|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射, JSON{dim:[{providerId,modelName}]}||false|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewConfigResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewConfigResponse|AiReviewConfigResponse|
|&emsp;&emsp;id|配置 ID|integer(int64)||
|&emsp;&emsp;taskId|任务 ID|integer(int64)||
|&emsp;&emsp;providerId|LLM 供应商 ID|integer(int64)||
|&emsp;&emsp;modelName|模型名称|string||
|&emsp;&emsp;promptTemplate|审核 Prompt 模板|string||
|&emsp;&emsp;scoringDimensions|评分维度列表|array|string|
|&emsp;&emsp;passThreshold|通过阈值|number||
|&emsp;&emsp;manualReviewThreshold|人工复核阈值|number||
|&emsp;&emsp;outputSchema|输出 JSON Schema|object||
|&emsp;&emsp;promptVersion|Prompt 版本号|string||
|&emsp;&emsp;maxRetry|最大重试次数|integer(int32)||
|&emsp;&emsp;aiFlowPolicy|AI 流转策略|string||
|&emsp;&emsp;allowAiDirectApprove|是否允许 AI 直接通过|boolean||
|&emsp;&emsp;allowAiDirectReject|是否允许 AI 直接打回|boolean||
|&emsp;&emsp;rejectThreshold|打回阈值|number||
|&emsp;&emsp;confidenceThreshold|置信度阈值|number||
|&emsp;&emsp;riskFlagsForceManual|强制转人工的风险标记|array|string|
|&emsp;&emsp;multimodalEnabled|是否启用多模态|boolean||
|&emsp;&emsp;degradationPenalty|多模态降级惩罚系数|number||
|&emsp;&emsp;visionDetail|视觉精度|string||
|&emsp;&emsp;maxImagesPerRequest|最大图片数|integer(int32)||
|&emsp;&emsp;allowAiDirectApproveWhenDegraded|降级时是否允许 AI 直接通过|boolean||
|&emsp;&emsp;reviewStrategy|审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE|string||
|&emsp;&emsp;voteModels|投票模型列表|array|object|
|&emsp;&emsp;voteMinAgreement|最少一致票数|integer(int32)||
|&emsp;&emsp;dimensionReviewers|深度模式维度→模型映射|object||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"taskId": 0,
		"providerId": 0,
		"modelName": "",
		"promptTemplate": "",
		"scoringDimensions": [],
		"passThreshold": 0,
		"manualReviewThreshold": 0,
		"outputSchema": {},
		"promptVersion": "",
		"maxRetry": 0,
		"aiFlowPolicy": "",
		"allowAiDirectApprove": true,
		"allowAiDirectReject": true,
		"rejectThreshold": 0,
		"confidenceThreshold": 0,
		"riskFlagsForceManual": [],
		"multimodalEnabled": true,
		"degradationPenalty": 0,
		"visionDetail": "",
		"maxImagesPerRequest": 0,
		"allowAiDirectApproveWhenDegraded": true,
		"reviewStrategy": "",
		"voteModels": [],
		"voteMinAgreement": 0,
		"dimensionReviewers": {}
	},
	"traceId": ""
}
```


## 测试 AI 审核提示词


**接口地址**:`/api/v1/tasks/{taskId}/ai-review-configs/{configId}/test`


**请求方式**:`POST`


**请求数据类型**:`application/x-www-form-urlencoded,application/json`


**响应数据类型**:`*/*`


**接口描述**:<p>用样例输入测试 AI 审核提示词和输出结构。</p>



**请求示例**:


```javascript
{
  "itemSnapshot": {},
  "answerJson": {}
}
```


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|taskId||path|true|integer(int64)||
|configId||path|true|integer(int64)||
|aiReviewPromptTestRequest|AiReviewPromptTestRequest|body|true|AiReviewPromptTestRequest|AiReviewPromptTestRequest|
|&emsp;&emsp;itemSnapshot|||true|object||
|&emsp;&emsp;answerJson|||true|object||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewPromptTestResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewPromptTestResponse|AiReviewPromptTestResponse|
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;status|可用值:SUCCESS,PROVIDER_UNAVAILABLE,PROVIDER_ERROR,RATE_LIMITED,TIMEOUT,INVALID_JSON|string||
|&emsp;&emsp;contentText||string||
|&emsp;&emsp;structuredJson||object||
|&emsp;&emsp;rawResponse||string||
|&emsp;&emsp;latencyMs||integer(int64)||
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"agentRunId": 0,
		"status": "",
		"contentText": "",
		"structuredJson": {},
		"rawResponse": "",
		"latencyMs": 0,
		"errorCode": "",
		"errorMessage": ""
	},
	"traceId": ""
}
```


## AI 审核结果


**接口地址**:`/api/v1/submissions/{submissionId}/ai-review-result`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>查询指定提交的 AI 审核结果。</p>



**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|submissionId||path|true|integer(int64)||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAiReviewResultResponse|


**响应参数**:


| 参数名称 | 参数说明 | 类型 | schema |
| -------- | -------- | ----- |----- | 
|code||integer(int32)|integer(int32)|
|message||string||
|data||AiReviewResultResponse|AiReviewResultResponse|
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;submissionId||integer(int64)||
|&emsp;&emsp;agentRunId||integer(int64)||
|&emsp;&emsp;providerId||integer(int64)||
|&emsp;&emsp;modelName||string||
|&emsp;&emsp;status|可用值:PENDING,RUNNING,SUCCESS,FAILED,RATE_LIMITED,MANUAL_REQUIRED|string||
|&emsp;&emsp;decision||string||
|&emsp;&emsp;averageScore||string||
|&emsp;&emsp;dimensionScores||object||
|&emsp;&emsp;riskFlags||string||
|&emsp;&emsp;suggestion||string||
|&emsp;&emsp;confidence||string||
|&emsp;&emsp;flowAction||string||
|&emsp;&emsp;promptMode||string||
|&emsp;&emsp;degraded||boolean||
|&emsp;&emsp;limitations||array|string|
|&emsp;&emsp;errorCode||string||
|&emsp;&emsp;errorMessage||string||
|&emsp;&emsp;createdAt||string(date-time)||
|&emsp;&emsp;updatedAt||string(date-time)||
|traceId||string||


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"id": 0,
		"submissionId": 0,
		"agentRunId": 0,
		"providerId": 0,
		"modelName": "",
		"status": "",
		"decision": "",
		"averageScore": "",
		"dimensionScores": {},
		"riskFlags": "",
		"suggestion": "",
		"confidence": "",
		"flowAction": "",
		"promptMode": "",
		"degraded": true,
		"limitations": [],
		"errorCode": "",
		"errorMessage": "",
		"createdAt": "",
		"updatedAt": ""
	},
	"traceId": ""
}
```