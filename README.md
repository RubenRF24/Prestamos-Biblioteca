# Sistema de Préstamos de Biblioteca

Reemplazo de la planilla de Excel por una aplicación real: catálogo de libros, préstamos con
reglas de negocio (atrasos, bloqueos, lista de espera), notificaciones por correo (asíncronas y
programadas), integración con **Open Library** por ISBN y seguridad con **JWT en cookie httpOnly**.

- **Backend:** Java 21 · Spring Boot 4 · PostgreSQL · Flyway · Spring Security · Spring Mail (Thymeleaf) · Caffeine
- **Frontend:** Angular 22 · Tailwind v4 · Signals · Bun
- **Infra:** Docker Compose (PostgreSQL + MailHog + backend + frontend)

---

## Cómo correrlo (un solo comando)

Requisito: Docker + Docker Compose.

```bash
docker compose up --build
```

Esto levanta **todo**:

| Servicio      | URL                              | Para qué |
|---------------|----------------------------------|----------|
| **Frontend**  | http://localhost:4200            | La aplicación |
| **Backend**   | http://localhost:8080            | API REST |
| **Swagger UI**| http://localhost:8080/swagger-ui.html | Documentación de la API |
| **MailHog**   | http://localhost:8025            | Bandeja donde llegan los correos enviados |
| PostgreSQL    | localhost:5433                   | Base de datos (5433 para no chocar con un Postgres local) |

No hace falta crear ningún archivo: el `docker-compose.yml` trae defaults de desarrollo.
Para personalizarlos, copiá `.env.example` a `.env` y editá lo que necesites.

### Usuario ADMIN de prueba

Se crea automáticamente al arrancar (seeder idempotente):

- **Correo:** `admin@biblioteca.local`
- **Contraseña:** `Admin123!`

Cualquiera puede registrarse desde el frontend (rol `BIBLIOTECARIO`).

### Ver los correos

Todo correo que envía la app (confirmación de préstamo, recordatorio, bloqueo, libro disponible,
activación de cuenta) aparece en **MailHog → http://localhost:8025**. No se usan credenciales SMTP reales.

---

## Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `biblioteca` | Credenciales de Postgres |
| `JWT_SECRET` | *(secreto de dev)* | Clave para firmar el JWT (≥ 32 bytes). **En prod es obligatoria.** |
| `APP_COOKIE_SECURE` | `false` (en compose) | `true` en prod con HTTPS; `false` en la demo local (http) |
| `APP_FRONTEND_BASE_URL` | `http://localhost:4200` | Usada en los links de los correos |
| `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | `admin@biblioteca.local` / `Admin123!` | ADMIN de arranque |
| `FRONTEND_PORT` / `BACKEND_PORT` / `POSTGRES_PORT` / `MAILHOG_SMTP_PORT` / `MAILHOG_UI_PORT` | `4200` / `8080` / `5433` / `1025` / `8025` | Puertos del host (cambialos si alguno está ocupado) |

**Regla dura:** no se sube ningún secreto ni `.env` real al repositorio (ver `.gitignore`).
Tampoco hay hashes de contraseña versionados: el ADMIN se siembra por código usando el `PasswordEncoder`.

---

## Arquitectura

Arquitectura **por dominio (screaming / package-by-feature)** en ambos lados: al abrir el proyecto
se ven los dominios del negocio, no las capas técnicas.

**Backend** (`com.biblioteca.prestamosbiblioteca`):

```
book/         (web · domain · infra)   -> catálogo + cliente Open Library
loan/         (web · domain · infra)   -> préstamos, atrasos, bloqueos
reservation/  (web · domain · infra)   -> lista de espera con exclusividad
user/         (web · domain · infra)   -> cuentas, roles, activación
notification/ (domain · infra)         -> correos (Thymeleaf) + eventos + schedulers
shared/       (config · security · web · exception)
```

**Frontend** (`src/app`): `core/` (interceptors, guards, auth store), `domains/` (auth, books,
loans, reservations, admin), cada uno con su servicio y sus páginas; estado con **signals**.

---

## Decisiones de diseño (y por qué)

Cuando el enunciado dejaba algo ambiguo, se documenta acá la decisión tomada.

1. **Tres roles y flujo reserva → confirmación.** El enunciado nombraba sólo `ADMIN` y
   `BIBLIOTECARIO`, pero al modelar quién reserva, quién presta y quién recibe aparecían tres
   actores. Resolución (documentada como desvío del enunciado):
   - **`ADMIN`**: gestiona el catálogo (alta/edición/baja), cuentas bloqueadas, estadísticas y da de
     alta bibliotecarios. No presta ni reserva.
   - **`BIBLIOTECARIO`** (personal de mesa, lo crea el ADMIN): **confirma** las reservas y ve la lista
     paginada de **préstamos activos** (quién tiene cada libro). No tiene "mis préstamos".
   - **`USUARIO`** (se registra solo): **reserva** libros y recibe los préstamos; ve "mis préstamos".
   - **Flujo:** el `USUARIO` reserva un libro → queda **retenido** (`RESERVADO`) unos días; el
     `BIBLIOTECARIO` lo ve en el catálogo y **confirma el préstamo**, y recién ahí arranca el conteo
     de los 14 días. Los roles se controlan con `@PreAuthorize` por endpoint.
   - `borrowerName`/`borrowerEmail` en `Loan` quedan como snapshot del lector al confirmar; el bloqueo
     por atrasos vive en `AppUser`. La cuenta provisional con activación por correo sigue existiendo
     para cuentas creadas sin contraseña.

2. **JWT en cookie httpOnly + CSRF.** El token no vive en `localStorage` (inmune a robo por XSS):
   viaja en una cookie `HttpOnly; SameSite=Strict`. Como eso reabre el riesgo de CSRF, se habilita
   protección **double-submit** (`XSRF-TOKEN` / `X-XSRF-TOKEN`), que Angular maneja de forma nativa.
   El frontend se sirve **mismo origen** que la API (Nginx proxya `/api`), por eso no hay CORS.
   El flag `Secure` es `false` en la demo local (http) y `true` en prod (HTTPS).

3. **RestClient en vez de WebClient.** El backend es servlet (webmvc), sin WebFlux. `RestClient`
   (Spring 6+) es el cliente HTTP natural para consumir Open Library.

4. **Tolerancia a fallos de Open Library.** El lookup tiene timeout de 3s y cachea el resultado
   (Caffeine). Si la API no responde o falla, **el alta del libro no se rompe**: se guarda con los
   datos manuales y el enriquecimiento (portada, temas, año) simplemente no se aplica.

5. **Lista de espera con ventana de exclusividad (48h).** Al devolverse un libro con reservas, se
   notifica al **primero de la fila** (FIFO), el libro queda `RESERVADO` retenido para esa persona
   y solo ella puede convertir la reserva en préstamo. Si no lo retira en 48h, un job programado
   la expira y **promueve al siguiente**; si no queda nadie, el libro vuelve a `DISPONIBLE`.

6. **Endpoints añadidos** sobre la lista del enunciado (implícitos en las reglas):
   `POST /api/auth/activate`, `GET /api/auth/activation/{token}`, `POST /api/auth/logout`,
   `GET /api/auth/me`, `PUT /api/admin/users/{id}/unblock`, `GET /api/admin/users/blocked`.

7. **Spring Boot 4 (más nuevo que el 3.3+ pedido).** El proyecto ya venía scaffolded en Boot 4.0.8
   (Framework 7, Jackson 3, Hibernate 7.2). Notas de compatibilidad: no hay bean de
   `RestClient.Builder` (se usa `RestClient.builder()`), y Jackson 3 reemplaza al `ObjectMapper` de
   Jackson 2. `springdoc-openapi` 2.8.6 funciona correctamente sobre Boot 4.

---

## Reglas de negocio (resumen)

- Préstamo válido solo si el libro está `DISPONIBLE` (o `RESERVADO` a nombre del propio solicitante),
  el ISBN no está duplicado al dar de alta un libro, y la cuenta no está bloqueada.
- Al prestar: el libro pasa a `PRESTADO`, `dueDate = hoy + 14 días` y sale el correo de confirmación.
- Al devolver tarde: la cuenta suma un atraso. Con **3 atrasos en 90 días** queda **bloqueada 1 semana**
  y se le avisa por correo. Un `ADMIN` puede levantar el bloqueo.
- Excepciones de negocio: `BookNotAvailableException`, `DuplicateIsbnException`, `UserBlockedException`,
  `ExternalBookLookupException`. Todas se traducen a JSON uniforme vía `@RestControllerAdvice`.

---

## Testing

```bash
cd prestamosbiblioteca && ./mvnw test
```

Cubre las cuatro áreas pedidas:

- **Reglas de negocio** (Mockito): ISBN duplicado, préstamo/devolución, bloqueo por 3 atrasos, alta con enriquecimiento.
- **Endpoints** (MockMvc + Testcontainers): registro con cookie de sesión, catálogo protegido (401).
- **Correo** (GreenMail): la confirmación de préstamo se envía con el asunto y destinatario correctos.
- **Open Library** (MockWebServer): respuesta OK mapeada y **caso de fallo** (500 → se guarda igual).

> Los tests de integración usan **Testcontainers** (Postgres real), así que requieren Docker corriendo.

---

## Perfiles de configuración

- `dev` (default): Postgres/MailHog en `localhost`, cookie `Secure=false`.
- `prod`: todo por variables de entorno (sin defaults sensibles), cookie `Secure=true` por defecto.
  El `docker-compose` usa este perfil y setea `APP_COOKIE_SECURE=false` porque la demo corre sobre http.
- `test`: datasource provisto por Testcontainers, SMTP embebido de GreenMail.

---

## CI/CD e imágenes (GitHub Actions + GHCR)

El workflow `.github/workflows/ci.yml` corre en cada push/PR a `main`:

1. **Tests del backend** (`mvnw test`, con Testcontainers) y **build del frontend** (`bun run build`).
2. Sólo en push a `main`, **publica las imágenes** del backend y del frontend en **GHCR**
   (`ghcr.io/<owner>/prestamos-biblioteca-backend` y `-frontend`), con tags `latest` y el SHA.

**Imágenes livianas:** los `Dockerfile` son **multi-stage** — un stage compila (Maven / Bun) y el
stage final parte de una base mínima (**JRE 21** / **Nginx**) y **sólo copia el artefacto** (el `.jar`
o el `dist/`). El stage de build se descarta: no queda en la imagen final, así que ni el código fuente
ni las herramientas de build engordan la imagen.

### Correr en producción (sin descargar el código)

Con las imágenes ya publicadas, alcanza el `docker-compose.prod.yml` (usa `image:` de GHCR, no compila):

```bash
IMAGE_PREFIX=ghcr.io/<tu-usuario> JWT_SECRET=... APP_ADMIN_PASSWORD=... \
  docker compose -f docker-compose.prod.yml up -d
```

`IMAGE_PREFIX` es el owner de GHCR; `IMAGE_TAG` (opcional) fija una versión (por defecto `latest`).

---

## Estructura del repositorio

```
prestamosbiblioteca/          Backend Spring Boot (+ Dockerfile)
PrestamosBibliotecaFrontend/  Frontend Angular (+ Dockerfile + nginx.conf)
docker-compose.yml            Orquestación completa
.env.example                  Variables documentadas
postman/                      Colección Postman de la API
```
