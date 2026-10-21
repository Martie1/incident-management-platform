import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import { ApiError, request } from './client'

describe('request', () => {
  it('returns the parsed JSON body', async () => {
    server.use(http.get('/api/ping', () => HttpResponse.json({ ok: true })))

    await expect(request('/api/ping')).resolves.toEqual({ ok: true })
  })

  it('returns undefined for 204 No Content', async () => {
    server.use(http.delete('/api/ping', () => new HttpResponse(null, { status: 204 })))

    await expect(request('/api/ping', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it.each([400, 404])('throws ApiError with the ProblemDetail detail for %i', async (status) => {
    server.use(
      http.get('/api/ping', () =>
        HttpResponse.json({ title: 'Error', status, detail: 'User 7 not found' }, { status }),
      ),
    )

    const error = await request('/api/ping').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status, message: 'User 7 not found' })
  })

  it('falls back to the ProblemDetail title when there is no detail', async () => {
    server.use(http.get('/api/ping', () => HttpResponse.json({ title: 'Conflict', status: 409 }, { status: 409 })))

    await expect(request('/api/ping')).rejects.toMatchObject({ status: 409, message: 'Conflict' })
  })

  it('falls back to a generic message when the error has no body', async () => {
    server.use(http.get('/api/ping', () => new HttpResponse(null, { status: 500 })))

    await expect(request('/api/ping')).rejects.toMatchObject({ status: 500, message: 'Request failed (500)' })
  })
})
