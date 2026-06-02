import type { DynamicFieldType, DynamicSchemaNode, MaterialDefinition } from '../../types/dynamicForm'

const choiceOptions = [
  { label: '选项 A', value: 'option_a' },
  { label: '选项 B', value: 'option_b' },
]

export const dynamicMaterialRegistry: Record<DynamicFieldType, MaterialDefinition> = {
  input: {
    type: 'input',
    title: '单行输入',
    group: 'text',
    description: '适合短文本、编号、名称等字段。',
    acceptsChildren: false,
    defaultProps: {
      placeholder: '请输入',
    },
  },
  textarea: {
    type: 'textarea',
    title: '多行输入',
    group: 'text',
    description: '适合描述、理由、证据片段等长文本。',
    acceptsChildren: false,
    defaultProps: {
      placeholder: '请输入详细内容',
    },
  },
  radio: {
    type: 'radio',
    title: '单选',
    group: 'choice',
    description: '从多个互斥选项中选择一个。',
    acceptsChildren: false,
    defaultProps: {
      options: choiceOptions,
    },
  },
  checkbox: {
    type: 'checkbox',
    title: '多选',
    group: 'choice',
    description: '从多个选项中选择一个或多个。',
    acceptsChildren: false,
    defaultProps: {
      options: choiceOptions,
    },
  },
  select: {
    type: 'select',
    title: '标签选择',
    group: 'choice',
    description: '用于标签、类别和枚举值选择。',
    acceptsChildren: false,
    defaultProps: {
      mode: 'tags',
      options: choiceOptions,
      placeholder: '请选择或输入标签',
    },
  },
  showItem: {
    type: 'showItem',
    title: '展示项',
    group: 'display',
    description: '展示说明、样本或不可编辑信息。',
    acceptsChildren: false,
    defaultProps: {
      text: '这里展示只读说明或样本信息。',
    },
  },
  richText: {
    type: 'richText',
    title: '富文本',
    group: 'text',
    description: '适合较长说明、证据描述和带格式内容。',
    acceptsChildren: false,
    defaultProps: {
      placeholder: '请输入富文本内容',
    },
  },
  fileUpload: {
    type: 'fileUpload',
    title: '文件/图片上传',
    group: 'media',
    description: '用于图片、附件或证据文件占位。',
    acceptsChildren: false,
    defaultProps: {
      accept: 'image/*,.pdf,.txt',
      maxCount: 3,
    },
  },
  jsonEditor: {
    type: 'jsonEditor',
    title: 'JSON 编辑器',
    group: 'display',
    description: '用于结构化标注结果或 schema 调试。',
    acceptsChildren: false,
    defaultProps: {
      placeholder: '{\n  "key": "value"\n}',
    },
  },
  llmPrompt: {
    type: 'llmPrompt',
    title: 'LLM 交互',
    group: 'smart',
    description: '字段级模型调用，输出可作为标注参考或一键填充。',
    acceptsChildren: false,
    defaultProps: {
      providerId: '',
      modelName: 'gpt-4o',
      promptTemplate: '请根据当前题目材料和已填写答案，为目标字段生成结构化建议。',
      targetFields: [],
      prompt: '请根据当前题目材料和已填写答案，为目标字段生成结构化建议。',
      text: '点击运行后展示模型建议。',
    },
  },
  group: {
    type: 'group',
    title: '分组容器',
    group: 'layout',
    description: '把相关字段组织到一个分组内。',
    acceptsChildren: true,
    allowedChildren: ['input', 'textarea', 'radio', 'checkbox', 'select', 'showItem', 'richText', 'fileUpload', 'jsonEditor', 'llmPrompt'],
    defaultProps: {},
  },
  tabs: {
    type: 'tabs',
    title: 'Tab 容器',
    group: 'layout',
    description: '把字段拆成多个页签区域。',
    acceptsChildren: true,
    allowedChildren: ['tabPane'],
    defaultProps: {},
  },
  tabPane: {
    type: 'tabPane',
    title: 'Tab 面板',
    group: 'layout',
    description: 'Tab 容器内的单个面板。',
    acceptsChildren: true,
    allowedChildren: ['input', 'textarea', 'radio', 'checkbox', 'select', 'showItem', 'richText', 'fileUpload', 'jsonEditor', 'llmPrompt'],
    defaultProps: {},
  },
}

export const dynamicMaterialGroups = [
  { key: 'text', title: '文本' },
  { key: 'choice', title: '选择' },
  { key: 'display', title: '展示' },
  { key: 'media', title: '媒体' },
  { key: 'smart', title: '智能' },
  { key: 'layout', title: '结构' },
] as const

export const paletteMaterialTypes: DynamicFieldType[] = [
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
]

export function createSchemaNodeFromMaterial(type: DynamicFieldType): DynamicSchemaNode {
  const definition = dynamicMaterialRegistry[type]
  const suffix = Math.random().toString(36).slice(2, 7)
  const baseNode: DynamicSchemaNode = {
    id: `node-${Date.now()}-${suffix}`,
    key: `${type}_${suffix}`,
    type,
    title: definition.title,
    props: { ...definition.defaultProps },
    rules: definition.defaultRules ? [...definition.defaultRules] : [],
  }

  if (type === 'group') {
    return {
      ...baseNode,
      children: [],
    }
  }

  if (type === 'tabs') {
    return {
      ...baseNode,
      children: [
        createTabPaneNode('基础信息'),
        createTabPaneNode('补充信息'),
      ],
    }
  }

  return baseNode
}

export function createTabPaneNode(title: string): DynamicSchemaNode {
  const suffix = Math.random().toString(36).slice(2, 7)

  return {
    id: `node-${Date.now()}-${suffix}`,
    key: `tab_${suffix}`,
    type: 'tabPane',
    title,
    props: {},
    children: [],
  }
}

export function canAcceptChild(parentType: DynamicFieldType | null, childType: DynamicFieldType) {
  if (!parentType) {
    return childType !== 'tabPane'
  }

  const parentDefinition = dynamicMaterialRegistry[parentType]

  if (!parentDefinition.acceptsChildren) {
    return false
  }

  return parentDefinition.allowedChildren?.includes(childType) ?? false
}
