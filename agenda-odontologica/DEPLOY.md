# Despliegue

## Render (Docker) + Neon (PostgreSQL)

El frontend y Spring Boot se despliegan juntos en un único servicio web; Neon proporciona PostgreSQL. Consultá los precios, límites, suspensión por inactividad, almacenamiento, cuotas y condiciones vigentes directamente en Render y Neon antes de crear cuentas: esos límites **no verificado**.

### 1. Crear la base PostgreSQL en Neon

1. Creá un proyecto PostgreSQL en Neon y seleccioná su región.
2. Copiá el host, nombre de base, usuario y contraseña que muestra el panel. No publiques la contraseña ni la guardes en Git.
3. Construí la URL JDBC con TLS habilitado:

   ```text
   jdbc:postgresql://HOST-NEON/NOMBRE_BASE?sslmode=require
   ```

   Reemplazá host y base por los que asignó Neon; usá aparte el usuario y la contraseña de tu instancia.

### 2. Crear el servicio web en Render

1. En Render, creá un **Web Service** conectado al repositorio.
2. Indicá el directorio raíz `CalendarioOdontologico/agenda-odontologica` (adaptá esta ruta a la ubicación real de la carpeta en tu repositorio).
3. Elegí **Docker** como runtime y usá el `Dockerfile` de ese directorio.
4. Configurá estas variables en **Environment**. Las contraseñas deben cargarse como secretos:

| Variable | Valor |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | URL JDBC de Neon terminada en `?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Usuario asignado por Neon |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de Neon (**secreto**) |
| `ADMIN_USERNAME` | Usuario administrador elegido por vos |
| `ADMIN_PASSWORD` | Contraseña robusta de administrador (**secreto**) |
| `APP_TIME_ZONE` | `America/Argentina/Buenos_Aires` |
| `CORS_ALLOWED_ORIGINS` | URL HTTPS pública del servicio; por ejemplo `https://tu-servicio.onrender.com` |
| `LOGIN_MAX_ATTEMPTS` | Fallos permitidos por IP y usuario antes de bloquear el siguiente intento; predeterminado `5` |
| `LOGIN_WINDOW_MINUTES` | Ventana del contador en minutos; predeterminado `15` |
| `LOGIN_LOCK_MINUTES` | Minutos de bloqueo tras alcanzar el límite; predeterminado `15` |
| `PORT` | No lo fijes manualmente si Render proporciona la variable `PORT` al contenedor. |

5. Reemplazá el ejemplo de URL en `CORS_ALLOWED_ORIGINS` por la dirección pública del servicio. No hace falta un sitio web separado.
6. Iniciá el despliegue. Flyway crea las tablas e índices automáticamente.
7. Verificá `/actuator/health`, abrí la URL HTTPS e iniciá sesión con las credenciales elegidas.

El perfil `prod` deshabilita Swagger UI y `/v3/api-docs`. Las cookies de sesión y CSRF se marcan `Secure` y `SameSite=Lax`; la cookie de sesión también lleva `HttpOnly` y `XSRF-TOKEN` permanece legible por el frontend. El servicio debe atenderse por HTTPS. Se permiten cinco fallos de login por IP y usuario dentro de la ventana; el sexto queda bloqueado. El contador se mantiene en memoria, se reinicia al reiniciar la aplicación y no se comparte entre instancias.

### 3. Comprobar la persistencia

Reiniciá el servicio web y verificá que pacientes y turnos se conserven. Los datos viven en Neon, separados del contenedor. Configurá los respaldos adecuados en la plataforma.

### Variables exactas de la aplicación

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST-NEON/NOMBRE_BASE?sslmode=require
SPRING_DATASOURCE_USERNAME=USUARIO-NEON
SPRING_DATASOURCE_PASSWORD=SECRETO-NEON
ADMIN_USERNAME=USUARIO-ADMIN
ADMIN_PASSWORD=SECRETO-ADMIN
APP_TIME_ZONE=America/Argentina/Buenos_Aires
CORS_ALLOWED_ORIGINS=https://TU-SERVICIO.onrender.com
LOGIN_MAX_ATTEMPTS=5
LOGIN_WINDOW_MINUTES=15
LOGIN_LOCK_MINUTES=15
PORT=inyectada-por-Render
```

Reemplazá los valores de ejemplo; no cargues `PORT` manualmente cuando Render ya lo inyecta.

## Docker Compose en VPS o PC propia

1. Instalá Docker Engine y Docker Compose en la máquina.
2. Copiá el proyecto y entrá al directorio `agenda-odontologica`.
3. Copiá el ejemplo privado de variables y reemplazá las contraseñas por claves únicas:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

4. Arrancá PostgreSQL y la aplicación:

   ```powershell
   docker compose up --build -d
   docker compose ps
   docker compose logs app
   ```

5. Abrí `http://IP-DE-LA-MAQUINA:8080`; antes de publicar el servicio en Internet, configurá un proxy inverso con HTTPS.
6. El volumen `postgres_data` preserva los datos al recrear contenedores:

   ```powershell
   docker compose down
   docker compose up -d
   ```

   No ejecutes `docker compose down -v` si querés conservar los datos. Configurá firewall, TLS, respaldos y actualizaciones del sistema para un servicio expuesto a Internet.
