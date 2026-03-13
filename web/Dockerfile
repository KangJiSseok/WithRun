FROM node:20-alpine AS build
WORKDIR /app
RUN corepack enable

COPY web/package.json web/pnpm-lock.yaml ./web/
WORKDIR /app/web
RUN pnpm install --frozen-lockfile

WORKDIR /app
RUN ln -s /app/web/node_modules /app/node_modules

COPY web ./web
COPY mobile/src/bridge ./mobile/src/bridge

WORKDIR /app/web
RUN pnpm build

FROM nginx:alpine
COPY --from=build /app/web/dist /usr/share/nginx/html
COPY web/nginx.conf /etc/nginx/conf.d/default.conf  # ← 추가
EXPOSE 80