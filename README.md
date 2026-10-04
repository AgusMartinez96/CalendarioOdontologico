# Agenda odontológica

Aplicación web privada para que un consultorio odontológico administre pacientes y turnos. Incluye calendario mensual, semanal y diario, filtros, inicio de sesión de administrador y persistencia en PostgreSQL.

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
| `LOGIN_MAX_ATTEMPTS` | Fallos permitidos por IP y usuario; predeterminado `5`. |
| `LOGIN_WINDOW_MINUTES` | Ventana para contar fallos; predeterminado `15` minutos. |
| `LOGIN_LOCK_MINUTES` | Duración del bloqueo tras alcanzar el límite; predeterminado `15` minutos. |
| `PORT` | Puerto HTTP; predeterminado `8080`. |
| `VITE_APP_TIME_ZONE` | Zona del calendario al compilar el frontend; predeterminada `America/Argentina/Buenos_Aires`. |

La aplicación no tiene credenciales predeterminadas: `ADMIN_PASSWORD` debe configurarse antes del arranque. Copiá `.env.example` a `.env` y reemplazá sus contraseñas de ejemplo; `.env` está excluido de Git.

## API, seguridad y persistencia

- API REST en `/api/v1/patients` y `/api/v1/appointments`; autenticación de sesión en `/api/auth/**`.
- La API requiere sesión de administrador; autenticación y `/actuator/health` son públicos. Las escrituras están protegidas contra CSRF.
- El inicio de sesión limita fallos por combinación de IP y usuario. El contador vive en memoria y se reinicia si la aplicación se reinicia; cada instancia mantiene su propio contador.
- Errores JSON uniformes: solicitud inválida 400, recurso inexistente 404 y conflictos de DNI/horario 409.
- En `dev`, Swagger UI y `/v3/api-docs` requieren login con rol `ADMIN`; en `prod` ambos están deshabilitados. `/actuator/health` es público y solo informa el estado `UP` o `DOWN`.
- En `prod`, las cookies de sesión y CSRF usan `Secure` y `SameSite=Lax`; la cookie de sesión también es `HttpOnly`. Por eso el perfil de producción requiere HTTPS. La cookie `XSRF-TOKEN` no es `HttpOnly` para que el frontend pueda leerla.
- PostgreSQL corre migraciones Flyway en `agenda-odontologica/src/main/resources/db/migration`; Hibernate usa `ddl-auto=validate`.

## Decisiones

- Se conservó Maven, Spring Boot 3.4.7 y Java 17. React/Vite es la interfaz; `MainController` reenvía `/`, `/calendar` y `/patients` a `static/index.html`. Se retiraron los controladores y las plantillas Thymeleaf legados.
- La base PostgreSQL nueva usa tablas `patients` y `appointments`, separadas de las antiguas tablas MySQL. No se convierte ni modifica automáticamente una base MySQL existente; sus datos requieren una migración explícita aprobada.
- No se puede eliminar un paciente con turnos registrados. Los turnos se cancelan en lugar de borrarse.
- Los horarios se interpretan en la zona configurada, la API transmite instantes ISO-8601 y PostgreSQL los conserva como instantes UTC.

## Despliegue

Seguí [`agenda-odontologica/DEPLOY.md`](./agenda-odontologica/DEPLOY.md) para Render con Neon o Docker Compose en una PC/VPS.
