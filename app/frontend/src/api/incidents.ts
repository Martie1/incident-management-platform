import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { request } from './client'
import type { Incident, IncidentRequest, Severity, Status } from './types'

export interface IncidentFilters {
  status?: Status
  severity?: Severity
}

export function listIncidents(filters: IncidentFilters = {}): Promise<Incident[]> {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.severity) params.set('severity', filters.severity)
  const query = params.size > 0 ? `?${params}` : ''
  return request<Incident[]>(`/api/incidents${query}`)
}

export function createIncident(body: IncidentRequest): Promise<Incident> {
  return request<Incident>('/api/incidents', { method: 'POST', body })
}

export function updateIncident(id: number, body: IncidentRequest): Promise<Incident> {
  return request<Incident>(`/api/incidents/${id}`, { method: 'PUT', body })
}

export function deleteIncident(id: number): Promise<void> {
  return request<void>(`/api/incidents/${id}`, { method: 'DELETE' })
}

export function useIncidents({ status, severity }: IncidentFilters) {
  return useQuery({
    queryKey: ['incidents', { status, severity }],
    queryFn: () => listIncidents({ status, severity }),
  })
}

/** Creates a new incident, or replaces `incident` when given. */
export function useSaveIncident(incident?: Incident) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: IncidentRequest) => (incident ? updateIncident(incident.id, body) : createIncident(body)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incidents'] }),
  })
}

export function useDeleteIncident() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteIncident,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incidents'] }),
  })
}
