FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx prisma generate
ARG APP_NAME
RUN npm run build ${APP_NAME}

FROM node:20-alpine AS runner
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY --from=builder /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=builder /app/node_modules/@prisma ./node_modules/@prisma
COPY --from=builder /app/dist ./dist
ARG APP_NAME
ENV APP_MAIN_FILE=dist/apps/${APP_NAME}/main.js
CMD node ${APP_MAIN_FILE}
