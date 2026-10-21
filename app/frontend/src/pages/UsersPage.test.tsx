import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import type { User, UserRequest } from '../api/types'
import { users as fixtures } from '../test/handlers'
import { renderWithProviders } from '../test/render'
import { server } from '../test/server'
import { UsersPage } from './UsersPage'

/** In-memory user store behind GET/POST/DELETE, so mutations show up on refetch. */
function mockUserStore() {
  let users: User[] = [...fixtures]
  server.use(
    http.get('/api/users', () => HttpResponse.json(users)),
    http.post('/api/users', async ({ request }) => {
      const user = { id: 10, ...((await request.json()) as UserRequest) }
      users = [...users, user]
      return HttpResponse.json(user, { status: 201 })
    }),
    http.delete('/api/users/:id', ({ params }) => {
      users = users.filter((user) => user.id !== Number(params.id))
      return new HttpResponse(null, { status: 204 })
    }),
  )
}

describe('UsersPage', () => {
  it('creates a user and shows it on the list', async () => {
    mockUserStore()
    const user = userEvent.setup()
    renderWithProviders(<UsersPage />, { route: '/users' })
    await screen.findByText('Anna Kowalska')

    await user.click(screen.getByRole('button', { name: 'New user' }))
    const dialog = screen.getByRole('dialog', { name: 'New user' })
    await user.type(screen.getByLabelText('Name'), 'Jan Zieliński')
    await user.type(screen.getByLabelText('Email'), 'jan.zielinski@example.com')
    await user.click(screen.getByRole('button', { name: 'Create' }))

    expect(await screen.findByText('Jan Zieliński')).toBeInTheDocument()
    expect(screen.getByText('jan.zielinski@example.com')).toBeInTheDocument()
    expect(dialog).not.toBeInTheDocument()
  })

  it('deletes a user after confirmation', async () => {
    mockUserStore()
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderWithProviders(<UsersPage />, { route: '/users' })
    await screen.findByText('Anna Kowalska')

    await user.click(screen.getByRole('button', { name: 'Delete Anna Kowalska' }))

    expect(confirm).toHaveBeenCalledWith('Delete user Anna Kowalska?')
    await waitFor(() => expect(screen.queryByText('Anna Kowalska')).not.toBeInTheDocument())
    expect(screen.getByText('Piotr Nowak')).toBeInTheDocument()
  })

  it('shows the conflict when the user still has open incidents', async () => {
    server.use(
      http.delete('/api/users/:id', () =>
        HttpResponse.json(
          { title: 'Conflict', status: 409, detail: 'User 1 has 2 open incidents assigned' },
          { status: 409 },
        ),
      ),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderWithProviders(<UsersPage />, { route: '/users' })
    await screen.findByText('Anna Kowalska')

    await user.click(screen.getByRole('button', { name: 'Delete Anna Kowalska' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('User 1 has 2 open incidents assigned')
    expect(screen.getByText('Anna Kowalska')).toBeInTheDocument()
  })
})
