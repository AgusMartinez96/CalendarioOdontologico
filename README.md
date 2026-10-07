# Agenda odontológica

Aplicación web privada para que un consultorio odontológico administre pacientes y turnos. Incluye calendario mensual, semanal y diario, filtros, registro simple con usuario y contraseña (sin email) y persistencia en PostgreSQL. Cada cuenta ve y administra solo sus propios pacientes y turnos.

## Requisitos

- Java 17 para ejecutar y probar el backend.
- Node.js 22 o posterior y npm para compilar el frontend.
- PostgreSQL para desarrollo local, o Docker Engine con Docker Compose para ejecutar la aplicación completa.

## Ejecutar en Windows con PowerShell

El proyecto Maven está en `agenda-odontologica`. La opción más sencilla es Docker Compose:

```powershell
Set-Location .\agenda-odontologica
Copy-Item .env.example .env
notepad .env
docker compose up --build
```

En `.env`, reemplazá las dos contraseñas de ejemplo por valores propios. Abrí `http://localhost:8080` e iniciá sesión con los valores de `ADMIN_USERNAME` y `ADMIN_PASSWORD`. Los datos persisten en el volumen `postgres_data`.

Para detener los servicios sin borrar datos:

```powershell
docker compose down
```

No uses `docker compose down -v` si querés conservar la base.

### Ejecutar backend y frontend fuera de Docker

Necesitás un PostgreSQL en ejecución con una base y un usuario disponibles. Flyway aplica las migraciones al arrancar.

```powershell
Set-Location .\agenda-odontologica
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/odontologia"
$env:SPRING_DATASOURCE_USERNAME = "agenda"
$env:SPRING_DATASOURCE_PASSWORD = "tu-clave-local"
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "una-clave-segura"
Set-Location .\frontend
npm.cmd ci
npm.cmd run build
Set-Location ..
.\mvnw.cmd spring-boot:run
```

Para desarrollo visual con recarga en caliente, ejecutá Vite en otra terminal mientras el backend está en ejecución:

```powershell
Set-Location .\agenda-odontologica\frontend
npm.cmd ci
npm.cmd run dev
```

Vite deriva `/api` y `/actuator` al backend en el puerto 8080 durante el desarrollo.

## Pruebas y paquete de producción

Desde `agenda-odontologica`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
Set-Location .\frontend
npm.cmd run build
```

Las pruebas de persistencia usan H2 únicamente durante los tests. Producción utiliza PostgreSQL y Flyway.

## Variables de entorno

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Perfil `dev` o `prod`. |
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credenciales PostgreSQL. |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Usuario administrador; contraseña verificada con BCrypt y obligatoria. |
| `APP_TIME_ZONE` | Zona horaria predeterminada `America/Argentina/Buenos_Aires`; la API trabaja con instantes UTC. |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS permitidos separados por comas; predeterminado `http://localhost:5173`. |
| `LOGIN_MAX_ATTEMPTS` | Fallos permitidos por IP y usuario antes de bloquear el siguiente intento; predeterminado `5`. |
| `LOGIN_WINDOW_MINUTES` | Ventana para contar fallos; predeterminado `15` minutos. |
| `LOGIN_LOCK_MINUTES` | Duración del bloqueo tras alcanzar el límite; predeterminado `15` minutos. |
| `APPOINTMENT_DEFAULT_MINUTES` | Duración en minutos asignada a un turno nuevo; predeterminado `15`. Al editar un turno, la duración existente se conserva aunque cambie el inicio. |
| `REGISTRATION_ENABLED` | `true` (predeterminado) permite crear cuentas; con `false` el registro responde 403 y `/api/auth/config` informa `registrationOpen=false`. |
| `MAX_USERS` | Tope total de cuentas, incluido el admin; predeterminado `50`. Al llegar al tope el registro responde 409 «Registro cerrado por cupo». |
| `REGISTER_MAX_PER_IP_PER_HOUR` | Intentos de registro válidos por IP y hora; predeterminado `5`. Al superarlo responde 429 con `Retry-After`. |
| `MAX_PATIENTS_PER_USER` | Pacientes por cuenta; predeterminado `200`. Al superarlo responde 409. |
| `MAX_APPOINTMENTS_PER_USER` | Turnos por cuenta (incluye cancelados); predeterminado `2000`. Al superarlo responde 409. |
| `TRUSTED_PROXY_HOPS` | Cantidad de proxies propios delante de la app; predeterminado `0` (se usa solo la IP del socket y no se confía en `X-Forwarded-For`). Con `N > 0` se toma la entrada N desde la derecha de `X-Forwarded-For`. Ver «Limitaciones». |
| `PORT` | Puerto HTTP; predeterminado `8080`. |
| `VITE_APP_TIME_ZONE` | Zona del calendario al compilar el frontend; predeterminada `America/Argentina/Buenos_Aires`. |

La aplicación no tiene credenciales predeterminadas: `ADMIN_PASSWORD` debe configurarse antes del arranque. Copiá `.env.example` a `.env` y reemplazá sus contraseñas de ejemplo; `.env` está excluido de Git.

## API, seguridad y persistencia

