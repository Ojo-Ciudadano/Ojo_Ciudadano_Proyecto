# Probes de salud de los servicios backend en Kubernetes UCT

Los Deployments `api-gateway`, `auth-usuarios`, `incidentes` y
`notificaciones`, definidos en
[`infrastructure/k8s/services/backend-workloads.yaml`](../infrastructure/k8s/services/backend-workloads.yaml),
usan `startupProbe`, `livenessProbe` y `readinessProbe` por HTTP en
`/health`, en el puerto de cada servicio (3000, 3001, 3002 y 3003,
respectivamente).

## Endpoint de salud

Cada aplicación expone `GET /health`, sin autenticación y sin consultar
PostgreSQL, Supabase ni otros servicios. Devuelve HTTP 200 y
`{"status":"ok"}` cuando el proceso HTTP está atendiendo solicitudes. En el
API Gateway, la ruta está excluida del prefijo global `/api/v1` para que siga
siendo accesible en `/health`.

El endpoint es deliberadamente liviano: sirve para determinar si el proceso
está en ejecución, no para certificar la salud de dependencias externas.
Las sondas de readiness y liveness comparten esta señal de proceso, de modo
que una caída de una dependencia no provoque reinicios repetidos de procesos
que siguen respondiendo.

## Umbrales

- **Startup:** espera 5 segundos antes de la primera comprobación y reintenta
  cada 5 segundos hasta 30 fallos. Esto permite aproximadamente 150 segundos
  de reintentos (más el retardo inicial) para completar el arranque en frío
  con el límite de CPU de 150m de UCT. Mientras no pase, Kubernetes no ejecuta
  liveness ni readiness.
- **Liveness:** consulta cada 10 segundos, con timeout de 3 segundos y 3
  fallos consecutivos. Si el proceso deja de responder, Kubernetes reinicia el
  contenedor.
- **Readiness:** consulta cada 5 segundos, con timeout de 3 segundos y 2
  fallos consecutivos. Si falla, el Pod deja de recibir tráfico hasta que la
  sonda vuelva a responder correctamente.

## Diagnóstico

Reemplaza `<pod>` y `<namespace>` por los valores del entorno:

```sh
kubectl describe pod <pod> -n <namespace>
kubectl logs <pod> -n <namespace> -c <contenedor>
```

En `describe pod`, revisa `Events`, el estado de las sondas y los contadores de
fallos. Mensajes como `connection refused` suelen indicar que la aplicación
aún no abrió el puerto configurado; un `404` indica que la ruta `/health` no
está disponible en el contenedor. En `kubectl logs`, revisa errores de
bootstrap, configuración y excepciones de inicio. Para observar los eventos
recientes de un Pod:

```sh
kubectl get events -n <namespace> --field-selector involvedObject.name=<pod> --sort-by=.lastTimestamp
```
