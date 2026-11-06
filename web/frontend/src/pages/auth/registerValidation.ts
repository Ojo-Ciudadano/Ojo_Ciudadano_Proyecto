export interface RegisterFormValues {
  nombre: string
  email: string
  telefono: string
  password: string
  confirmPassword: string
}

export interface RegisterFieldErrors {
  nombre?: string
  email?: string
  password?: string
  confirmPassword?: string
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const MIN_PASSWORD_LENGTH = 6

/**
 * Validaciones de cliente del formulario de registro web.
 * El backend (POST /api/v1/auth/register) vuelve a validar los datos.
 */
export function validateRegisterForm(
  values: RegisterFormValues,
): RegisterFieldErrors {
  const errors: RegisterFieldErrors = {}

  if (!values.nombre.trim()) {
    errors.nombre = 'El nombre es obligatorio'
  }

  if (!values.email.trim()) {
    errors.email = 'El correo electrónico es obligatorio'
  } else if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = 'Ingresa un correo electrónico válido'
  }

  if (!values.password) {
    errors.password = 'La contraseña es obligatoria'
  } else if (values.password.length < MIN_PASSWORD_LENGTH) {
    errors.password = `La contraseña debe tener al menos ${MIN_PASSWORD_LENGTH} caracteres`
  }

  if (!values.confirmPassword) {
    errors.confirmPassword = 'Debes confirmar la contraseña'
  } else if (values.confirmPassword !== values.password) {
    errors.confirmPassword = 'Las contraseñas no coinciden'
  }

  return errors
}

export function hasRegisterErrors(errors: RegisterFieldErrors): boolean {
  return Object.keys(errors).length > 0
}
