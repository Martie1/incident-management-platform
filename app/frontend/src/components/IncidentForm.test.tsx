import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { incidents, users } from '../test/handlers'
import { renderWithProviders } from '../test/render'
import { server } from '../test/server'
import { IncidentForm } from './IncidentForm'

describe('IncidentForm', () => {
  it('creates an incident with the entered values', async () => {
    let body: unknown
    server.use(
      http.post('/api/incidents', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ ...incidents[0], id: 3 }, { status: 201 })
      }),
    )
    const onDone = vi.fn()
    const user = userEvent.setup()
    renderWithProviders(<IncidentForm users={users} onDone={onDone} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Title'), 'Login page is down')
    await user.type(screen.getByLabelText('Description'), 'Returns 502')
    await user.selectOptions(screen.getByLabelText('Severity'), 'HIGH')
    await user.selectOptions(screen.getByLabelText('Assignee'), 'Piotr Nowak')
    await user.click(screen.getByRole('button', { name: 'Create' }))

    await waitFor(() => expect(onDone).toHaveBeenCalled())
    expect(body).toEqual({
      title: 'Login page is down',
      description: 'Returns 502',
      severity: 'HIGH',
      status: 'OPEN',
      assigneeId: 2,
    })
  })

  it('shows the backend error and stays open', async () => {
    server.use(
      http.post('/api/incidents', () =>
        HttpResponse.json(
          { title: 'Bad Request', status: 400, detail: 'Assignee 2 does not exist' },
          { status: 400 },
        ),
      ),
    )
    const onDone = vi.fn()
    const user = userEvent.setup()
    renderWithProviders(<IncidentForm users={users} onDone={onDone} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Title'), 'Login page is down')
    await user.click(screen.getByRole('button', { name: 'Create' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Assignee 2 does not exist')
    expect(onDone).not.toHaveBeenCalled()
  })

  it('keeps an unknown assignee selectable when editing', () => {
    renderWithProviders(
      <IncidentForm incident={{ ...incidents[0], assigneeId: 42 }} users={users} onDone={vi.fn()} onCancel={vi.fn()} />,
    )

    const assignee = screen.getByLabelText('Assignee')
    expect(assignee).toHaveValue('42')
    expect(screen.getByRole('option', { name: 'Unknown user (#42)', selected: true })).toBeInTheDocument()
  })
})
