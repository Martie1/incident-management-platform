import { useState } from 'react'
import type { User } from '../api/types'
import { useSaveUser } from '../api/users'

interface UserFormProps {
  /** User to edit; a new one is created when omitted. */
  user?: User
  onDone: () => void
  onCancel: () => void
}

export function UserForm({ user, onDone, onCancel }: UserFormProps) {
  const save = useSaveUser(user)
  const [name, setName] = useState(user?.name ?? '')
  const [email, setEmail] = useState(user?.email ?? '')

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        save.mutate({ name, email }, { onSuccess: onDone })
      }}
    >
      <label>
        Name
        <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={100} />
      </label>
      <label>
        Email
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          maxLength={254}
        />
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
          {user ? 'Save' : 'Create'}
        </button>
      </div>
    </form>
  )
}
