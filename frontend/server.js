#!/usr/bin/env node

/**
 * HTTP Server Configuration
 * Servidor de desarrollo para Frontend SERNAPESCA
 * 
 * Uso: npm run dev
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = process.env.PORT || 4200;
const PUBLIC_DIR = path.join(__dirname, 'public');

// MIME types
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
};

/**
 * Maneja cada request
 */
function handleRequest(req, res) {
  const parsedUrl = url.parse(req.url, true);
  let pathname = parsedUrl.pathname;

  // Ruta raíz → index.html
  if (pathname === '/') {
    pathname = '/index.html';
  }

  // Ruta completa del archivo
  let filePath = path.join(PUBLIC_DIR, pathname);

  // Seguridad: no permitir salir del directorio público
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('Forbidden');
    return;
  }

  // Si es directorio, intentar index.html
  fs.stat(filePath, (err, stats) => {
    if (err) {
      // Archivo no encontrado
      res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end('<h1>404 - Not Found</h1><p>Archivo no encontrado: ' + pathname + '</p>');
      console.log(`[404] ${pathname}`);
      return;
    }

    if (stats.isDirectory()) {
      // Intentar index.html en el directorio
      filePath = path.join(filePath, 'index.html');
      fs.readFile(filePath, (err, content) => {
        if (err) {
          res.writeHead(404, { 'Content-Type': 'text/plain' });
          res.end('404 - Not Found');
          console.log(`[404] ${pathname}index.html`);
          return;
        }

        const ext = path.extname(filePath);
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(content);
        console.log(`[200] ${pathname}index.html`);
      });
    } else {
      // Leer archivo
      fs.readFile(filePath, (err, content) => {
        if (err) {
          res.writeHead(500, { 'Content-Type': 'text/plain' });
          res.end('500 - Server Error');
          console.error(`[500] Error reading ${filePath}:`, err);
          return;
        }

        const ext = path.extname(filePath);
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';

        // Headers CORS para desarrollo
        res.writeHead(200, {
          'Content-Type': contentType,
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0',
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, OPTIONS',
        });

        res.end(content);
        console.log(`[200] ${pathname}`);
      });
    }
  });
}

// Crear servidor
const server = http.createServer(handleRequest);

server.listen(PORT, () => {
  console.log('\n╔════════════════════════════════════════════════════════════╗');
  console.log('║          SERNAPESCA Frontend Dev Server                    ║');
  console.log('╚════════════════════════════════════════════════════════════╝');
  console.log(`\n✅ Servidor escuchando en: http://localhost:${PORT}\n`);
  console.log(`📁 Sirviendo desde: ${PUBLIC_DIR}\n`);
  console.log('💡 Presiona CTRL+C para detener el servidor\n');
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`\n❌ Error: Puerto ${PORT} ya está en uso`);
    console.error(`   Intenta con otro puerto: PORT=4201 npm run dev\n`);
  } else {
    console.error('\n❌ Error del servidor:', err);
  }
  process.exit(1);
});

// Manejo de signals
process.on('SIGINT', () => {
  console.log('\n\n⏹️  Servidor detenido');
  process.exit(0);
});
