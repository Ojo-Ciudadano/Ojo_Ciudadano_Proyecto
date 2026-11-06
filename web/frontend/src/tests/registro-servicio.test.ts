import { afterEach, describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { apiClient } from '../services/api'
import { authService } from '../services/auth.service'
import type { RegisterRequest } from '../types'

const originalPost = apiClient.post

afterEach(() => {
  apiClient.post = originalPost
})

describe('authService.register', () => {
  it('envía los datos a POST /auth/register y devuelve el usuario público', async () => {
    let capturedUrl: unknown
    let capturedBody: unknown

    apiClient.post = (async (url: string, body: unknown) => {
      capturedUrl = url
      capturedBody = body

      return {
        data: {
          id: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
          nombre: 'Ana Ciudadana',
          email: 'ana@example.invalid',
          telefono: null,
          fechaRegistro: '2026-11-05T12:00:00.000Z',
        },
      }
    }) as unknown as typeof apiClient.post

    const payload: RegisterRequest = {
      nombre: 'Ana Ciudadana',
      email: 'ana@example.invalid',
      password: '123456',
    }

    const result = await authService.register(payload)

    assert.equal(capturedUrl, '/auth/register')
    assert.deepEqual(capturedBody, payload)
    assert.equal(result.email, payload.email)
    assert.equal(result.nombre, payload.nombre)
  })

  it('propaga el error del backend cuando el correo ya está registrado', async () => {
    apiClient.post = (async () => {
      throw {
        statusCode: 409,
        timestamp: '2026-11-05T12:00:00.000Z',
        path: '/auth/register',
        error: 'Conflict',
        message: 'Ya existe una cuenta con ese correo',
      }
    }) as unknown as typeof apiClient.post

    await assert.rejects(
      authService.register({
        nombre: 'Ana Ciudadana',
        email: 'ana@example.invalid',
        password: '123456',
      }),
      (error: { statusCode?: number }) => error.statusCode === 409,
    )
  })
})
