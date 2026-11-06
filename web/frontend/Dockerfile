# syntax=docker/dockerfile:1

# =============================================================================
# Etapa 1 - Compilacion del bundle React (Vite)
# =============================================================================
FROM node:20-alpine AS builder

WORKDIR /app

ARG VITE_API_URL=https://ojociudadano.dev.censei.cl
ENV VITE_API_URL=${VITE_API_URL}

# Guarda de configuracion: el SPA debe consumir la API en su MISMO origen
# (https://ojociudadano.dev.censei.cl/api/v1). El certificado autofirmado del
# Ingress solo cubre ese host; apuntar al host de API rompe las llamadas del
# navegador (CORS/TLS) y es un error que ya ocurrio con la imagen v1.0.2.
RUN if [ "${VITE_API_URL}" = "https://api-ojociudadano.dev.censei.cl" ]; then \
        echo "ERROR: VITE_API_URL debe ser el host de la Web (same-origin), no el host de la API." >&2; \
        exit 1; \
    fi; \
    case "${VITE_API_URL}" in \
        */) echo "ERROR: VITE_API_URL no debe terminar en '/' (api.ts agrega /api/v1)." >&2; exit 1;; \
    esac

COPY package.json package-lock.json ./
# Se usa npm ci --no-audit --no-fund con reintentos de red para evitar saturar
# el resolver DNS de Alpine (EAI_AGAIN) y garantizar que node_modules/.bin/tsc
# quede instalado de forma determinista desde package-lock.json.
RUN npm config set fetch-retries 5 && \
    npm config set fetch-retry-mintimeout 20000 && \
    npm config set fetch-retry-maxtimeout 120000 && \
    npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# =============================================================================
# Etapa 2 - Servidor Nginx con los activos estaticos
# =============================================================================
FROM nginx:stable-alpine AS runner

# Se reemplaza la configuracion por defecto por la del proyecto (SPA fallback).
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
