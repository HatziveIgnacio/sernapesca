# Fundamentos de la Arquitectura - Sernapesca

Este documento expone las justificaciones técnicas detrás de las decisiones arquitectónicas tomadas para el desarrollo del sistema de validación y carga de planillas para Sernapesca. El objetivo es garantizar que la solución sea escalable, mantenible y cumpla con los estándares de la industria y la Ingeniería de Software.

## 1. Separación de Responsabilidades (Frontend y Backend Independientes)

Se ha optado por no crear una aplicación monolítica, separando físicamente el Frontend y el Backend en proyectos independientes.
*   **Desacoplamiento:** Permite que los equipos (o miembros del equipo) puedan trabajar en paralelo sin generar conflictos en la base de código.
*   **Escalabilidad:** Si el procesamiento de Excel demanda mucha CPU, el backend puede escalarse independientemente del frontend.
*   **Seguridad:** La lógica de negocio pesada, la extracción de reglas de validación y las credenciales de la base de datos quedan completamente aisladas del cliente web.

## 2. NestJS (Backend)

NestJS es un framework progresivo de Node.js elegido por las siguientes razones:
*   **Arquitectura de Software Estricta:** Impone un uso de la Programación Orientada a Objetos (POO), Inyección de Dependencias y Módulos. Esto evita el código espagueti común en aplicaciones Express clásicas y es vital para sistemas empresariales complejos.
*   **Afinidad con Angular:** NestJS está fuertemente inspirado en Angular. Compartir la misma filosofía (Controladores, Servicios, Decoradores, TypeScript) reduce drásticamente la curva de aprendizaje si un desarrollador necesita trabajar en ambos lados del stack (Fullstack).
*   **Ecosistema:** Facilita enormemente la implementación de validaciones, autenticación y la configuración de Swagger.

## 3. Angular (Frontend)

Se seleccionó Angular como framework de lado del cliente debido a:
*   **Orientación Empresarial:** Es un framework "con opinión" (opinionated). A diferencia de React (que es una librería), Angular provee todo lo necesario por defecto (Enrutamiento, Formularios Reactivos, Cliente HTTP), estandarizando la forma en la que se desarrolla.
*   **Formularios Reactivos (Reactive Forms):** Esencial para los filtros dinámicos por laboratorio, mes y fecha requeridos. Maneja validaciones complejas de forma robusta.
*   **TypeScript:** Su fuerte dependencia de TypeScript garantiza que los datos que viajan hacia y desde el backend mantengan su integridad, reduciendo errores en tiempo de ejecución.

## 4. Orquestación con Docker & Docker Compose

Docker es el estándar de la industria para la contenedorización.
*   **Consistencia de Entornos:** Resuelve el clásico problema de "en mi máquina funciona". Cualquier desarrollador (o el profesor del taller) solo necesita ejecutar `docker-compose up` para tener la base de datos, el backend y el frontend funcionando con sus versiones exactas y dependencias correctas.
*   **Despliegue Simplificado (CI/CD):** Facilita la transición del proyecto a un entorno de producción, ya que los contenedores aseguran que el software correrá de forma idéntica sin importar el sistema operativo del servidor.

## 5. Prisma ORM (Persistencia)

Para la capa de datos (PostgreSQL/MySQL), Prisma ORM ofrece ventajas insuperables en el ecosistema TypeScript:
*   **Type-Safety End-to-End:** Prisma lee el esquema de la base de datos y genera tipos exactos. Si la tabla `LoadAttemptLogs` cambia, TypeScript alertará de los errores de compilación instantáneamente en el código de NestJS antes de probar la aplicación.
*   **Modelado Sencillo:** Definir la jerarquía de `Privilegios -> Perfiles -> Usuarios` se hace mediante un archivo de esquema declarativo y altamente legible, haciendo que las migraciones sean automáticas y predecibles.

## 6. Sincronización mediante OpenAPI/Swagger (Sin Nx)

El requerimiento de mantener las interfaces sincronizadas sin herramientas de monorepo pesado (como Nx) se resuelve mediante una estrategia impulsada por contratos:
*   **Single Source of Truth (SSOT):** El backend es la única fuente de la verdad. NestJS genera un contrato OpenAPI estandarizado que describe todos los endpoints, respuestas de error y modelos de datos.
*   **Generación de Código:** El frontend, utilizando el generador de cliente, lee este contrato y crea automáticamente los servicios de Angular y las interfaces TypeScript. Esto elimina el código repetitivo (boilerplate HTTP), evita errores por tipear mal el nombre de una propiedad y asegura que si el Backend rompe el contrato, el Frontend lo detecte en tiempo de compilación.
