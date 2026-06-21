FROM node:20.11.1-slim AS deps
WORKDIR /app

COPY package*.json ./
RUN npm config set registry https://registry.npmmirror.com
RUN npm ci

FROM deps AS build
WORKDIR /app

ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY . .
RUN npm run build

FROM nginx:1.25.3-alpine AS runtime

RUN addgroup -S nginxuser && adduser -S nginxuser -G nginxuser

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chown -R nginxuser:nginxuser /usr/share/nginx/html \
    && chown -R nginxuser:nginxuser /var/cache/nginx \
    && chown -R nginxuser:nginxuser /var/log/nginx \
    && touch /var/run/nginx.pid \
    && chown -R nginxuser:nginxuser /var/run/nginx.pid

EXPOSE 80

USER nginxuser

CMD ["nginx", "-g", "daemon off;"]
