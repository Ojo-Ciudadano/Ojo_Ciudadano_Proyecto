import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  hasRegisterErrors,
  validateRegisterForm,
  type RegisterFormValues,
} from '../pages/auth/registerValidation'

function createValues(
  overrides: Partial<RegisterFormValues> = {},
): RegisterFormValues {
  return {
    nombre: 'Ana Ciudadana',
    email: 'ana@example.invalid',
    telefono: '',
    password: '123456',
    confirmPassword: '123456',
    ...overrides,
  }
}

describe('validateRegisterForm', () => {
  it('acepta un formulario válido', () => {
    const errors = validateRegisterForm(createValues())

    assert.deepEqual(errors, {})
    assert.equal(hasRegisterErrors(errors), false)
  })

  it('exige el nombre y rechaza solo espacios', () => {
    assert.equal(
      validateRegisterForm(createValues({ nombre: '' })).nombre,
      'El nombre es obligatorio',
    )
    assert.equal(
      validateRegisterForm(createValues({ nombre: '   ' })).nombre,
      'El nombre es obligatorio',
    )
  })

  it('exige el correo electrónico', () => {
    assert.equal(
      validateRegisterForm(createValues({ email: '' })).email,
      'El correo electrónico es obligatorio',
    )
  })

  it('rechaza un correo con formato inválido', () => {
    assert.equal(
      validateRegisterForm(createValues({ email: 'ana-sin-dominio' })).email,
      'Ingresa un correo electrónico válido',
    )
  })

  it('exige la contraseña', () => {
    assert.equal(
      validateRegisterForm(createValues({ password: '' })).password,
      'La contraseña es obligatoria',
    )
  })

  it('exige una contraseña de al menos 6 caracteres', () => {
    assert.equal(
      validateRegisterForm(createValues({ password: '12345' })).password,
      'La contraseña debe tener al menos 6 caracteres',
    )
  })

  it('exige confirmar la contraseña', () => {
    assert.equal(
      validateRegisterForm(createValues({ confirmPassword: '' }))
        .confirmPassword,
      'Debes confirmar la contraseña',
    )
  })

  it('rechaza contraseñas que no coinciden', () => {
    assert.equal(
      validateRegisterForm(createValues({ confirmPassword: '654321' }))
        .confirmPassword,
      'Las contraseñas no coinciden',
    )
  })

  it('informa todos los errores encontrados', () => {
    const errors = validateRegisterForm({
      nombre: '',
      email: 'correo-invalido',
      telefono: '',
      password: '123',
      confirmPassword: '456',
    })

    assert.deepEqual(errors, {
      nombre: 'El nombre es obligatorio',
      email: 'Ingresa un correo electrónico válido',
      password: 'La contraseña debe tener al menos 6 caracteres',
      confirmPassword: 'Las contraseñas no coinciden',
    })
    assert.equal(hasRegisterErrors(errors), true)
  })
})
