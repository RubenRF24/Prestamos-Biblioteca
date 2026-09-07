# Prueba Técnica — Fullstack Java

## Sistema de préstamos para una biblioteca

En la biblioteca, los préstamos se llevan en una planilla de Excel que vive en un solo computador. Nadie puede saber si un libro está disponible sin llamar y preguntar, los atrasos no se le avisan a nadie hasta que alguien se acuerda de revisar la planilla, y cuando alguien quiere un libro que ya está prestado termina preguntando cada semana si ya lo devolvieron.

La tarea es construir, de principio a fin, la aplicación que reemplace eso: un backend en Spring Boot con reglas de negocio reales sobre préstamos y atrasos, un frontend que la gente use sin pensarlo, y un servicio de correo que avise antes de que venza un préstamo — apoyado en una API externa real que completa los datos de un libro con solo el ISBN.

## Lo esencial

| Campo | Detalle |
|---|---|
| **Nivel** | Semi-senior / Senior Fullstack |
| **Duración sugerida** | 8 – 10 horas (repartibles en varias sesiones) |
| **Entrega** | Repositorio Git (público o zip) + README + colección Postman/Insomnia |
| **Puntaje total** | 100 puntos (ver desglose al final) |

## Con qué se construye

- **Java 21** y **Spring Boot 3.3+** (Web, Data JPA, Validation, Security, Mail, Cache)
- **PostgreSQL** como base de datos
- **Flyway** para migraciones versionadas
- **Spring Mail** con plantillas **Thymeleaf** para el correo
- **WebClient** o **RestClient** para consultar la API de libros
- **Frontend libre**: React, Angular o Vue, con TypeScript
- **Docker Compose** con Postgres y **MailHog** (SMTP de mentira con interfaz web para ver los correos que la aplicación realmente envía) para pruebas integradas

---

## 1. Modelo de datos

No hace falta un modelo complicado, pero sí se necesitan estas cuatro piezas trabajando juntas: los **libros** del catálogo, los **préstamos** que se hacen sobre ellos, las **cuentas** de quienes los usan y una **lista de espera** para cuando alguien quiere un libro que ya está prestado.

### `Loan` — el centro de todo

```java
@Entity
public class Loan {
    // id, book (ManyToOne), borrowerName, borrowerEmail
    // loanDate, dueDate (loanDate + 14 días), returnDate (nullable)
    // reminderSentAt (nullable) -> evita mandar el recordatorio dos veces
    // overdueNoticeSentAt (nullable) -> evita repetir el aviso de vencido
}
```

### Las otras tres, en resumen

- **`Book`** — título, autor, ISBN (único), año de publicación, estado (`DISPONIBLE`, `PRESTADO`, `RESERVADO`) y, cuando viene enriquecido desde la API externa, portada y temas.
- **`AppUser`** — nombre, correo (único), contraseña (hasheada), rol (`ADMIN` o `BIBLIOTECARIO`) y si está bloqueado temporalmente para pedir préstamos.
- **`Reservation`** — libro, correo de quien espera, fecha de solicitud y estado (`PENDIENTE`, `NOTIFICADO`, `CANCELADO`, `CUMPLIDO`).

---

## 2. Reglas de negocio

Esta es la parte que separa un CRUD de una aplicación real.

- Al registrar un préstamo, validar que:
  - el libro esté `DISPONIBLE`
  - el ISBN no esté duplicado al registrar un libro nuevo
  - la persona no tenga la cuenta bloqueada
- Si todo está en orden:
  - el libro pasa a `PRESTADO`
  - se calcula `dueDate` a 14 días
  - se dispara el correo de confirmación
- Al devolver un libro después de la fecha límite, la cuenta acumula un **atraso**.
  - Al llegar a **tres atrasos en los últimos 90 días**, la cuenta queda **bloqueada** para pedir nuevos préstamos durante **una semana** y se le avisa por correo.
  - Un `ADMIN` puede levantar el bloqueo antes si hace falta.
- Cuando un libro se devuelve y hay alguien esperando exactamente ese título:
  - se notifica automáticamente al primero de la fila
  - el libro pasa a `RESERVADO` en vez de volver a `DISPONIBLE`

### Excepciones de negocio (mínimo requerido)

- `BookNotAvailableException`
- `DuplicateIsbnException`
- `UserBlockedException`
- `ExternalBookLookupException` (para cuando falle la API externa del punto 4)

---

## 3. Notificaciones por correo

**Uno de los dos puntos que más pesan en la evaluación.** Se necesita un servicio de correo real (no un `System.out.println` disfrazado), con al menos estos cuatro envíos:

1. **Confirmación** al registrar el préstamo, con el libro, la fecha y la fecha límite de devolución.
2. **Recordatorio automático** uno o dos días antes de que venza — esto exige una tarea programada que revise diariamente qué préstamos están por vencer.
3. **Aviso de bloqueo** cuando alguien llega a los tres atrasos.
4. **Libro disponible**, para quien estaba primero en la lista de espera cuando alguien lo devuelve.

El envío **no debe bloquear la petición HTTP** que lo origina: se publica un evento de dominio al crear el préstamo y se procesa de forma asíncrona.

```java
public record LoanCreatedEvent(Long loanId) {}

@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onLoanCreated(LoanCreatedEvent event) {
    notificationService.sendLoanConfirmation(event.loanId());
}

// La tarea de recordatorios corre una vez al día:
@Scheduled(cron = "0 0 8 * * *")
public void sendDueSoonReminders() { ... }
```

