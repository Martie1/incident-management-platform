import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { useDeleteIncident, useIncidents } from '../api/incidents'
import { label, SEVERITIES, STATUSES, unknownUserLabel, type Incident } from '../api/types'
import { useUsers } from '../api/users'
import { Badge } from '../components/Badge'
import { IncidentForm } from '../components/IncidentForm'
import { Modal } from '../components/Modal'

type Filter = 'status' | 'severity'

export function IncidentsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  // Unknown values in the URL are ignored instead of being sent to the backend.
  const status = STATUSES.find((value) => value === searchParams.get('status'))
  const severity = SEVERITIES.find((value) => value === searchParams.get('severity'))

  const incidents = useIncidents({ status, severity })
  const users = useUsers()
  const deleteIncident = useDeleteIncident()
  const [form, setForm] = useState<{ incident?: Incident } | null>(null)

  function setFilter(name: Filter, value: string) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (value) {
        next.set(name, value)
      } else {
        next.delete(name)
      }
      return next
    })
  }

  function assigneeName(assigneeId: number | null) {
    if (assigneeId == null) return 'Unassigned'
    if (!users.data) return '…'
    return users.data.find((user) => user.id === assigneeId)?.name ?? unknownUserLabel(assigneeId)
  }

  function handleDelete(incident: Incident) {
    if (window.confirm(`Delete incident #${incident.id} "${incident.title}"?`)) {
      deleteIncident.mutate(incident.id)
    }
  }

  return (
    <>
      <div className="toolbar">
        <h1>Incidents</h1>
        <button type="button" onClick={() => setForm({})}>
          New incident
        </button>
      </div>

      <div className="grid">
        <label>
          Status
          <select value={status ?? ''} onChange={(event) => setFilter('status', event.target.value)}>
            <option value="">All</option>
            {STATUSES.map((value) => (
              <option key={value} value={value}>
                {label(value)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Severity
          <select value={severity ?? ''} onChange={(event) => setFilter('severity', event.target.value)}>
            <option value="">All</option>
            {SEVERITIES.map((value) => (
              <option key={value} value={value}>
                {label(value)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {deleteIncident.error && (
        <p role="alert" className="error">
          {deleteIncident.error.message}
        </p>
      )}
      {incidents.isPending && <p>Loading…</p>}
      {incidents.isError && (
        <p role="alert" className="error">
          {incidents.error.message}
        </p>
      )}
      {incidents.isSuccess && (
        <div className="overflow-auto">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Severity</th>
                <th>Status</th>
                <th>Assignee</th>
                <th>Created</th>
                <th>Resolved</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {incidents.data.map((incident) => (
                <tr key={incident.id}>
                  <td>{incident.id}</td>
                  <td>{incident.title}</td>
                  <td>
                    <Badge kind="severity" value={incident.severity} />
                  </td>
                  <td>
                    <Badge kind="status" value={incident.status} />
                  </td>
                  <td>{assigneeName(incident.assigneeId)}</td>
                  <td>{new Date(incident.createdAt).toLocaleString()}</td>
                  <td>{incident.resolvedAt ? new Date(incident.resolvedAt).toLocaleString() : '—'}</td>
                  <td>
                    <div className="actions">
                      <button type="button" className="outline" onClick={() => setForm({ incident })}>
                        Edit
                      </button>
                      <button
                        type="button"
                        className="outline secondary"
                        aria-label={`Delete incident ${incident.id}`}
                        onClick={() => handleDelete(incident)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {incidents.data.length === 0 && (
                <tr>
                  <td colSpan={8}>No incidents found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {form && (
        <Modal title={form.incident ? `Edit incident #${form.incident.id}` : 'New incident'} onClose={() => setForm(null)}>
          <IncidentForm
            incident={form.incident}
            users={users.data ?? []}
            onDone={() => setForm(null)}
            onCancel={() => setForm(null)}
          />
        </Modal>
      )}
    </>
  )
}
