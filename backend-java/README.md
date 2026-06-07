# Backend Java — SERNAPESCA

Spring Boot 3.5 · Java 21 · H2 (dev) · SQL Server 2017 (prod)

El backend corre en `http://localhost:8080` y expone la API REST bajo `/api`.

---

## Requisitos

| Herramienta | Versión mínima | Notas |
|---|---|---|
| Java JDK | 21 | Obligatorio. Java 17 no es compatible. |
| Maven | — | No necesitas instalarlo; `mvnw.cmd` lo descarga solo. |

### Verificar Java 21

```cmd
java -version
```

Debe mostrar `openjdk 21` o `java 21`. Si muestra otra versión, instala JDK 21 y configura `JAVA_HOME`.

---

## Levantar en modo desarrollo (Windows)

Abre una terminal **CMD** (no PowerShell) en la carpeta `backend-java/`:

```cmd
cd C:\ruta\al\repo\sernapesca\backend-java
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

**Primera ejecución:** `mvnw.cmd` descarga Maven 3.9.9 automáticamente (~10 MB). Las siguientes veces arranca directo.

El servidor está listo cuando ves:

```
Started SernapescaApplication in X.XXX seconds
```

### Si Java 21 no está en tu PATH

El `mvnw.cmd` incluido apunta a `C:\Java\jdk21.0.10_7`. Si tu JDK 21 está en otra ruta, edita la primera línea del archivo:

```bat
set JAVA_HOME=C:\ruta\a\tu\jdk21
```

---

## Levantar en modo desarrollo (macOS / Linux)

```bash
cd backend-java
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Si no tienes permisos de ejecución:

```bash
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Base de datos en desarrollo

El perfil `dev` usa **H2 en memoria** — no requiere instalar ninguna base de datos.

- Los datos se pierden al reiniciar el servidor (es normal en dev).
- Consola H2 disponible en: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:sernapesca`
  - Usuario: `sa` · Contraseña: *(vacía)*

---

## Usuario de prueba

La autenticación es por RUT. En desarrollo puedes usar:

| RUT | Rol |
|---|---|
| `12345678-9` | ADMINISTRADOR |

Endpoint de login:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{ "rut": "12345678-9" }
```

Devuelve un JWT que el frontend adjunta automáticamente en cada petición.

---

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Autenticación por RUT |
| GET | `/api/plantillas` | Listar plantillas Excel |
| POST | `/api/plantillas` | Subir plantilla |
| GET | `/api/periodos` | Listar períodos |
| POST | `/api/periodos` | Crear período |
| PUT | `/api/periodos/{id}/cerrar` | Cerrar período |

---

## Errores comunes

**`JAVA_HOME` apunta a Java 17**
Edita `mvnw.cmd` y pon la ruta correcta a tu JDK 21.

**Puerto 8080 ocupado**
Otro proceso usa el puerto. Ciérralo o cambia el puerto en `application.properties`:
```properties
server.port=8081
```

**`mvnw.cmd` no se reconoce**
Asegúrate de estar dentro de `backend-java/`, no en la raíz del repo ni en `backend/`.

**Error de conexión desde el frontend**
Verifica que el backend esté corriendo en `:8080` y que el frontend use `http://localhost:8080/api` en `environment.ts`.
