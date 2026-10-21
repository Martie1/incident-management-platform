import { useState } from 'react'
import { useSaveIncident } from '../api/incidents'
import {
  label,
  SEVERITIES,
  STATUSES,
  unknownUserLabel,
  type Incident,
  type Severity,
  type Status,
  type User,
} from '../api/types'

interface IncidentFormProps {
  /** Incident to edit; a new one is created when omitted. */
  incident?: Incident
  users: User[]
  onDone: () => void
  onCancel: () => void
}

export function IncidentForm({ incident, users, onDone, onCancel }: IncidentFormProps) {
  const save = useSaveIncident(incident)
  const [title, setTitle] = useState(incident?.title ?? '')
  const [description, setDescription] = useState(incident?.description ?? '')
  const [severity, setSeverity] = useState<Severity>(incident?.severity ?? 'MEDIUM')
  const [status, setStatus] = useState<Status>(incident?.status ?? 'OPEN')
  const [assigneeId, setAssigneeId] = useState(incident?.assigneeId?.toString() ?? '')

  const currentAssigneeId = incident?.assigneeId
  const unknownAssigneeId =
    currentAssigneeId != null && !users.some((user) => user.id === currentAssigneeId) ? currentAssigneeId : null

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        save.mutate(
          {
            title,
            description: description || null,
            severity,
            status,
            assigneeId: assigneeId ? Number(assigneeId) : null,
          },
          { onSuccess: onDone },
        )
      }}
    >
      <label>
        Title
        <input value={title} onChange={(event) => setTitle(event.target.value)} required maxLength={200} />
      </label>
      <label>
        Description
        <textarea
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          maxLength={2000}
          rows={3}
        />
      </label>
      <div className="grid">
        <label>
          Severity
          <select value={severity} onChange={(event) => setSeverity(event.target.value as Severity)} required>
            {SEVERITIES.map((value) => (
              <option key={value} value={value}>
                {label(value)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select value={status} onChange={(event) => setStatus(event.target.value as Status)}>
            {STATUSES.map((value) => (
              <option key={value} value={value}>
                {label(value)}
              </option>
            ))}
          </select>
        </label>
      </div>
      <label>
        Assignee
        <select value={assigneeId} onChange={(event) => setAssigneeId(event.target.value)}>
          <option value="">Unassigned</option>
          {unknownAssigneeId != null && <option value={unknownAssigneeId}>{unknownUserLabel(unknownAssigneeId)}</option>}
          {users.map((user) => (
            <option key={user.id} value={user.id}>
              {user.name}
            </option>
          ))}
        </select>
      </label>

      {save.error && (
        <p role="alert" className="error">
          {save.error.message}
        </p>
      )}
      <div className="form-actions">
        <button type="button" className="secondary" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" aria-busy={save.isPending} disabled={save.isPending}>
          {incident ? 'Save' : 'Create'}
        </button>
      </div>
    </form>
  )
}
