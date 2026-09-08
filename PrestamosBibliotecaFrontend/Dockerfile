# ---- Build ----
FROM oven/bun:1 AS build
WORKDIR /app
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run build

# ---- Runtime (Nginx sirve el SPA y proxya /api al backend) ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/PrestamosBibliotecaFrontend/browser /usr/share/nginx/html
EXPOSE 80
