import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { request } from './client'
import type { User, UserRequest } from './types'

export function listUsers(): Promise<User[]> {
  return request<User[]>('/api/users')
}

export function createUser(body: UserRequest): Promise<User> {
  return request<User>('/api/users', { method: 'POST', body })
}

export function updateUser(id: number, body: UserRequest): Promise<User> {
  return request<User>(`/api/users/${id}`, { method: 'PUT', body })
}

export function deleteUser(id: number): Promise<void> {
  return request<void>(`/api/users/${id}`, { method: 'DELETE' })
}

export function useUsers() {
  return useQuery({ queryKey: ['users'], queryFn: listUsers })
}

/** Creates a new user, or replaces `user` when given. */
export function useSaveUser(user?: User) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: UserRequest) => (user ? updateUser(user.id, body) : createUser(body)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  })
}

export function useDeleteUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteUser,
    // The backend unassigns the user's resolved incidents, so those change too.
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['users'] }),
        queryClient.invalidateQueries({ queryKey: ['incidents'] }),
      ]),
  })
}
