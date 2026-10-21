import { useState } from 'react'
import type { User } from '../api/types'
import { useDeleteUser, useUsers } from '../api/users'
import { Modal } from '../components/Modal'
import { UserForm } from '../components/UserForm'

export function UsersPage() {
  const users = useUsers()
  const deleteUser = useDeleteUser()
  const [form, setForm] = useState<{ user?: User } | null>(null)

  function handleDelete(user: User) {
    if (window.confirm(`Delete user ${user.name}?`)) {
      deleteUser.mutate(user.id)
    }
  }

  return (
    <>
      <div className="toolbar">
        <h1>Users</h1>
        <button type="button" onClick={() => setForm({})}>
          New user
        </button>
      </div>

      {/* e.g. 409 when the user still has open incidents assigned */}
      {deleteUser.error && (
        <p role="alert" className="error">
          {deleteUser.error.message}
        </p>
      )}
      {users.isPending && <p>Loading…</p>}
      {users.isError && (
        <p role="alert" className="error">
          {users.error.message}
        </p>
      )}
      {users.isSuccess && (
        <div className="overflow-auto">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.data.map((user) => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>
                    <div className="actions">
                      <button type="button" className="outline" onClick={() => setForm({ user })}>
                        Edit
                      </button>
                      <button
                        type="button"
                        className="outline secondary"
                        aria-label={`Delete ${user.name}`}
                        onClick={() => handleDelete(user)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {users.data.length === 0 && (
                <tr>
                  <td colSpan={4}>No users found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {form && (
        <Modal title={form.user ? `Edit user #${form.user.id}` : 'New user'} onClose={() => setForm(null)}>
          <UserForm user={form.user} onDone={() => setForm(null)} onCancel={() => setForm(null)} />
        </Modal>
      )}
    </>
  )
}
