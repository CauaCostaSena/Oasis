// Servidor estático de desenvolvimento. Node não faz parte do backend do Oásis.
// Expõe somente os arquivos públicos, sem servir backend, SQL, backups ou docs.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const types = { '.html':'text/html; charset=utf-8', '.css':'text/css; charset=utf-8', '.js':'text/javascript; charset=utf-8', '.svg':'image/svg+xml', '.png':'image/png', '.jpg':'image/jpeg' };
http.createServer((req, res) => {
    let pathname;
    try { pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname); }
    catch (_) { res.writeHead(400).end(); return; }
    if (pathname === '/') pathname = '/index.html';
    if (!/^\/(?:[a-z-]+\.html|(?:css|js|images)\/[^\\]+)$/.test(pathname)) { res.writeHead(404).end(); return; }
    const file = path.resolve(root, '.' + pathname);
    if (!file.startsWith(root + path.sep) || !types[path.extname(file)]) { res.writeHead(404).end(); return; }
    fs.readFile(file, (error, data) => {
        if (error) { res.writeHead(404).end(); return; }
        res.writeHead(200, { 'Content-Type':types[path.extname(file)], 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff' });
        res.end(data);
    });
}).listen(8090, '127.0.0.1', () => console.log('Frontend: http://127.0.0.1:8090'));
