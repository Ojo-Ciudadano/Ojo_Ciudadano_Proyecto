# Verificación de Integración de Inicio de Sesión

## Objetivo
Verificar que el flujo de inicio de sesión web funciona correctamente.

## Pasos de verificación

### 1. Configuración previa
- [ ] Backend corriendo en `localhost:3000`
- [ ] Frontend corriendo en `localhost:5173`
- [ ] Base de datos PostgreSQL activa
- [ ] Usuario de prueba creado en la BD

### 2. Verificación visual
- [ ] La página de login se muestra correctamente
- [ ] El formulario tiene campos de email y contraseña
- [ ] El botón "Iniciar sesión" está presente
- [ ] El diseño es responsive

### 3. Validaciones de campos
- [ ] Email vacío muestra error
- [ ] Email inválido muestra error
- [ ] Contraseña vacía muestra error
- [ ] Contraseña corta muestra error

### 4. Flujo de autenticación
- [ ] Login exitoso redirige al dashboard
- [ ] Login fallido muestra mensaje de error
- [ ] Token JWT se guarda en localStorage
- [ ] Usuario se guarda en localStorage

### 5. Rutas protegidas
- [ ] Acceder a `/` sin login redirige a `/login`
- [ ] Acceder a `/` con login muestra el dashboard
- [ ] Cerrar sesión limpia el localStorage
- [ ] Cerrar sesión redirige a `/login`

### 6. Notificaciones
- [ ] Snackbar de éxito al iniciar sesión
- [ ] Snackbar de error al fallar
- [ ] Alert de error visible en el formulario

## Resultados

| Prueba | Resultado | Observaciones |
|---|---|---|
| Página de login | Pendiente | |
| Campos de formulario | Pendiente | |
| Validaciones | Pendiente | |
| Login exitoso | Pendiente | |
| Login fallido | Pendiente | |
| Rutas protegidas | Pendiente | |
| Notificaciones | Pendiente | |

## Fecha de verificación
___/___/______

## Verificado por
_________________