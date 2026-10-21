import { http, HttpResponse } from 'msw'
import type { Incident, User } from '../api/types'

export const users: User[] = [
  { id: 1, name: 'Anna Kowalska', email: 'anna.kowalska@example.com' },
  { id: 2, name: 'Piotr Nowak', email: 'piotr.nowak@example.com' },
]

export const incidents: Incident[] = [
  {
    id: 1,
    title: 'Payment API returns 500',
    description: 'Checkout fails since the last deployment.',
    severity: 'CRITICAL',
    status: 'IN_PROGRESS',
    assigneeId: 1,
    createdAt: '2026-10-20T08:00:00Z',
    resolvedAt: null,
  },
  {
    id: 2,
    title: 'Disk usage above 85%',
    description: null,
    severity: 'MEDIUM',
    status: 'RESOLVED',
    assigneeId: null,
    createdAt: '2026-10-19T08:00:00Z',
    resolvedAt: '2026-10-19T10:00:00Z',
  },
]

export const handlers = [
  http.get('/api/users', () => HttpResponse.json(users)),
  http.get('/api/incidents', () => HttpResponse.json(incidents)),
]
