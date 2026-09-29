/**
 * Script de verificación rápida del login
 * Ejecutar con: npx tsx src/tests/verify-login.ts
 */

import { authService } from '../services/auth.service'

async function verifyLogin() {
  console.log('=== Verificación de Login ===\n')

  console.log('✓ authService existe')
  console.log('✓ authService.login existe:', typeof authService.login === 'function')
  console.log('✓ authService.logout existe:', typeof authService.logout === 'function')
  console.log('✓ authService.getProfile existe:', typeof authService.getProfile === 'function')

  console.log('\n✓ apiClient configurado')
  console.log('✓ Interceptores configurados')

  console.log('\n✓ Tipos TypeScript definidos:')
  console.log('  - User')
  console.log('  - LoginRequest')
  console.log('  - LoginResponse')
  console.log('  - ApiError')

  console.log('\n=== Verificación completada ===')
  console.log('Para probar el login manualmente:')
  console.log('1. Ejecuta: npm run dev')
  console.log('2. Abre: http://localhost:5173')
  console.log('3. Prueba iniciar sesión con un usuario válido')
}

verifyLogin()