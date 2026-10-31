# syntax=docker/dockerfile:1

# =============================================================================
# Etapa 1 - Compilacion del bundle React (Vite)
# =============================================================================
FROM node:20-alpine AS builder

WORKDIR /app

# URL publica del API Gateway. Vite incrusta las variables VITE_* en el bundle
# durante `npm run build`; el valor por defecto apunta al API Gateway remoto de
# Kubernetes UCT para que la imagen desplegada nunca use localhost.
ARG VITE_API_URL=https://ojociudadano.dev.censei.cl
ENV VITE_API_URL=${VITE_API_URL}

# Se copian primero los manifiestos de dependencias para aprovechar la cache de
# capas de Docker mientras el codigo fuente no cambie.
COPY package.json package-lock.json ./
RUN npm ci

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
