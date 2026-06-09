import type {
  DynamicFieldType,
  DynamicFormSchema,
  DynamicFormSubmitResult,
  DynamicSchemaNode,
  DynamicValidationRule,
} from '../../types/dynamicForm'
import type {
  LabelerAssignmentListQuery,
  LabelerAssignmentStats,
  LabelerAssignmentStatus,
  LabelerAssignmentSummary,
  LabelerClaimOptions,
  LabelerSubmissionStats,
  LabelerTaskListQuery,
  LabelerTaskStatus,
  LabelerTaskSummary,
  LabelingDraft,
  LabelingQuestion,
  LabelingReviewSummary,
  LabelingSubmission,
  LabelingSubmitResult,
  LabelingSubmitValidationResult,
  SubmissionItemHistoryResponse,
} from '../../types/labeling'
import { fromBackendTemplateSchema } from '../../features/dynamic-form/utils/backendSchema'
import { ApiError, request } from '../http'
import { mockLabelingService } from './labelingService'
import { getNowLabel, validateQuestionDraft, validateTaskDrafts } from './labelingServiceHelpers'

interface TaskSnapshotResponse {
  taskId: number
  title: string
  tags?: string[]
  quota?: number
  deadlineAt?: string
  status?: string
  strategy?: string
  claimedCount?: number
  maxClaimsPerLabeler?: number
  max_claims_per_labeler?: number
  overlapCount?: number
  publishedAt?: string
  endedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

interface RewardSummaryResponse {
  rewardMode?: string
  unitReward?: number
  rewardCurrency?: string
  amount?: number
  unit?: string
  text?: string
}

interface MarketItemPreviewResponse {
  itemId: number
  externalId?: string
  itemJson?: string
  metadataJson?: string
}

interface MarketTaskResponse {
  task?: TaskSnapshotResponse
  taskId?: number
  title?: string
  description?: string
  instructionRichText?: string
  tags?: string[]
  quota?: number
  deadlineAt?: string
  status?: string
  availableCount?: number
  currentUserClaimedCount?: number
  maxClaimsPerLabeler?: number
  max_claims_per_labeler?: number
  itemsPreview?: MarketItemPreviewResponse[]
  rewardSummary?: {
    amount?: number
    unit?: string
    text?: string
    rewardMode?: string
    unitReward?: number
    rewardCurrency?: string
  }
}

interface AssignmentClaimResponse {
  assignmentId: number
  taskId?: number
  datasetItemId: number
  templateVersionId: number
  status?: string
  schemaJson?: string
  itemJson?: string
  metadataJson?: string
  draftAnswerJson?: string
  draftVersion?: number
}

interface ClaimItemResponse {
  claimId: number
  itemId: number
  submissionId?: number
  latestSubmissionId?: number
  externalId?: string
  claimStatus?: string
  itemJson?: string
  metadataJson?: string
  draftVersion?: number
  latestSubmissionStatus?: string
  updatedAt?: string
}

interface ClaimedTaskResponse {
  task?: TaskSnapshotResponse
  taskId: number
  title?: string
  description?: string
  instructionRichText?: string
  myClaimedCount?: number
  mySubmittedCount?: number
  myApprovedCount?: number
  items?: ClaimItemResponse[]
}

interface AnswerTemplateResponse {
  taskId: number
  templateVersionId: number
  schemaJson?: string
}

interface AssignmentDraftResponse {
  assignmentId: number
  draftAnswerJson?: string
  draftVersion?: number
  status?: string
  updatedAt?: string
}

interface SubmissionSubmitResponse {
  submissionId?: number
  assignmentId?: number
  versionNo?: number
  status?: string
}

interface CachedAssignmentContext {
  assignmentId: string
  taskId: string
  datasetItemId?: string
  templateVersionId?: string
  task?: LabelerTaskSummary
  question?: LabelingQuestion
  draft?: LabelingDraft
  draftVersion?: number
  status?: string
  claimedAt?: string
  returnedAt?: string | null
  updatedAt?: string
}

interface CachedClaimedTaskContext {
  task: LabelerTaskSummary
  questions: LabelingQuestion[]
}

const assignmentCacheKey = 'labelhub-real-labeler-assignments'
const assignmentCache = readAssignmentCache()
const claimedTaskCache: Record<string, CachedClaimedTaskContext> = {}

function readAssignmentCache(): Record<string, CachedAssignmentContext> {
  if (typeof window === 'undefined') {
    return {}
  }

  try {
    const rawValue = window.sessionStorage.getItem(assignmentCacheKey)
    const parsedValue = rawValue ? JSON.parse(rawValue) : {}

    return parsedValue && typeof parsedValue === 'object' && !Array.isArray(parsedValue)
      ? parsedValue as Record<string, CachedAssignmentContext>
      : {}
  } catch {
    return {}
  }
}

function persistAssignmentCache() {
  if (typeof window === 'undefined') {
    return
  }

  window.sessionStorage.setItem(assignmentCacheKey, JSON.stringify(assignmentCache))
}

function cacheAssignment(context: CachedAssignmentContext) {
  assignmentCache[context.assignmentId] = {
    ...assignmentCache[context.assignmentId],
    ...context,
  }
  persistAssignmentCache()

  return assignmentCache[context.assignmentId]
}

function findAssignmentByTaskId(taskId: string) {
  return Object.values(assignmentCache).find((context) => context.taskId === taskId)
}

function parseJsonValue(value?: string | null): unknown {
  if (!value) {
    return null
  }

  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function parseJsonDeep(value: unknown): unknown {
  let current = value

  for (let index = 0; index < 3; index += 1) {
    if (typeof current !== 'string') {
      return current
    }

    const parsed = parseJsonValue(current)

    if (parsed === null) {
      return current
    }

    current = parsed
  }

  return current
}

function toRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function formatValue(value: unknown) {
  if (value === null || value === undefined) {
    return ''
  }

  if (typeof value === 'string') {
    return value
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }

  return JSON.stringify(value)
}

function toStringValue(value: unknown, fallback: string) {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function isDynamicFieldType(value: unknown): value is DynamicFieldType {
  return [
    'input',
    'textarea',
    'radio',
    'checkbox',
    'select',
    'showItem',
    'richText',
    'fileUpload',
    'jsonEditor',
    'llmPrompt',
    'group',
    'tabs',
    'tabPane',
  ].includes(String(value))
}

function normalizeFieldType(type: unknown, component?: unknown, hasChildren = false): DynamicFieldType {
  if (isDynamicFieldType(type)) {
    return type
  }

  const componentName = String(component ?? type ?? '').toLowerCase()

  if (componentName.includes('llm')) {
    return 'llmPrompt'
  }

  if (componentName.includes('textarea')) {
    return 'textarea'
  }

  if (componentName.includes('radio')) {
    return 'radio'
  }

  if (componentName.includes('checkbox')) {
    return 'checkbox'
  }

  if (componentName.includes('select')) {
    return 'select'
  }

  if (componentName.includes('showitem')) {
    return 'showItem'
  }

  if (componentName.includes('richtext')) {
    return 'richText'
  }

  if (componentName.includes('upload')) {
    return 'fileUpload'
  }

  if (componentName.includes('json')) {
    return 'jsonEditor'
  }

  if (componentName.includes('tabs')) {
    return 'tabs'
  }

  if (componentName.includes('tabpane')) {
    return 'tabPane'
  }

  if (hasChildren || String(type).toLowerCase() === 'object') {
    return 'group'
  }

  return 'input'
}

function normalizeRules(value: unknown): DynamicValidationRule[] | undefined {
  const validators = Array.isArray(value) ? value : value ? [value] : []
  const rules: DynamicValidationRule[] = []

  validators.forEach((validator) => {
    const validatorRecord = toRecord(validator)
    const message = validatorRecord.message as string | undefined

    if (validatorRecord.required) {
      rules.push({ type: 'required', message })

      return
    }

    if (typeof validatorRecord.min === 'number') {
      rules.push({ type: 'minLength', value: validatorRecord.min, message })

      return
    }

    if (typeof validatorRecord.max === 'number') {
      rules.push({ type: 'maxLength', value: validatorRecord.max, message })

      return
    }

    if (Array.isArray(validatorRecord.enum)) {
      const values = validatorRecord.enum.filter((item): item is string | number | boolean =>
        typeof item === 'string' || typeof item === 'number' || typeof item === 'boolean',
      )

      if (values.length) {
        rules.push({ type: 'enum', values, message })
      }
    }
  })

  return rules.length ? rules : undefined
}

function isDynamicValidationRule(value: unknown): value is DynamicValidationRule {
  const rule = toRecord(value)

  return ['required', 'minLength', 'maxLength', 'enum'].includes(String(rule.type))
}

function normalizeDynamicNode(value: unknown, fallbackKey: string): DynamicSchemaNode | null {
  const node = toRecord(value)

  if (!Object.keys(node).length) {
    return null
  }

  const rawChildren = Array.isArray(node.children) ? node.children : []
  const key = toStringValue(node.key ?? node.name, fallbackKey)
  const type = normalizeFieldType(node.type, node['x-component'], rawChildren.length > 0)
  const children = normalizeDynamicNodes(rawChildren, key)
  const rules = Array.isArray(node.rules) ? node.rules.filter(isDynamicValidationRule) : undefined

  return {
    ...node,
    id: toStringValue(node.id, `node-${key}`),
    key,
    type,
    title: toStringValue(node.title ?? node.label, key),
    props: toRecord(node.props),
    rules,
    children: children.length ? children : undefined,
  }
}

function normalizeFormilyProperty(key: string, value: unknown): DynamicSchemaNode | null {
  const property = toRecord(value)

  if (!Object.keys(property).length) {
    return null
  }

  const properties = toRecord(property.properties)
  const children = normalizeFormilyProperties(properties, key)
  const componentProps = toRecord(property['x-component-props'])
  const type = normalizeFieldType(property.type, property['x-component'], children.length > 0)

  return {
    id: toStringValue(property.id, `node-${key}`),
    key,
    type,
    title: toStringValue(property.title, key),
    defaultValue: property.default,
    props: componentProps,
    rules: normalizeRules(property['x-validator']),
    children: children.length ? children : undefined,
  }
}

function normalizeTemplateComponent(value: unknown, fallbackKey: string): DynamicSchemaNode | null {
  const component = toRecord(value)

  if (!Object.keys(component).length) {
    return null
  }

  const rawChildren = Array.isArray(component.children)
    ? component.children
    : Array.isArray(component.components)
      ? component.components
      : []
  const key = toStringValue(component.field ?? component.key ?? component.name, fallbackKey)
  const type = normalizeFieldType(undefined, component.type, rawChildren.length > 0)
  const children = normalizeTemplateComponents(rawChildren, key)
  const props = {
    ...Object.fromEntries(
      Object.entries(component).filter(([propKey]) =>
        !['id', 'type', 'field', 'key', 'name', 'label', 'title', 'required', 'rules', 'children', 'components'].includes(propKey),
      ),
    ),
    ...toRecord(component.props),
  }
  const rules = normalizeRules(component.rules) ?? []

  if (component.required && !rules.some((rule) => rule.type === 'required')) {
    rules.push({ type: 'required' })
  }

  if (type === 'showItem' && props.text === undefined) {
    props.text = toStringValue(component.label ?? component.field, key)
  }

  return {
    id: toStringValue(component.id, `node-${key}`),
    key,
    type,
    title: toStringValue(component.title ?? component.label, key),
    props,
    rules: rules.length ? rules : undefined,
    children: children.length ? children : undefined,
  }
}

function normalizeDynamicNodes(nodes: unknown[], parentKey = 'field') {
  return nodes
    .map((node, index) => normalizeDynamicNode(node, `${parentKey}_${index + 1}`))
    .filter((node): node is DynamicSchemaNode => Boolean(node))
}

function normalizeTemplateComponents(components: unknown[], parentKey = 'field') {
  return components
    .map((component, index) => normalizeTemplateComponent(component, `${parentKey}_${index + 1}`))
    .filter((node): node is DynamicSchemaNode => Boolean(node))
}

function normalizeFormilyProperties(properties: Record<string, unknown>, parentKey = 'field') {
  return Object.entries(properties)
    .map(([key, property], index) => normalizeFormilyProperty(key || `${parentKey}_${index + 1}`, property))
    .filter((node): node is DynamicSchemaNode => Boolean(node))
}

function parseSchema(schemaJson?: unknown, templateVersionId?: number | string): DynamicFormSchema {
  const parsedValue = parseJsonDeep(schemaJson)
  const parsedSchema = toRecord(parsedValue)
  const nestedSchema = toRecord(parsedSchema.schema)
  const sourceSchema = Object.keys(nestedSchema).length ? nestedSchema : parsedSchema

  if (Array.isArray(sourceSchema.components)) {
    const backendSchema = fromBackendTemplateSchema(sourceSchema)

    return {
      ...backendSchema,
      id: String(backendSchema.id ?? sourceSchema.id ?? templateVersionId ?? 'template'),
      version: String(backendSchema.version ?? sourceSchema.version ?? templateVersionId ?? '1'),
      title: String(backendSchema.title ?? sourceSchema.title ?? '标注模板'),
    }
  }

  const rawNodes = Array.isArray(parsedValue)
    ? parsedValue
    : Array.isArray(sourceSchema.nodes)
      ? sourceSchema.nodes
      : []
  const parsedNodes = rawNodes.length
    ? normalizeDynamicNodes(rawNodes)
    : Array.isArray(sourceSchema.components)
      ? normalizeTemplateComponents(sourceSchema.components)
      : normalizeFormilyProperties(toRecord(sourceSchema.properties))

  return {
    id: String(sourceSchema.id ?? templateVersionId ?? 'template'),
    version: String(sourceSchema.version ?? templateVersionId ?? '1'),
    title: String(sourceSchema.title ?? '标注模板'),
    nodes: parsedNodes,
  }
}

function buildSourceRecord(itemList: unknown[]): Record<string, string> {
  if (itemList.length === 0) {
    return {}
  }

  return itemList.reduce<Record<string, string>>((source, item, index) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      Object.entries(item as Record<string, unknown>).forEach(([key, value]) => {
        const sourceKey = source[key] === undefined ? key : `${key} ${index + 1}`
        source[sourceKey] = formatValue(value)
      })

      return source
    }

    source[`材料 ${index + 1}`] = formatValue(item)

    return source
  }, {})
}

function buildClaimedItemSource(item: ClaimItemResponse | AssignmentClaimResponse) {
  const sourceItems = [parseJsonValue(item.itemJson), parseJsonValue(item.metadataJson)].filter((value) => value !== null)

  return buildSourceRecord(sourceItems)
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function buildRewardText(rewardSummary?: RewardSummaryResponse) {
  if (rewardSummary?.text) {
    return rewardSummary.text
  }

  if (rewardSummary?.unitReward !== undefined) {
    return `${rewardSummary.unitReward}${rewardSummary.rewardCurrency ? ` ${rewardSummary.rewardCurrency}` : ''}/题`
  }

  if (rewardSummary?.amount !== undefined) {
    return `${rewardSummary.amount}${rewardSummary.unit ?? ''}`
  }

  return '-'
}

function getMarketTaskSnapshot(response: MarketTaskResponse): TaskSnapshotResponse {
  return response.task ?? {
    taskId: response.taskId ?? 0,
    title: response.title ?? '',
    tags: response.tags,
    quota: response.quota,
    deadlineAt: response.deadlineAt,
    status: response.status,
  }
}

function getMaxClaimsPerLabeler(response: MarketTaskResponse, task: TaskSnapshotResponse) {
  return response.maxClaimsPerLabeler ?? response.max_claims_per_labeler ?? task.maxClaimsPerLabeler ?? task.max_claims_per_labeler
}

function mapMarketStatus(response: MarketTaskResponse): LabelerTaskStatus {
  const availableCount = response.availableCount ?? 0

  if (availableCount <= 0) {
    return 'ended'
  }

  return 'available'
}

function mapTaskStatusToAssignmentStatus(status: LabelerTaskStatus): LabelerAssignmentStatus {
  switch (status) {
    case 'in_progress':
      return 'DRAFTING'
    case 'submitted':
      return 'SUBMITTED'
    case 'approved':
      return 'APPROVED'
    case 'rejected':
      return 'RETURNED'
    case 'ended':
      return 'CANCELLED'
    case 'available':
    case 'claimed':
    default:
      return 'CLAIMED'
  }
}

function mapQuestionStatus(status?: string): LabelingQuestion['status'] {
  switch (status) {
    case 'DRAFTING':
      return 'draft'
    case 'AI_RETURNED':
    case 'RETURNED':
      return 'rejected'
    case 'SUBMITTED':
    case 'APPROVED':
      return 'submitted'
    case 'CLAIMED':
    default:
      return 'pending'
  }
}

function mapSubmissionStatus(status?: string): LabelingSubmission['status'] {
  if (status === 'APPROVED') {
    return 'approved'
  }

  if (status === 'AI_RETURNED' || status === 'RETURNED' || status === 'REJECTED') {
    return 'rejected'
  }

  return 'submitted'
}

function getTaskStatusFromClaimedItems(items: ClaimItemResponse[]): LabelerTaskStatus {
  if (items.length === 0) {
    return 'claimed'
  }

  if (items.some((item) => item.claimStatus === 'AI_RETURNED' || item.claimStatus === 'RETURNED')) {
    return 'rejected'
  }

  if (items.some((item) => item.claimStatus === 'DRAFTING')) {
    return 'in_progress'
  }

  if (items.every((item) => item.claimStatus === 'APPROVED')) {
    return 'approved'
  }

  if (items.every((item) => item.claimStatus === 'SUBMITTED' || item.claimStatus === 'APPROVED')) {
    return 'submitted'
  }

  if (items.every((item) => item.claimStatus === 'CANCELLED')) {
    return 'ended'
  }

  return 'claimed'
}

function buildTaskSummaryFromMarket(response: MarketTaskResponse): LabelerTaskSummary {
  const task = getMarketTaskSnapshot(response)
  const totalQuestions = task.quota ?? response.availableCount ?? 0
  const availableCount = response.availableCount ?? 0

  return {
    id: String(task.taskId),
    title: task.title || `任务 #${task.taskId}`,
    description: response.description ?? '',
    instruction: response.instructionRichText ?? response.description ?? '',
    tags: task.tags ?? [],
    status: mapMarketStatus(response),
    templateId: '',
    templateName: '-',
    deadline: formatDateTime(task.deadlineAt),
    rewardText: buildRewardText(response.rewardSummary),
    totalQuestions,
    completedQuestions: Math.max(totalQuestions - availableCount, 0),
    strategy: task.strategy,
    availableCount,
    currentUserClaimedCount: response.currentUserClaimedCount,
    maxClaimsPerLabeler: getMaxClaimsPerLabeler(response, task),
  }
}

function buildTaskSummaryFromClaimedTask(response: ClaimedTaskResponse): LabelerTaskSummary {
  const task = response.task ?? {
    taskId: response.taskId,
    title: response.title ?? '',
  }
  const items = response.items ?? []
  const completedQuestions = response.mySubmittedCount ?? items.filter((item) => item.claimStatus === 'SUBMITTED' || item.claimStatus === 'APPROVED').length

  return {
    id: String(task.taskId),
    title: task.title || `任务 #${task.taskId}`,
    description: response.description ?? '',
    instruction: response.instructionRichText ?? response.description ?? '',
    tags: task.tags ?? [],
    status: getTaskStatusFromClaimedItems(items),
    templateId: '',
    templateName: '-',
    deadline: formatDateTime(task.deadlineAt),
    rewardText: '-',
    totalQuestions: response.myClaimedCount ?? items.length,
    completedQuestions,
    strategy: task.strategy,
    claimedAt: items[0]?.updatedAt ? formatDateTime(items[0].updatedAt) : undefined,
    submittedAt: completedQuestions > 0 ? formatDateTime(items[0]?.updatedAt) : undefined,
    reviewedAt: formatDateTime(task.updatedAt),
  }
}

function buildQuestionFromClaimedItem(
  taskId: string,
  item: ClaimItemResponse,
  schema: DynamicFormSchema,
  templateVersionId?: number | string | null,
): LabelingQuestion {
  return {
    id: String(item.claimId),
    taskId,
    assignmentId: String(item.claimId),
    submissionId: item.latestSubmissionId ?? item.submissionId ?? null,
    datasetItemId: String(item.itemId),
    templateVersionId,
    title: item.externalId ? `题目 ${item.externalId}` : `题目 #${item.itemId}`,
    description: '',
    source: buildClaimedItemSource(item),
    schema,
    status: mapQuestionStatus(item.claimStatus),
  }
}

function buildAssignmentSummary(response: ClaimedTaskResponse): LabelerAssignmentSummary {
  const task = response.task ?? {
    taskId: response.taskId,
    title: response.title ?? '',
  }
  const items = response.items ?? []
  const firstItem = items[0]
  const status = getTaskStatusFromClaimedItems(items)

  return {
    id: String(task.taskId),
    assignmentId: firstItem ? String(firstItem.claimId) : String(task.taskId),
    taskId: String(task.taskId),
    taskTitle: task.title || `任务 #${task.taskId}`,
    datasetItemId: firstItem ? String(firstItem.itemId) : '-',
    status: mapTaskStatusToAssignmentStatus(status),
    draftVersion: firstItem?.draftVersion ?? 0,
    claimedAt: firstItem?.updatedAt ? formatDateTime(firstItem.updatedAt) : '-',
    updatedAt: firstItem?.updatedAt ? formatDateTime(firstItem.updatedAt) : formatDateTime(task.updatedAt),
    myClaimedCount: response.myClaimedCount ?? items.length,
    mySubmittedCount: response.mySubmittedCount ?? items.filter((item) => item.claimStatus === 'SUBMITTED').length,
    myApprovedCount: response.myApprovedCount ?? items.filter((item) => item.claimStatus === 'APPROVED').length,
  }
}

function buildAssignmentStats(assignments: LabelerAssignmentSummary[]): LabelerAssignmentStats {
  return {
    total: assignments.length,
    claimed: assignments.filter((assignment) => assignment.status === 'CLAIMED').length,
    drafting: assignments.filter((assignment) => assignment.status === 'DRAFTING').length,
    submitted: assignments.filter((assignment) => assignment.status === 'SUBMITTED').length,
    returned: assignments.filter((assignment) => assignment.status === 'RETURNED' || assignment.status === 'AI_RETURNED').length,
    approved: assignments.filter((assignment) => assignment.status === 'APPROVED').length,
    cancelled: assignments.filter((assignment) => assignment.status === 'CANCELLED').length,
  }
}

function buildQuestion(detail: AssignmentClaimResponse, taskId: string): LabelingQuestion {
  const assignmentId = String(detail.assignmentId)
  const datasetItemId = String(detail.datasetItemId)
  const templateVersionId = detail.templateVersionId

  return {
    id: assignmentId,
    taskId,
    title: `题目 #${datasetItemId}`,
    description: '',
    source: buildClaimedItemSource(detail),
    schema: parseSchema(detail.schemaJson, templateVersionId),
    status: mapQuestionStatus(detail.status),
  }
}

function buildDraft(
  taskId: string,
  questionId: string,
  userId: string,
  values: Record<string, unknown>,
  updatedAt?: string,
): LabelingDraft {
  return {
    id: `draft-${questionId}`,
    taskId,
    questionId,
    userId,
    values,
    updatedAt: formatDateTime(updatedAt) === '-' ? getNowLabel() : formatDateTime(updatedAt),
  }
}

function buildSubmission(
  task: LabelerTaskSummary,
  response: SubmissionSubmitResponse,
  answer: DynamicFormSubmitResult,
  userId: string,
): LabelingSubmission {
  return {
    id: String(response.submissionId ?? response.assignmentId ?? `submission-${task.id}-${Date.now()}`),
    taskId: task.id,
    taskTitle: task.title,
    userId,
    status: mapSubmissionStatus(response.status),
    submittedAt: getNowLabel(),
    answers: [
      {
        ...answer,
        values: { ...answer.values },
      },
    ],
  }
}

function cloneQuestion(question: LabelingQuestion): LabelingQuestion {
  return {
    ...question,
    source: { ...question.source },
    schema: {
      ...question.schema,
      nodes: [...question.schema.nodes],
    },
  }
}

function matchesQuery(task: LabelerTaskSummary, query: LabelerTaskListQuery) {
  const keyword = query.keyword.trim().toLowerCase()
  const matchesKeyword =
    keyword.length === 0 ||
    task.title.toLowerCase().includes(keyword) ||
    task.description.toLowerCase().includes(keyword) ||
    task.tags.some((tag) => tag.toLowerCase().includes(keyword))
  const matchesTag = query.tag === 'all' || task.tags.includes(query.tag)
  const matchesStatus = query.status === 'all' || task.status === query.status

  return matchesKeyword && matchesTag && matchesStatus
}

function normalizeListResponse<T>(response: T[] | { list?: T[]; records?: T[]; content?: T[]; data?: T[] }) {
  if (Array.isArray(response)) {
    return response
  }

  return response.list ?? response.records ?? response.content ?? response.data ?? []
}

function getErrorDetailMessage(error: ApiError) {
  const details = error.details

  if (details && typeof details === 'object' && 'message' in details && typeof details.message === 'string') {
    return details.message
  }

  return ''
}

function normalizeAssignmentError(error: unknown): never {
  if (!(error instanceof ApiError)) {
    throw error
  }

  const code = String(error.code ?? '')
  const messageByCode: Record<string, string> = {
    '400101': '当前 assignment 状态不允许提交',
    '409101': '草稿版本冲突，请刷新后重试',
    '409301': 'Schema 校验失败，请检查答案后重试',
  }

  if (messageByCode[code]) {
    const detailMessage = getErrorDetailMessage(error)

    throw new ApiError({
      code: error.code,
      message: detailMessage || messageByCode[code],
      status: error.status,
      url: error.url,
      method: error.method,
      details: error.details,
    })
  }

  throw error
}

async function fetchClaimedTasks(query: LabelerAssignmentListQuery = {}) {
  const response = await request.get<ClaimedTaskResponse[] | { list?: ClaimedTaskResponse[]; records?: ClaimedTaskResponse[]; content?: ClaimedTaskResponse[]; data?: ClaimedTaskResponse[] }>(
    '/v1/claims',
    {
      params: {
        taskId: query.taskId,
        status: query.status === 'all' ? undefined : query.status,
        page: query.page ?? 1,
        size: query.size ?? 100,
      },
    },
  )
  const claimedTasks = normalizeListResponse(response)

  claimedTasks.forEach((claimedTask) => {
    const task = buildTaskSummaryFromClaimedTask(claimedTask)

    claimedTask.items?.forEach((item) => {
      cacheAssignment({
        ...assignmentCache[String(item.claimId)],
        assignmentId: String(item.claimId),
        taskId: task.id,
        datasetItemId: String(item.itemId),
        task,
        draftVersion: item.draftVersion,
        status: item.claimStatus,
        updatedAt: item.updatedAt,
      })
    })
  })

  return claimedTasks
}

async function resolveAssignmentForTask(taskId: string) {
  const cachedAssignment = findAssignmentByTaskId(taskId)

  if (cachedAssignment) {
    return cachedAssignment
  }

  const claimedTask = claimedTaskCache[taskId] ?? await loadClaimedTask(taskId)
  const firstQuestion = claimedTask.questions[0]

  return firstQuestion ? assignmentCache[firstQuestion.id] : null
}

async function loadAnswerTemplate(taskId: string) {
  return request.get<AnswerTemplateResponse>(`/v1/labeler/tasks/${taskId}/answer-template`)
}

async function loadAssignmentDetail(assignmentId: string) {
  return assignmentCache[assignmentId] ?? null
}

async function loadClaimedTask(taskId: string) {
  const [claimedTasks, answerTemplate] = await Promise.all([
    fetchClaimedTasks({ taskId, page: 1, size: 100 }),
    loadAnswerTemplate(taskId),
  ])
  const response = claimedTasks[0] ?? {
    taskId: Number(taskId),
    title: `任务 #${taskId}`,
    items: [],
  }
  const resolvedTaskId = String(response.task?.taskId ?? response.taskId ?? taskId)
  const items = response.items ?? []
  const task = {
    ...buildTaskSummaryFromClaimedTask(response),
    templateId: String(answerTemplate.templateVersionId ?? ''),
    templateName: answerTemplate.templateVersionId ? `模板版本 #${answerTemplate.templateVersionId}` : '-',
  }
  const schema = parseSchema(answerTemplate.schemaJson, answerTemplate.templateVersionId)
  const questions = items.map((item) => buildQuestionFromClaimedItem(resolvedTaskId, item, schema, answerTemplate.templateVersionId))

  questions.forEach((question, index) => {
    const item = items[index]

    cacheAssignment({
      ...assignmentCache[question.id],
      assignmentId: question.id,
      taskId: resolvedTaskId,
      datasetItemId: String(item.itemId),
      templateVersionId: String(answerTemplate.templateVersionId ?? ''),
      task,
      question,
      draft: assignmentCache[question.id]?.draft,
      draftVersion: assignmentCache[question.id]?.draftVersion ?? item.draftVersion,
      status: assignmentCache[question.id]?.status ?? item.claimStatus,
      updatedAt: assignmentCache[question.id]?.updatedAt ?? item.updatedAt,
    })
  })

  claimedTaskCache[resolvedTaskId] = {
    task,
    questions,
  }

  return claimedTaskCache[resolvedTaskId]
}

async function ensureAssignmentDetailForTask(taskId: string) {
  const assignment = await resolveAssignmentForTask(taskId)

  if (!assignment) {
    return null
  }

  if (assignment.question) {
    return assignment
  }

  return assignment
}

export const realLabelingService = {
  async listMarketTasks(query: LabelerTaskListQuery): Promise<LabelerTaskSummary[]> {
    const params = {
      keyword: query.keyword || undefined,
      tag: query.tag === 'all' ? undefined : query.tag,
      status: query.status === 'available' || query.status === 'all' ? 'PUBLISHED' : undefined,
    }
    const marketResponse = await request.get<MarketTaskResponse[]>('/v1/market/tasks', { params })

    return marketResponse.map(buildTaskSummaryFromMarket).filter((task) => matchesQuery(task, query))
  },

  async listTags(): Promise<string[]> {
    const marketTasks = await this.listMarketTasks({
      keyword: '',
      tag: 'all',
      status: 'available',
    })

    return Array.from(new Set(marketTasks.flatMap((task) => task.tags ?? []))).sort((first, second) =>
      first.localeCompare(second, 'zh-CN'),
    )
  },

  async getTaskDetail(taskId: string): Promise<LabelerTaskSummary | null> {
    const claimedTask = claimedTaskCache[taskId] ?? await loadClaimedTask(taskId)

    return {
      ...claimedTask.task,
      tags: [...claimedTask.task.tags],
    }
  },

  async claimTask(taskId: string, options: LabelerClaimOptions): Promise<LabelerTaskSummary | null> {
    const claimResponse = await request.post<AssignmentClaimResponse, LabelerClaimOptions>(
      `/v1/tasks/${taskId}/items/claim`,
      options,
    )
    const assignmentId = String(claimResponse.assignmentId)
    const question = claimResponse.schemaJson ? buildQuestion(claimResponse, taskId) : undefined
    const task = claimedTaskCache[taskId]?.task
    const nextTask: LabelerTaskSummary = task
      ? {
          ...task,
          status: 'claimed',
          claimedAt: getNowLabel(),
        }
      : {
          id: taskId,
          title: `任务 #${taskId}`,
          description: '',
          instruction: '',
          tags: [],
          status: 'claimed',
          templateId: '',
          templateName: '-',
          deadline: '-',
          rewardText: '-',
          totalQuestions: 1,
          completedQuestions: 0,
          claimedAt: getNowLabel(),
        }

    cacheAssignment({
      assignmentId,
      taskId,
      datasetItemId: String(claimResponse.datasetItemId),
      templateVersionId: String(claimResponse.templateVersionId),
      task: nextTask,
      question,
      draft: question
        ? buildDraft(taskId, question.id, '', toRecord(parseJsonValue(claimResponse.draftAnswerJson)))
        : undefined,
      draftVersion: claimResponse.draftVersion,
      status: claimResponse.status ?? 'CLAIMED',
      claimedAt: getNowLabel(),
    })

    return nextTask
  },

  async listQuestions(taskId: string): Promise<LabelingQuestion[]> {
    const claimedTask = claimedTaskCache[taskId] ?? await loadClaimedTask(taskId)

    return claimedTask.questions.map(cloneQuestion)
  },

  async getQuestion(questionId: string): Promise<LabelingQuestion | null> {
    const assignment = assignmentCache[questionId] ?? await loadAssignmentDetail(questionId)

    return assignment.question ? { ...assignment.question, source: { ...assignment.question.source } } : null
  },

  async getDraft(taskId: string, questionId: string, userId: string): Promise<LabelingDraft | null> {
    const assignment = assignmentCache[questionId] ?? await ensureAssignmentDetailForTask(taskId)

    if (!assignment) {
      return null
    }

    const draftResponse = await request.get<AssignmentDraftResponse>(`/v1/claims/${assignment.assignmentId}/draft`).catch((error: unknown) => {
      normalizeAssignmentError(error)
    })
    const draft = buildDraft(taskId, assignment.assignmentId, userId, toRecord(parseJsonValue(draftResponse.draftAnswerJson)), draftResponse.updatedAt)

    cacheAssignment({
      ...assignment,
      draft,
      draftVersion: draftResponse.draftVersion,
      status: draftResponse.status ?? assignment.status,
      updatedAt: draftResponse.updatedAt,
    })

    return draft
  },

  async saveDraft(payload: Omit<LabelingDraft, 'id' | 'updatedAt'>): Promise<LabelingDraft> {
    const assignment = assignmentCache[payload.questionId] ?? await ensureAssignmentDetailForTask(payload.taskId)
    const assignmentId = assignment?.assignmentId ?? payload.questionId
    const draftResponse = await request.put<AssignmentDraftResponse, { answerJson: string; clientVersion: number }>(
      `/v1/claims/${assignmentId}/draft`,
      {
        answerJson: JSON.stringify(payload.values),
        clientVersion: assignment?.draftVersion ?? 0,
      },
    ).catch((error: unknown) => {
      normalizeAssignmentError(error)
    })
    const draftValues = draftResponse.draftAnswerJson
      ? toRecord(parseJsonValue(draftResponse.draftAnswerJson))
      : payload.values
    const draft = buildDraft(payload.taskId, assignmentId, payload.userId, draftValues, draftResponse.updatedAt)

    cacheAssignment({
      ...(assignment ?? {
        assignmentId,
        taskId: payload.taskId,
      }),
      draft,
      draftVersion: draftResponse.draftVersion,
      status: draftResponse.status ?? 'DRAFTING',
      updatedAt: draftResponse.updatedAt,
    })

    return draft
  },

  async submitAnswers(taskId: string, userId: string, answers: DynamicFormSubmitResult[]): Promise<LabelingSubmission | null> {
    const assignment = await ensureAssignmentDetailForTask(taskId)

    if (!assignment || !answers[0]) {
      return null
    }

    await this.saveDraft({
      taskId,
      questionId: assignment.assignmentId,
      userId,
      values: answers[0].values,
    })

    const result = await this.submitQuestionDraft(taskId, assignment.assignmentId, userId)

    return result.submission
  },

  async validateSubmission(taskId: string, userId: string): Promise<LabelingSubmitValidationResult> {
    const assignment = await ensureAssignmentDetailForTask(taskId)
    const questions = assignment?.question ? [assignment.question] : []
    const drafts = assignment?.draft ? [{ ...assignment.draft, userId }] : []

    return validateTaskDrafts(questions, drafts, taskId, userId)
  },

  async submitTaskDrafts(taskId: string, userId: string): Promise<LabelingSubmitResult> {
    const assignment = await ensureAssignmentDetailForTask(taskId)

    if (!assignment) {
      return {
        submission: null,
        validation: {
          valid: false,
          errors: [
            {
              questionId: '',
              questionTitle: '',
              message: '当前任务没有可提交的题目',
            },
          ],
        },
      }
    }

    return this.submitQuestionDraft(taskId, assignment.assignmentId, userId)
  },

  async submitQuestionDraft(taskId: string, questionId: string, userId: string): Promise<LabelingSubmitResult> {
    const assignment = assignmentCache[questionId] ?? await ensureAssignmentDetailForTask(taskId)
    const question = assignment?.question
    const draft = assignment?.draft ? { ...assignment.draft, userId } : null
    const validation = validateQuestionDraft(question ? [question] : [], draft ? [draft] : [], taskId, questionId, userId)

    if (!assignment || !question || !draft || !validation.valid) {
      return {
        submission: null,
        validation,
      }
    }

    const answer: DynamicFormSubmitResult = {
      templateId: question.schema.id,
      schemaVersion: question.schema.version,
      values: { ...draft.values },
    }
    const submitResponse = await request.post<SubmissionSubmitResponse, { answerJson: string; clientVersion: number }>(
      `/v1/claims/${assignment.assignmentId}/submit`,
      {
        answerJson: JSON.stringify(draft.values),
        clientVersion: assignment.draftVersion ?? 0,
      },
    ).catch((error: unknown) => {
      normalizeAssignmentError(error)
    })
    const task = assignment.task ?? await this.getTaskDetail(taskId) ?? {
      id: taskId,
      title: `任务 #${taskId}`,
      description: '',
      instruction: '',
      tags: [],
      status: 'submitted' as const,
      templateId: '',
      templateName: '-',
      deadline: '-',
      rewardText: '-',
      totalQuestions: 1,
      completedQuestions: 1,
    }
    const submission = buildSubmission(task, submitResponse, answer, userId)

    cacheAssignment({
      ...assignment,
      question: {
        ...question,
        submissionId: submission.id,
        status: 'submitted',
      },
      task: {
        ...task,
        status: submission.status,
        completedQuestions: Math.max(task.completedQuestions, 1),
        submittedAt: submission.submittedAt,
      },
      status: submitResponse.status ?? 'SUBMITTED',
    })

    return {
      submission,
      validation,
    }
  },

  async getReviewSummary(taskId: string): Promise<LabelingReviewSummary | null> {
    void taskId

    return null
  },

  async getSubmissionItemHistory(submissionId: string): Promise<SubmissionItemHistoryResponse | null> {
    return request.get<SubmissionItemHistoryResponse>(`/v1/submissions/${submissionId}/item-history`)
  },

  async getSubmissionStats(): Promise<LabelerSubmissionStats> {
    return mockLabelingService.getSubmissionStats()
  },

  async listSubmissions(): Promise<LabelingSubmission[]> {
    return mockLabelingService.listSubmissions()
  },

  async getAssignmentStats(): Promise<LabelerAssignmentStats> {
    const assignments = await fetchClaimedTasks({ page: 1, size: 100 })

    return buildAssignmentStats(assignments.map(buildAssignmentSummary))
  },

  async listAssignments(query: LabelerAssignmentListQuery = {}): Promise<LabelerAssignmentSummary[]> {
    const assignments = await fetchClaimedTasks(query)

    return assignments.map(buildAssignmentSummary)
  },
}