Usar plantillas HTML con **Thymeleaf** (en `templates/email/`) en vez de armar el cuerpo del correo a mano, y probar todo en local contra **MailHog** — así se ve exactamente lo que le llegaría a un usuario real sin necesidad de credenciales SMTP.

---

## 4. Autocompletar libros: integración con una API externa

**El segundo punto que más pesa.** Al registrar un libro con solo el ISBN, la aplicación debe poder completar título, autor, año y portada consultando una API pública y gratuita — por ejemplo, **Open Library**, que no requiere API key:

```
GET https://openlibrary.org/isbn/{isbn}.json
GET https://covers.openlibrary.org/b/isbn/{isbn}-L.jpg  (portada)
```

El resultado para un mismo ISBN no cambia casi nunca, así que tiene sentido guardarlo en **caché** (Caffeine en memoria es suficiente) en vez de golpear la API cada vez que alguien lo busca.

Si el servicio externo no responde a tiempo o falla, **el registro del libro no debe romperse**: se guarda con los datos que la persona escribió a mano, y esto se documenta en el README.

```java
@Cacheable(cacheNames = "openLibraryLookup", key = "#isbn")
public Optional<ExternalBookData> lookupByIsbn(String isbn) {
    try {
        OpenLibraryResponse resp = webClient.get()
            .uri("/isbn/{isbn}.json", isbn)
            .retrieve()
            .bodyToMono(OpenLibraryResponse.class)
            .timeout(Duration.ofSeconds(3))
            .block();
        return Optional.ofNullable(resp).map(this::toExternalBookData);
    } catch (Exception e) {
        return Optional.empty(); // el libro se guarda igual, con los datos manuales
    }
}
```

Exponer además un `GET /api/books/lookup/{isbn}` que devuelva la previsualización **sin guardar nada**, para que el frontend muestre un botón "Autocompletar desde ISBN" antes de confirmar el formulario.

---

## 5. API REST

```
POST   /api/auth/register
POST   /api/auth/login

GET    /api/books
GET    /api/books/lookup/{isbn}   -> preview desde Open Library
POST   /api/books                 -> ADMIN
DELETE /api/books/{id}            -> ADMIN, solo si DISPONIBLE

POST   /api/loans
GET    /api/loans/mine
PUT    /api/loans/{id}/return

POST   /api/reservations
DELETE /api/reservations/{id}

GET    /api/admin/stats           -> ADMIN
```

Manejar los errores de forma consistente con un `@RestControllerAdvice` (código, mensaje y ruta en JSON) y documentar la API con **springdoc-openapi**.

### Seguridad

- Login con **JWT** sobre **Spring Security**.
- Cualquiera autenticado puede ver el catálogo y pedir préstamos.
- Solo un `ADMIN` puede registrar libros, eliminarlos o ver las estadísticas globales.
- Contraseñas siempre con **BCrypt**, nunca en texto plano.

---

## 6. Frontend

**No es un extra: es parte del entregable.** Construir al menos estas pantallas, con manejo real de estados de carga y error (nada de tragarse un 500 en silencio):

- **Login.**
- **Catálogo** con búsqueda — por título, autor o estado, con el botón de "Autocompletar desde ISBN" en el formulario de alta.
- **Mis préstamos** — con opción de devolver y ver si está vencido.
- **Panel de administración** — libros, cuentas bloqueadas y un par de estadísticas (prestados, vencidos).

Requisitos adicionales:
- Un servicio HTTP centralizado que agregue el token en cada llamada.
- Formularios con validación que reflejen las mismas reglas del backend.
- Algún tipo de manejo de estado (Context/Redux/Zustand en React, Pinia en Vue, Signals/Services en Angular).

---

## 7. Testing, Docker y entrega

### Testing
Cubrir con pruebas automatizadas:
- Las reglas de negocio del punto 2 (ISBN duplicado, préstamo y devolución, bloqueo por atrasos).
- Al menos un par de endpoints con **MockMvc**.
- El servicio de correo — **GreenMail** levanta un SMTP embebido perfecto para verificar que el correo de confirmación de verdad se envía, con el asunto y destinatario correctos.
- El cliente de Open Library — simular sus respuestas con **WireMock** o **MockWebServer**, incluyendo el caso en que la API falla.

### Docker y entrega
- Entregar un `docker-compose.yml` con la app, PostgreSQL y MailHog, para que levantar todo sea un solo comando.
- El README debe explicar:
  - cómo correrlo
  - qué variables de entorno existen
  - cómo entrar con un usuario ADMIN de prueba
  - dónde ver los correos enviados (MailHog en `localhost:8025`)

---

## 8. Cómo se evalúa

| Área | Puntos |
|---|---|
| Modelo de datos y persistencia (migraciones incluidas) | 12 |
| Reglas de negocio (préstamos, atrasos, bloqueos, lista de espera) | 18 |
| Notificaciones por correo (asíncronas + programadas + plantillas) | 18 |
| Integración con Open Library (caché y manejo de fallos) | 14 |
| API REST y seguridad (JWT, roles, errores consistentes) | 14 |
| Frontend (funcionalidad, estado, experiencia de uso) | 12 |
| Testing automatizado | 8 |
| Docker, documentación y calidad general del código | 4 |
| **Total** | **100** |

### Notas finales del cliente

- Si algo no queda claro, documentar en el README la decisión tomada en vez de dejarlo sin resolver: se prefiere ver cómo se piensa a que se adivine lo que se quería.
- **Regla dura única**: nada de credenciales o secretos subidos al repositorio.

---

*Fuente: Ezertech · Prueba Técnica de Ingreso — Confidencial, uso exclusivo del proceso de selección.*
