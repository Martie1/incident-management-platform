import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { incidents } from '../test/handlers'
import { renderWithProviders } from '../test/render'
import { server } from '../test/server'
import { IncidentsPage } from './IncidentsPage'

describe('IncidentsPage', () => {
  it('renders incidents from the API', async () => {
    renderWithProviders(<IncidentsPage />, { route: '/incidents' })

    const row = (await screen.findByText('Payment API returns 500')).closest('tr')!
    expect(within(row).getByText('Critical')).toBeInTheDocument()
    expect(within(row).getByText('In progress')).toBeInTheDocument()
    expect(await within(row).findByText('Anna Kowalska')).toBeInTheDocument()
    expect(within(row).getByText('—')).toBeInTheDocument()

    const resolvedRow = screen.getByText('Disk usage above 85%').closest('tr')!
    expect(within(resolvedRow).getByText('Unassigned')).toBeInTheDocument()
  })

  it('sends the selected filters to the backend and keeps them in the URL', async () => {
    const requested: URLSearchParams[] = []
    server.use(
      http.get('/api/incidents', ({ request }) => {
        requested.push(new URL(request.url).searchParams)
        return HttpResponse.json(incidents)
      }),
    )
    const user = userEvent.setup()
    renderWithProviders(<IncidentsPage />, { route: '/incidents' })
    await screen.findByText('Payment API returns 500')

    await user.selectOptions(screen.getByLabelText('Status'), 'OPEN')
    await user.selectOptions(screen.getByLabelText('Severity'), 'HIGH')

    await waitFor(() => expect(requested.at(-1)?.toString()).toBe('status=OPEN&severity=HIGH'))
    expect(requested[0].toString()).toBe('')
  })

  it('reads the initial filters from the query string', async () => {
    const requested: URLSearchParams[] = []
    server.use(
      http.get('/api/incidents', ({ request }) => {
        requested.push(new URL(request.url).searchParams)
        return HttpResponse.json([])
      }),
    )
    renderWithProviders(<IncidentsPage />, { route: '/incidents?severity=LOW' })

    expect(await screen.findByText('No incidents found.')).toBeInTheDocument()
    expect(screen.getByLabelText('Severity')).toHaveValue('LOW')
    expect(requested.map(String)).toEqual(['severity=LOW'])
  })

  it('shows "Unknown user" for an assignee missing from the user list', async () => {
    server.use(http.get('/api/incidents', () => HttpResponse.json([{ ...incidents[0], assigneeId: 42 }])))

    renderWithProviders(<IncidentsPage />, { route: '/incidents' })

    expect(await screen.findByText('Unknown user (#42)')).toBeInTheDocument()
  })
})