- API REST en `/api/v1/patients` y `/api/v1/appointments`; autenticación de sesión en `/api/auth/**`.
- La API requiere sesión iniciada; `/api/auth/**` (login, registro, configuración, sesión, logout) y `/actuator/health` son públicos. Las escrituras, incluido el registro, están protegidas contra CSRF.
- Registro: `POST /api/auth/register` con `{username, password}`. Usuario de 3 a 30 caracteres (letras, números, punto, guion y guion bajo, empezando con letra o número), único sin distinguir mayúsculas. Contraseña de al menos 10 caracteres y como máximo 72 bytes en UTF-8 (límite de BCrypt), distinta del usuario y fuera de una lista embebida de contraseñas comunes; no se recortan espacios. Al registrarse se inicia sesión y se responde 201.
- Aislamiento: pacientes y turnos tienen dueño (`owner_id`) que sale siempre del usuario autenticado. Un recurso ajeno responde 404, también para el admin. El DNI es único por dueño y el solapamiento de turnos se evalúa solo entre turnos del mismo dueño.
- Migración V3/V4: los pacientes y turnos existentes pasan a pertenecer al admin. Al arrancar se crea o actualiza el admin con `ADMIN_USERNAME`/`ADMIN_PASSWORD`.
- Sesión: cambio de id de sesión al iniciar sesión, timeout de 30 minutos y logout que invalida. Cabeceras: `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Content-Security-Policy` y HSTS solo en `prod`.
- El inicio de sesión permite cinco fallos por combinación de IP y usuario dentro de la ventana; el sexto intento queda bloqueado durante el período configurado. El contador vive en memoria y se reinicia si la aplicación se reinicia; cada instancia mantiene su propio contador.
- Errores JSON uniformes: solicitud inválida 400, recurso inexistente 404 y conflictos de DNI/horario 409. Al crear un turno se calcula el fin con `APPOINTMENT_DEFAULT_MINUTES`; al cambiar el inicio de uno existente se conserva su duración previa.
- En `dev`, Swagger UI y `/v3/api-docs` requieren login con rol `ADMIN`; en `prod` ambos están deshabilitados. `/actuator/health` es público y solo informa el estado `UP` o `DOWN`.
- En `prod`, las cookies de sesión y CSRF usan `Secure` y `SameSite=Lax`; la cookie de sesión también es `HttpOnly`. Por eso el perfil de producción requiere HTTPS. La cookie `XSRF-TOKEN` no es `HttpOnly` para que el frontend pueda leerla.
- PostgreSQL corre migraciones Flyway en `agenda-odontologica/src/main/resources/db/migration`; Hibernate usa `ddl-auto=validate`.

## Decisiones

- Se conservó Maven, Spring Boot 3.4.7 y Java 17. React/Vite es la interfaz; `MainController` reenvía `/`, `/calendar` y `/patients` a `static/index.html`. Se retiraron los controladores y las plantillas Thymeleaf legados.
- La base PostgreSQL nueva usa tablas `patients` y `appointments`, separadas de las antiguas tablas MySQL. No se convierte ni modifica automáticamente una base MySQL existente; sus datos requieren una migración explícita aprobada.
- No se puede eliminar un paciente con turnos registrados. Los turnos se cancelan en lugar de borrarse.
- Los horarios se interpretan en la zona configurada, la API transmite instantes ISO-8601 y PostgreSQL los conserva como instantes UTC.

## Limitaciones

- **Sin email no hay recuperación de contraseña** ni cambio de contraseña: quien la olvida pierde el acceso a su cuenta. El administrador puede dar de baja la cuenta (los datos se pierden) y la persona puede registrarse de nuevo con otro usuario.
- Los límites de intentos (login y registro) y los contadores viven en memoria: se reinician con la aplicación y no se comparten entre instancias. Los topes por cuenta y `MAX_USERS` se comprueban antes de insertar, por lo que dos solicitudes simultáneas en el límite podrían excederlo por una unidad.
- Detrás de un proxy (Render), `getRemoteAddr()` suele ser la IP del proxy: con `TRUSTED_PROXY_HOPS=0` todos los usuarios comparten el contador por IP. Configurá `TRUSTED_PROXY_HOPS` con la cantidad real de proxies de tu plataforma solo si sabés que agregan la IP del cliente a `X-Forwarded-For`; un valor mayor que la cantidad real permitiría falsificar la IP.
- Una sesión ya iniciada de una cuenta deshabilitada sigue siendo válida hasta que vence (30 minutos de inactividad).

### Administrar cuentas con SQL (administrador)

Conectate a la base (Docker Compose: `docker compose exec db psql -U agenda -d odontologia`; Neon: editor SQL del panel). Reemplazá `nombre.de.usuario` por el usuario en minúsculas.

Deshabilitar una cuenta (impide iniciar sesión; conserva sus datos):

```sql
UPDATE users SET enabled = false
WHERE username_normalized = 'nombre.de.usuario' AND role = 'ROLE_USER';
-- Para reactivarla: enabled = true
```

Eliminar una cuenta y todos sus datos (irreversible; hacé un respaldo antes). El orden respeta las claves foráneas:

```sql
BEGIN;
DELETE FROM appointments WHERE owner_id = (SELECT id FROM users WHERE username_normalized = 'nombre.de.usuario' AND role = 'ROLE_USER');
DELETE FROM patients     WHERE owner_id = (SELECT id FROM users WHERE username_normalized = 'nombre.de.usuario' AND role = 'ROLE_USER');
DELETE FROM users        WHERE username_normalized = 'nombre.de.usuario' AND role = 'ROLE_USER';
COMMIT;
```

Eliminar la cuenta libera el nombre de usuario y un lugar del cupo `MAX_USERS`.

## Despliegue

Seguí [`agenda-odontologica/DEPLOY.md`](./agenda-odontologica/DEPLOY.md) para Render con Neon o Docker Compose en una PC/VPS.
