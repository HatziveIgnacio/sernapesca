# Sernapesca Hub

Plataforma de gestión para Sernapesca, construida con **Angular** (Frontend), **NestJS** (Backend) y orquestada con **Docker**.

## Requisitos Previos

Asegúrate de tener instalados los siguientes programas en tu entorno de desarrollo:
- [Git](https://git-scm.com/)
- [Node.js](https://nodejs.org/es/) (v18 o superior)
- [Docker](https://www.docker.com/) y Docker Compose (opcional, pero recomendado)

## Instalación y Configuración

Sigue estos pasos para clonar e iniciar el proyecto localmente.

### Opción 1: Usar Docker (Recomendado)
Esta opción levanta automáticamente el frontend, el backend y la base de datos PostgreSQL.

1. Clona el repositorio:
   ```bash
   git clone https://github.com/HatziveIgnacio/sernapesca.git
   cd sernapesca
   ```

2. Construye y levanta los contenedores:
   ```bash
   docker-compose up -d --build
   ```

3. Acceso a las aplicaciones:
   - **Frontend (Angular)**: [http://localhost:4200](http://localhost:4200)
   - **Backend (NestJS API)**: [http://localhost:3000](http://localhost:3000)

*Para detener los contenedores, usa el comando: `docker-compose down`.*

---

### Opción 2: Ejecución Manual (Modo Desarrollo)

Si prefieres levantar los servicios manualmente para tener un entorno de desarrollo interactivo (hot-reloading, debbuging directo):

1. **Clona el repositorio:**
   ```bash
   git clone https://github.com/HatziveIgnacio/sernapesca.git
   cd sernapesca
   ```

2. **Inicia el Backend (NestJS):**
   Abre una terminal y ejecuta:
   ```bash
   cd backend
   npm install
   npm run start:dev
   ```
   *El backend estará corriendo en el puerto `3000`.*

3. **Inicia el Frontend (Angular):**
   Abre una **nueva** terminal y ejecuta:
   ```bash
   cd frontend
   npm install
   npm start
   ```
   *El frontend estará corriendo en el puerto `4200`.*

4. **Accede a la aplicación:** Abre tu navegador y ve a [http://localhost:4200](http://localhost:4200).

## Estructura del Proyecto

- `/frontend`: Aplicación cliente construida en Angular con diseño Vanilla CSS (cero dependencias visuales externas). Contiene el sistema de Layouts y componentes (`auth`, `dashboard`, `laboratorios`, etc.).
- `/backend`: API REST construida con NestJS. Estructurada modularmente reflejando la arquitectura de pantallas del frontend (`/auth`, `/dashboard`, `/laboratorios`).
- `docker-compose.yml`: Archivo de orquestación de microservicios y bases de datos.
