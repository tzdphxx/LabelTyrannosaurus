import type { DynamicFormSchema, DynamicSchemaNode } from '../../../types/dynamicForm'

type NodeVisitor = (node: DynamicSchemaNode, parent: DynamicSchemaNode | null) => void

function cloneNode(node: DynamicSchemaNode): DynamicSchemaNode {
  return {
    ...node,
    props: { ...node.props },
    rules: node.rules ? [...node.rules] : undefined,
    visibleWhen: node.visibleWhen ? { ...node.visibleWhen } : undefined,
    children: node.children?.map(cloneNode),
  }
}

export function cloneSchema(schema: DynamicFormSchema): DynamicFormSchema {
  return {
    ...schema,
    nodes: schema.nodes.map(cloneNode),
  }
}

export function walkSchemaNodes(nodes: DynamicSchemaNode[], visitor: NodeVisitor, parent: DynamicSchemaNode | null = null) {
  nodes.forEach((node) => {
    visitor(node, parent)

    if (node.children) {
      walkSchemaNodes(node.children, visitor, node)
    }
  })
}

export function findSchemaNode(schema: DynamicFormSchema, nodeId: string) {
  let found: DynamicSchemaNode | null = null

  walkSchemaNodes(schema.nodes, (node) => {
    if (node.id === nodeId) {
      found = node
    }
  })

  return found
}

export function updateSchemaNode(
  schema: DynamicFormSchema,
  nodeId: string,
  updater: (node: DynamicSchemaNode) => DynamicSchemaNode,
): DynamicFormSchema {
  function updateList(nodes: DynamicSchemaNode[]): DynamicSchemaNode[] {
    return nodes.map((node) => {
      if (node.id === nodeId) {
        return updater(cloneNode(node))
      }

      return {
        ...node,
        children: node.children ? updateList(node.children) : undefined,
      }
    })
  }

  return {
    ...schema,
    nodes: updateList(schema.nodes),
  }
}

export function insertSchemaNode(
  schema: DynamicFormSchema,
  node: DynamicSchemaNode,
  parentId: string | null,
): DynamicFormSchema {
  if (!parentId) {
    return {
      ...schema,
      nodes: [...schema.nodes, node],
    }
  }

  return updateSchemaNode(schema, parentId, (parent) => ({
    ...parent,
    children: [...(parent.children ?? []), node],
  }))
}

export function deleteSchemaNode(schema: DynamicFormSchema, nodeId: string): DynamicFormSchema {
  function deleteFromList(nodes: DynamicSchemaNode[]): DynamicSchemaNode[] {
    return nodes
      .filter((node) => node.id !== nodeId)
      .map((node) => ({
        ...node,
        children: node.children ? deleteFromList(node.children) : undefined,
      }))
  }

  return {
    ...schema,
    nodes: deleteFromList(schema.nodes),
  }
}

export function reorderSchemaNodes(schema: DynamicFormSchema, activeId: string, overId: string): DynamicFormSchema {
  function reorderList(nodes: DynamicSchemaNode[]): { nodes: DynamicSchemaNode[]; changed: boolean } {
    const activeIndex = nodes.findIndex((node) => node.id === activeId)
    const overIndex = nodes.findIndex((node) => node.id === overId)

    if (activeIndex >= 0 && overIndex >= 0) {
      const nextNodes = [...nodes]
      const [movedNode] = nextNodes.splice(activeIndex, 1)
      nextNodes.splice(overIndex, 0, movedNode)

      return {
        nodes: nextNodes,
        changed: true,
      }
    }

    let changed = false
    const nextNodes = nodes.map((node) => {
      if (!node.children) {
        return node
      }

      const childResult = reorderList(node.children)

      if (childResult.changed) {
        changed = true

        return {
          ...node,
          children: childResult.nodes,
        }
      }

      return node
    })

    return {
      nodes: nextNodes,
      changed,
    }
  }

  return {
    ...schema,
    nodes: reorderList(schema.nodes).nodes,
  }
}

export function getSchemaNodeKeys(schema: DynamicFormSchema) {
  const keys: string[] = []

  walkSchemaNodes(schema.nodes, (node) => {
    if (node.type !== 'group' && node.type !== 'tabs' && node.type !== 'tabPane') {
      keys.push(node.key)
    }
  })

  return keys
}

export function validateDynamicSchema(schema: DynamicFormSchema) {
  const errors: string[] = []
  const keys = new Map<string, string>()

  walkSchemaNodes(schema.nodes, (node) => {
    if (!node.title.trim()) {
      errors.push('字段标题不能为空')
    }

    if (node.type !== 'group' && node.type !== 'tabs' && node.type !== 'tabPane') {
      if (!node.key.trim()) {
        errors.push(`${node.title} 的字段 key 不能为空`)
      }

      if (keys.has(node.key)) {
        errors.push(`字段 key "${node.key}" 重复`)
      }

      keys.set(node.key, node.id)
    }

    if ((node.type === 'radio' || node.type === 'checkbox' || node.type === 'select') && !node.props.options?.length) {
      errors.push(`${node.title} 至少需要一个选项`)
    }

    if (node.type === 'tabs' && !node.children?.length) {
      errors.push(`${node.title} 至少需要一个 Tab 面板`)
    }
  })

  return {
    valid: errors.length === 0,
    errors,
  }
}
