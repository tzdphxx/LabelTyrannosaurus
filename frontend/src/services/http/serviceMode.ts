import type { ServiceMode } from './httpTypes'

const serviceModeValues = new Set<ServiceMode>(['mock', 'real'])

export function getServiceMode(): ServiceMode {
  const mode = String(import.meta.env.VITE_SERVICE_MODE ?? '')
    .trim()
    .replace(/^['"]|['"]$/g, '')

  return serviceModeValues.has(mode as ServiceMode) ? (mode as ServiceMode) : 'mock'
}

export function isRealServiceMode() {
  return getServiceMode() === 'real'
}
