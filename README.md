# Sernapesca Hub

Plataforma de gestion para Sernapesca compuesta por:

- `frontend`: aplicacion Angular.
- `backend-java`: API Spring Boot / Java 21 actualmente en uso.

El directorio `backend/` existe en el repositorio como codigo legado de NestJS, pero no forma parte del flujo activo del proyecto.

## Stack vigente

- Frontend: Angular 21
- Backend: Spring Boot 3 / Java 21
- Build backend: Maven

## Requisitos previos

Instala estas herramientas antes de levantar el proyecto:

- Git
- Node.js 18+ y npm
- Java 21
- Maven 3.9+ o compatible

## Estructura del repositorio

- `frontend/`: cliente Angular con las vistas de login, dashboard, laboratorios y plantillas.
- `backend-java/`: backend Spring Boot que expone la API activa.
- `backend/`: backend NestJS antiguo, no usado actualmente.
- `docker-compose.yml`: archivo antiguo que aun referencia `backend/`; no representa el setup vigente.

## Puertos de desarrollo

- Frontend Angular: `http://localhost:4200`
- Backend Java: `http://localhost:8080`
- Base path API Java: `http://localhost:8080/api`

## Levantar el proyecto en desarrollo

1. Clona el repositorio:

   ```bash
   git clone https://github.com/HatziveIgnacio/sernapesca.git
   cd sernapesca
   ```

2. Levanta el backend Java en una terminal:

   ```bash
   cd backend-java
   mvn spring-boot:run
   ```

   El backend queda escuchando en `http://localhost:8080`.

3. Levanta el frontend en otra terminal:

   ```bash
   cd frontend
   npm install
   npm start
   ```

   El frontend queda disponible en `http://localhost:4200`.

## Configuracion importante

- El backend Java usa `backend-java/src/main/resources/application.properties`.
- El frontend apunta al backend Java mediante `frontend/src/environments/environment.ts`.
- El backend Java permite CORS desde `http://localhost:4200` y `http://localhost:4201`.

## Comandos utiles

Frontend:

```bash
cd frontend
npm start
npm run build
npm test -- --watch=false
```

Backend Java:

```bash
cd backend-java
mvn spring-boot:run
mvn test
mvn package
```

## Estado actual del proyecto

- La API activa es `backend-java`.
- `backend/` no debe usarse para levantar el entorno normal.
- `docker-compose.yml` todavia no fue actualizado al backend Java, por lo que hoy no es la opcion recomendada para desarrollo.
