import { label, type Severity, type Status } from '../api/types'

type BadgeProps = { kind: 'severity'; value: Severity } | { kind: 'status'; value: Status }

export function Badge({ kind, value }: BadgeProps) {
  return <span className={`badge badge-${kind}-${value.toLowerCase()}`}>{label(value)}</span>
}
