# syntax=docker/dockerfile:1

# =============================================================================
# Etapa 1 - Compilacion del bundle React (Vite)
# =============================================================================
FROM node:20-alpine AS builder

WORKDIR /app

ARG VITE_API_URL=https://ojociudadano.dev.censei.cl
ENV VITE_API_URL=${VITE_API_URL}

COPY package.json package-lock.json ./
RUN npm install
RUN npm install -g typescript

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
