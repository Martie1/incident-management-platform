export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type Status = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED'

export const SEVERITIES: readonly Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
export const STATUSES: readonly Status[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED']

export interface Incident {
  id: number
  title: string
  description: string | null
  severity: Severity
  status: Status
  assigneeId: number | null
  createdAt: string
  resolvedAt: string | null
}

export interface IncidentRequest {
  title: string
  description?: string | null
  severity: Severity
  status?: Status
  assigneeId?: number | null
}

export interface User {
  id: number
  name: string
  email: string
}

export interface UserRequest {
  name: string
  email: string
}

export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
}

/** `IN_PROGRESS` -> `In progress` */
export function label(value: Severity | Status): string {
  return value.charAt(0) + value.slice(1).toLowerCase().replace('_', ' ')
}

/** Shown for an `assigneeId` that doesn't match any known user. */
export function unknownUserLabel(id: number): string {
  return `Unknown user (#${id})`
}
