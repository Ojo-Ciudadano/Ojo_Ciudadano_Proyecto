# Excepción TLS temporal para probar Android debug

El usuario autorizó expresamente permitir el acceso de desarrollo tras revisar
el bloqueo TLS. Esta autorización posterior sustituye, para esta excepción
debug concreta, la restricción anterior de no modificar la validación TLS.
No se cambió backend, infraestructura, frontend o CI/CD.

## Alcance

- Solo se aplica a la variante `debug` y al cliente configurado para
  `https://api-ojociudadano.dev.censei.cl/`.
- La configuración de red debug confía en dos certificados públicos concretos
  observados en el ingreso. No hay un TrustManager que acepte cualquier cadena.
- El verificador conserva las comprobaciones normales de hostname. Como
  excepción, acepta el hostname exacto de la API cuando el certificado hoja
  está vigente y su SHA-256 coincide con uno de los dos certificados incluidos.
- No se incluyen subdominios, otros dominios ni otros certificados en la
  excepción. No se permite HTTP para la API. Se conservan las excepciones
  locales de HTTP que ya existían en debug.
- `src/release` contiene una implementación que devuelve el builder sin
  modificarlo. Los certificados y la excepción de hostname están en
  `src/debug` y no se empaquetan en release.

Esta es una decisión de confianza temporal para desarrollo sobre los
certificados observados, no una reparación de la identidad TLS del servidor.
Los certificados no fueron proporcionados por una CA institucional. Usar
cuentas de prueba. Si el ingreso cambia sus certificados, la excepción puede
dejar de funcionar; no se descargan ni aceptan certificados nuevos automáticamente.
La solución definitiva sigue siendo un certificado válido para el dominio.

## Archivos

- `app/src/main/java/cl/uct/ojociudadano/data/remote/RetrofitClient.kt`: invoca
  la configuración TLS correspondiente a la variante.
- `app/src/debug/java/cl/uct/ojociudadano/data/remote/EnvironmentTls.kt`:
  verificador con dominio y huellas acotados.
- `app/src/debug/res/xml/network_security_config.xml`: confianza por dominio.
- `app/src/debug/res/raw/debug_ingress_a.pem` y `debug_ingress_b.pem`:
  certificados públicos, sin claves privadas ni secretos.
- `app/src/release/java/cl/uct/ojociudadano/data/remote/EnvironmentTls.kt`:
  conserva la configuración original de producción.
- `app/src/testDebug/java/cl/uct/ojociudadano/data/remote/EnvironmentTlsTest.kt`:
  pruebas positivas y de rechazo de la excepción.

Huellas SHA-256 del certificado completo:

```text
BB:C1:E7:30:AD:19:37:BE:88:85:00:B3:70:5A:BC:B7:56:09:90:BA:9A:59:00:41:FF:53:59:C4:F6:94:80:AA
D6:8C:BD:F6:64:6D:C6:E4:9C:17:D0:1D:02:82:A6:D2:14:C6:AC:D9:D2:84:52:78:EF:89:70:1A:02:00:EC:7E
```

## Verificación

```sh
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease --console=plain
```

Resultado: BUILD SUCCESSFUL. De 292 pruebas, 289 aprobaron y 3 integraciones
remotas optativas se omitieron. No hubo fallos. Lint debug/release terminó con
cero errores y 25 advertencias por variante. Se generaron ambos APK; release
es un APK sin firmar. Se inspeccionó el XML de seguridad empaquetado en debug y
se comprobó que los dos certificados debug no existen dentro del APK release.
El bytecode release devuelve el builder original sin configurar otro verificador.

Las cinco pruebas nuevas verifican ambos certificados, rechazo de otro dominio
y subdominio, rechazo de un certificado diferente, ausencia/error del
certificado del servidor, conservación del verificador en entornos distintos
y funcionamiento de la comprobación normal del hostname.

Se comprobó además el login remoto desde una sonda JVM temporal usando el
verificador debug compilado y un almacén de confianza con únicamente los dos
certificados incluidos: HTTP 200, JWT presente y rol CIUDADANO. No se usó un
TrustManager permisivo ni se imprimieron credenciales o JWT. Esta comprobación
no ejecuta la UI Android ni sustituye la prueba manual del APK.

La variante release se verifica mediante compilación, revisión del bytecode
de su configuración TLS y ausencia de los certificados debug en su APK. Este
proyecto no habilita la tarea `testReleaseUnitTest`; no se afirmó haberla ejecutado.

## Prueba manual

1. Instalar `app/build/outputs/apk/debug/app-debug.apk`, reemplazando la versión
   anterior. También se puede ejecutar la variante debug desde Android Studio.
2. Iniciar sesión con la cuenta Ciudadano de prueba. Debe abrir el flujo
   Ciudadano con la sesión real, sin error TLS.
3. Cerrar sesión y probar la cuenta Administrador; verificar su destino.
4. Probar registro con un correo nuevo: debe volver al login con confirmación,
   sin establecer sesión automáticamente. La excepción TLS sirve para ambos
   endpoints, pero no se creó una cuenta desde la sonda de diagnóstico.
5. Verificar logout, restauración al reiniciar y credenciales incorrectas.

Si aparece un error 5xx, ya es una respuesta del servidor y no debe ocultarse.
Si reaparece el error TLS, comprobar si el ingreso cambió su certificado antes
de modificar las huellas. No ampliar la excepción a certificados arbitrarios.

Referencia de configuración por dominio y variantes:
[Network security configuration](https://developer.android.com/privacy-and-security/security-config).
