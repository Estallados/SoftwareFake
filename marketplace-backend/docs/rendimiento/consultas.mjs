// Cuenta las consultas SQL que lanza cada endpoint. Requiere: ./entorno.sh iniciar --sql
//   node consultas.mjs <usuarios.json> <app-sql.log> <salida.json>
import http from 'node:http';
import { readFileSync, writeFileSync } from 'node:fs';
const [archivo, log, salida] = process.argv.slice(2);
const u = JSON.parse(readFileSync(archivo, 'utf8')); const h = u.find(x => x.rol === 'HUESPED' && x.id > 1500).token;
const pedir = (ruta, token) => new Promise(ok => http.get({ host: 'localhost', port: 8081, path: ruta, headers: token ? { Authorization: 'Bearer ' + token } : {} }, r => { r.resume(); r.on('end', () => ok(r.statusCode)); }).on('error', e => ok(e.code)));
const lineas = () => readFileSync(log, 'latin1').split('\n').filter(l => l.includes('org.hibernate.SQL')).length;
const esperar = ms => new Promise(r => setTimeout(r, ms));
const casos = [['Búsqueda por ciudad (12)', '/alojamientos?ciudad=Cali&tamanio=12'], ['Búsqueda con fechas (12)', '/alojamientos?ciudad=Cali&llegada=2027-03-01&salida=2027-03-04&huespedes=2&tamanio=12'],
  ['Catálogo /disponibles (20)', '/alojamientos/disponibles?tamanio=20'], ['Detalle de alojamiento', '/alojamientos/10'], ['Disponibilidad', '/alojamientos/10/disponibilidad'], ['Reseñas', '/alojamientos/10/resenas'],
  ['Perfil (token)', '/usuarios/perfil', h], ['Notificaciones no leídas', '/notificaciones/noleidas', h], ['Mis reservas (10)', '/reservas/mias?tamanio=10', h]];
const res = [];
for (const [nombre, ruta, token] of casos) {
  await pedir(ruta, token); await esperar(300); const antes = lineas(); const estado = await pedir(ruta, token); await esperar(500);
  res.push({ nombre, ruta, estado, consultas: lineas() - antes }); console.log(nombre.padEnd(32), estado, res.at(-1).consultas);
}
writeFileSync(salida, JSON.stringify(res, null, 1));
