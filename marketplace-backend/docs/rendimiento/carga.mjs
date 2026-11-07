// Generador de carga sin dependencias (Node 22). Modelo cerrado (usuarios virtuales con tiempo de espera)
// y modelo abierto (tasa de llegada constante: no se frena cuando el servidor se pone lento).
//   node carga.mjs <escenario> [base_url] [usuarios.json] [carpeta_resultados]
// Escenarios: humo | carga | estres | pico | login | login_fallido | resistencia | concurrencia
// Variables: BASE_RPS, PICO_RPS, RESISTENCIA_RPS, RESISTENCIA_MIN.
import http from 'node:http';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { monitorEventLoopDelay, performance } from 'node:perf_hooks';
import { randomUUID } from 'node:crypto';

const [escenario = 'humo', BASE = 'http://localhost:8081', archivoUsuarios = '/tmp/marketplace-carga/usuarios.json',
  salida = new URL('./resultados', import.meta.url).pathname] = process.argv.slice(2);
const destino = new URL(BASE);
if (!['localhost', '127.0.0.1'].includes(destino.hostname)) throw new Error('Estas pruebas solo corren contra localhost');
const TIMEOUT_MS = 10000, MAX_EN_VUELO = 3000;
const agente = new http.Agent({ keepAlive: true, maxSockets: 4000 });
const usuarios = JSON.parse(readFileSync(archivoUsuarios, 'utf8'));
const huespedes = usuarios.filter(u => u.rol === 'HUESPED');
const CIUDADES = ['Bogotá', 'Medellín', 'Cartagena', 'Cali', 'Santa Marta', 'Manizales', 'Barranquilla', 'Bucaramanga', 'Pereira', 'Villa de Leyva'];
const TIPOS = ['Apartamento', 'Casa', 'Cabaña', 'Estudio', 'Finca'];
const azar = n => Math.floor(Math.random() * n);
const de = l => l[azar(l.length)];
const fecha = dias => new Date(Date.now() + dias * 864e5).toISOString().slice(0, 10);
const esperar = ms => new Promise(r => setTimeout(r, ms));

function pedir(metodo, ruta, { token, cuerpo, cabeceras } = {}) {
  return new Promise(ok => {
    const datos = cuerpo === undefined ? null : JSON.stringify(cuerpo);
    const inicio = performance.now();
    const req = http.request({ host: destino.hostname, port: destino.port, path: ruta, method: metodo, agent: agente, timeout: TIMEOUT_MS,
      headers: { Accept: 'application/json', ...(datos ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(datos) } : {}),
        ...(token ? { Authorization: 'Bearer ' + token } : {}), ...cabeceras } }, res => {
      const trozos = []; res.on('data', t => trozos.push(t));
      res.on('end', () => ok({ estado: res.statusCode, ms: performance.now() - inicio, cuerpo: Buffer.concat(trozos).toString() }));
      res.on('error', e => ok({ estado: 0, ms: performance.now() - inicio, error: e.code || 'ERROR_RESPUESTA' }));
    });
    req.on('timeout', () => req.destroy(Object.assign(new Error('t'), { code: 'TIMEOUT' })));
    req.on('error', e => ok({ estado: 0, ms: performance.now() - inicio, error: e.code || e.message }));
    if (datos) req.write(datos);
    req.end();
  });
}

// ---- Métricas por etapa y operación -------------------------------------------------
const etapas = []; let etapa = null;
function nuevaEtapa(nombre, objetivo) { etapa = { nombre, objetivo, inicio: performance.now(), ops: new Map(), descartadas: 0, retrasoMaxMs: 0 }; etapas.push(etapa); }
function anotar(op, r, esperados) {
  if (!etapa) return;
  let o = etapa.ops.get(op); if (!o) etapa.ops.set(op, o = { ms: [], ok: 0, err: 0, estados: {} });
  o.ms.push(r.ms);
  const clave = r.error || String(r.estado); o.estados[clave] = (o.estados[clave] || 0) + 1;
  esperados.includes(r.estado) ? o.ok++ : o.err++;
}
async function op(nombre, metodo, ruta, opciones = {}, esperados = [200]) {
  const r = await pedir(metodo, ruta, opciones); anotar(nombre, r, esperados); return r;
}
const pct = (o, p) => o.length ? o[Math.min(o.length - 1, Math.floor(o.length * p))] : 0;
function resumen(ms, ok, err, seg) {
  const o = [...ms].sort((a, b) => a - b); const r = v => Math.round(v * 10) / 10;
  return { n: ms.length, rps: r(ms.length / seg), ok, err, errPct: r(100 * err / Math.max(1, ms.length)),
    p50: r(pct(o, .5)), p90: r(pct(o, .9)), p95: r(pct(o, .95)), p99: r(pct(o, .99)), max: r(o.at(-1) || 0) };
}
function cerrarEtapa() {
  if (!etapa) return null;
  const seg = (performance.now() - etapa.inicio) / 1000; const ms = []; let ok = 0, err = 0; const por = {}; const estados = {};
  for (const [k, o] of etapa.ops) { for (const v of o.ms) ms.push(v); ok += o.ok; err += o.err; por[k] = { ...resumen(o.ms, o.ok, o.err, seg), estados: o.estados };
    for (const [e, n] of Object.entries(o.estados)) estados[e] = (estados[e] || 0) + n; }
  etapa.resultado = { nombre: etapa.nombre, objetivo: etapa.objetivo, segundos: Math.round(seg), ...resumen(ms, ok, err, seg), descartadas: etapa.descartadas, retrasoMaxMs: Math.round(etapa.retrasoMaxMs), estados, operaciones: por };
  const r = etapa.resultado;
  console.log(`${r.nombre.padEnd(20)} obj=${String(r.objetivo).padEnd(7)} rps=${String(r.rps).padEnd(7)} p50=${String(r.p50).padEnd(8)} p95=${String(r.p95).padEnd(8)} p99=${String(r.p99).padEnd(8)} err=${r.errPct}% desc=${r.descartadas} ${JSON.stringify(estados)}`);
  etapa = null; return r;
}

// ---- Telemetría del backend ----------------------------------------------------------
const admin = await iniciarAdmin();
const serie = []; const lazo = monitorEventLoopDelay({ resolution: 20 }); lazo.enable();
async function metrica(nombre, seleccion = 'VALUE') {
  const r = await pedir('GET', '/actuator/metrics/' + nombre, { token: admin });
  try { return JSON.parse(r.cuerpo).measurements.find(m => m.statistic === seleccion)?.value ?? null; } catch { return null; }
}
async function iniciarAdmin() {
  const r = await pedir('POST', '/auth/login', { cuerpo: { correo: 'admin@carga.local', contrasenia: 'Carga#2026' } });
  if (r.estado !== 200) throw new Error('El login del administrador falló: ' + r.estado + ' ' + r.cuerpo);
  return JSON.parse(r.cuerpo).token;
}
// CPU real de cada proceso (100 % = un núcleo), leída de /proc: separa backend de base de datos.
const TRABAJO = archivoUsuarios.replace(/\/[^/]*$/, ''); const previo = {};
function cpuProceso(nombre) {
  try {
    const pid = readFileSync(`${TRABAJO}/${nombre}.pid`, 'utf8').trim();
    const c = readFileSync(`/proc/${pid}/stat`, 'utf8').split(') ')[1].split(' ');
    const tics = +c[11] + +c[12], ahora = performance.now(); const p = previo[nombre]; previo[nombre] = { tics, ahora };
    return p ? Math.round((tics - p.tics) * 1000 / (ahora - p.ahora)) : null;
  } catch { return null; }
}
const telemetria = setInterval(async () => {
  const [hilos, hikPend, hikAct, heap, cpu] = await Promise.all(['tomcat.threads.busy', 'hikaricp.connections.pending', 'hikaricp.connections.active', 'jvm.memory.used?tag=area:heap', 'process.cpu.usage'].map(n => metrica(n)));
  serie.push({ t: Math.round(performance.now() / 1000), cpuBackend: cpuProceso('app'), cpuH2: cpuProceso('h2'), etapa: etapa?.nombre, hilos, hikPend, hikAct, heapMB: heap && Math.round(heap / 1e6), cpuPct: cpu && Math.round(cpu * 100), retrasoGeneradorMs: Math.round(lazo.max / 1e6) });
  lazo.reset();
}, 5000); telemetria.unref();

// ---- Mezcla de tráfico (viaje del usuario en el marketplace) ---------------------------
let ranura = 0;
const planes = [
  [22, () => op('buscar_ciudad', 'GET', `/alojamientos?ciudad=${encodeURIComponent(de(CIUDADES))}&pagina=${azar(5)}&tamanio=12`)],
  [13, () => { const d = 20 + azar(200); return op('buscar_fechas', 'GET', `/alojamientos?ciudad=${encodeURIComponent(de(CIUDADES))}&llegada=${fecha(d)}&salida=${fecha(d + 3)}&huespedes=2&tamanio=12`); }],
  [5, () => op('buscar_filtros', 'GET', `/alojamientos?tipo=${encodeURIComponent(de(TIPOS))}&precioMinimo=100000&precioMaximo=400000&amenidades=wifi&orden=precio_menor&tamanio=12`)],
  [12, () => op('disponibles', 'GET', `/alojamientos/disponibles?pagina=${azar(5)}&tamanio=20`)],
  [3, () => op('mejor_calificados', 'GET', '/alojamientos/disponibles?orden=mejor_calificados&tamanio=20')],
  [18, () => op('detalle', 'GET', `/alojamientos/${1 + azar(4000)}`)],
  [7, () => op('disponibilidad', 'GET', `/alojamientos/${1 + azar(4000)}/disponibilidad`)],
  [5, () => op('resenas', 'GET', `/alojamientos/${1 + azar(4000)}/resenas`)],
  [3, () => op('tipos', 'GET', '/alojamientos/tipos')],
  [3, () => op('perfil', 'GET', '/usuarios/perfil', { token: de(huespedes).token })],
  [3, () => op('noleidas', 'GET', '/notificaciones/noleidas', { token: de(huespedes).token })],
  [3, () => op('mis_reservas', 'GET', '/reservas/mias?tamanio=10', { token: de(huespedes).token })],
  [2, () => cotizarYReservar(false)],
  [1, () => cotizarYReservar(true)],
];
const totalPeso = planes.reduce((s, p) => s + p[0], 0);
function elegir() { let x = Math.random() * totalPeso; for (const [p, f] of planes) if ((x -= p) < 0) return f(); }
async function cotizarYReservar(reservar) {
  // Fechas propias de cada intento: aquí se mide el camino feliz, no el choque de fechas.
  const n = ranura++; const u = de(huespedes); const dia = 20 + (n % 150) * 4;
  const c = await op('cotizar', 'POST', '/cotizaciones', { token: u.token, cuerpo: { idAlojamiento: 1 + (Math.floor(n / 150) % 4000), fechaEntrada: fecha(dia), fechaSalida: fecha(dia + 3), huespedes: 2 } }, [201]);
  if (!reservar || c.estado !== 201) return;
  await op('reservar', 'POST', '/reservas', { token: u.token, cabeceras: { 'Idempotency-Key': randomUUID() }, cuerpo: { idCotizacion: JSON.parse(c.cuerpo).idCotizacion, metodoPago: 'pm_card_visa' } }, [201]);
}

// ---- Modelos de carga --------------------------------------------------------------------
async function cerrado(fases, espera = [1000, 3000], accion = elegir) {
  let objetivo = 0, activos = 0, fin = false; const hilos = [];
  const vu = async id => { while (!fin) { if (id >= objetivo) { await esperar(250); continue; } await accion().catch(() => {}); await esperar(espera[0] + Math.random() * (espera[1] - espera[0])); } };
  for (const f of fases) {
    nuevaEtapa(f.nombre, f.vus + ' VU'); const desde = objetivo; const t0 = performance.now(); const pasos = Math.max(1, (f.rampa || 0) / 1000);
    for (let i = 1; i <= pasos; i++) { objetivo = Math.round(desde + (f.vus - desde) * i / pasos); while (activos < objetivo) hilos.push(vu(activos++)); if (f.rampa) await esperar(1000); }
    await esperar(f.seg * 1000 - (performance.now() - t0)); cerrarEtapa();
  }
  fin = true; await Promise.race([Promise.all(hilos), esperar(12000)]);
}
async function abierto(nombre, rps, seg, accion = elegir) {
  nuevaEtapa(nombre, rps + ' rps'); const e = etapa; let enVuelo = 0; const t0 = performance.now(); let lanzadas = 0;
  while (true) {
    const t = performance.now() - t0; if (t >= seg * 1000) break;
    const debe = Math.floor(t / 1000 * rps);
    if (debe - lanzadas > 0) e.retrasoMaxMs = Math.max(e.retrasoMaxMs, (debe - lanzadas) / rps * 1000);
    while (lanzadas < debe) { lanzadas++; if (enVuelo >= MAX_EN_VUELO) { e.descartadas++; continue; } enVuelo++; accion().catch(() => {}).finally(() => enVuelo--); }
    await esperar(2);
  }
  const fin = performance.now() + TIMEOUT_MS; while (enVuelo > 0 && performance.now() < fin) await esperar(50);
  return cerrarEtapa();
}

// ---- Escenarios --------------------------------------------------------------------------
const MIN = 60;
const escenarios = {
  humo: () => cerrado([{ nombre: 'humo', vus: 5, seg: 30 }]),
  carga: () => cerrado([
    { nombre: 'rampa_50', vus: 50, seg: MIN, rampa: 30000 }, { nombre: 'normal_50', vus: 50, seg: 3 * MIN },
    { nombre: 'rampa_200', vus: 200, seg: MIN, rampa: 30000 }, { nombre: 'alta_200', vus: 200, seg: 3 * MIN },
    { nombre: 'rampa_400', vus: 400, seg: MIN, rampa: 30000 }, { nombre: 'punta_400', vus: 400, seg: 3 * MIN }]),
  async estres() {
    for (const rps of [50, 100, 150, 200, 225, 250, 275, 300, 350, 400, 500, 600, 800, 1000, 1500, 2000]) {
      const r = await abierto(`escalon_${rps}`, rps, MIN);
      if (r.errPct > 20 || r.descartadas > rps * 5) { console.log('Punto de quiebre: se detiene la subida.'); break; }
    }
    await esperar(5000);
    await abierto('recuperacion_1', 50, MIN); await abierto('recuperacion_2', 50, MIN);
  },
  async pico() {
    const base = +process.env.BASE_RPS || 50, p = +process.env.PICO_RPS || 1000;
    await abierto('antes', base, MIN); await abierto('pico', p, 30); await abierto('despues_1', base, MIN); await abierto('despues_2', base, MIN);
  },
  async login() {
    const a = () => op('login_admin', 'POST', '/auth/login', { cuerpo: { correo: 'admin@carga.local', contrasenia: 'Carga#2026' } });
    for (const rps of [5, 10, 20, 40, 80, 120, 160, 240]) { const r = await abierto(`login_${rps}`, rps, 30, a); if (r.errPct > 20) break; }
  },
  async login_fallido() {
    // Fuerza bruta distribuida: mide si una contraseña mala cuesta CPU y si algo la frena.
    const f = () => op('login_fallido', 'POST', '/auth/login', { cuerpo: { correo: de(huespedes).correo, contrasenia: 'Mala#' + azar(1e6) } }, [401]);
    const p = () => op('busqueda_victima', 'GET', `/alojamientos/${1 + azar(4000)}`);
    await abierto('solo_lecturas', 100, 30, p);
    for (const rps of [50, 100, 200]) await Promise.all([abierto(`ataque_${rps}`, rps + 100, 30, () => Math.random() < 100 / (100 + rps) ? p() : f())]);
  },
  async resistencia() {
    const rps = +process.env.RESISTENCIA_RPS || 100, min = +process.env.RESISTENCIA_MIN || 15;
    // RESISTENCIA_RAMPA=1 sube como lo haría un día normal; sin ella se mide el golpe en frío tras un arranque.
    if (process.env.RESISTENCIA_RAMPA) for (const r of [25, 50, 75, 100, 125]) await abierto(`rampa_${r}`, r, MIN);
    for (let i = 1; i <= min / 5; i++) await abierto(`tramo_${i * 5}min`, rps, 5 * MIN);
  },
  async concurrencia() {
    // N huéspedes reservan a la vez las mismas fechas del mismo alojamiento: debe ganar exactamente uno.
    nuevaEtapa('doble_reserva', '100 x 20 rondas'); const rondas = []; const rep = {};
    for (let ronda = 0; ronda < 20; ronda++) {
      const id = 3900 + ronda; const dia = 100; const grupo = huespedes.slice(ronda * 100, ronda * 100 + 100);
      const cot = await Promise.all(grupo.map(u => op('cotizar', 'POST', '/cotizaciones', { token: u.token, cuerpo: { idAlojamiento: id, fechaEntrada: fecha(dia), fechaSalida: fecha(dia + 3), huespedes: 2 } }, [201])));
      const res = await Promise.all(grupo.map((u, i) => cot[i].estado === 201 ? op('reservar_mismas_fechas', 'POST', '/reservas', { token: u.token, cabeceras: { 'Idempotency-Key': randomUUID() }, cuerpo: { idCotizacion: JSON.parse(cot[i].cuerpo).idCotizacion, metodoPago: 'pm_card_visa' } }, [201, 409]) : { estado: -1 }));
      const conteo = {}; res.forEach(r => conteo[r.estado] = (conteo[r.estado] || 0) + 1); rondas.push({ alojamiento: id, ...conteo });
      for (const [k, v] of Object.entries(conteo)) rep[k] = (rep[k] || 0) + v;
    }
    const r1 = cerrarEtapa();
    // La misma Idempotency-Key enviada 30 veces a la vez debe crear una sola reserva.
    nuevaEtapa('idempotencia', '30 x 20 rondas'); const ids = [];
    for (let ronda = 0; ronda < 20; ronda++) {
      const u = huespedes[1000 + ronda]; const dia = 200 + ronda * 5;
      const c = await pedir('POST', '/cotizaciones', { token: u.token, cuerpo: { idAlojamiento: 3800, fechaEntrada: fecha(dia), fechaSalida: fecha(dia + 3), huespedes: 2 } });
      const idc = JSON.parse(c.cuerpo).idCotizacion; const clave = randomUUID();
      const rs = await Promise.all(Array.from({ length: 30 }, () => op('reservar_misma_clave', 'POST', '/reservas', { token: u.token, cabeceras: { 'Idempotency-Key': clave }, cuerpo: { idCotizacion: idc, metodoPago: 'pm_card_visa' } }, [200, 201, 409])));
      const creadas = new Set(rs.filter(x => [200, 201].includes(x.estado)).map(x => JSON.parse(x.cuerpo).idReserva));
      ids.push({ ronda, reservasDistintas: creadas.size, estados: rs.reduce((m, x) => (m[x.estado] = (m[x.estado] || 0) + 1, m), {}) });
    }
    cerrarEtapa();
    console.log('Resultado por estado:', rep);
    return { rondas, ganadoresPorRonda: rondas.map(r => r[201] || 0), idempotencia: ids, primeraEtapa: r1?.nombre };
  },
};

if (!escenarios[escenario]) throw new Error('Escenario desconocido: ' + escenario);
console.log(`Escenario ${escenario} contra ${BASE} (${new Date().toISOString()})`);
const extra = await escenarios[escenario]();
clearInterval(telemetria);
mkdirSync(salida, { recursive: true });
writeFileSync(`${salida}/${escenario}${process.env.SUFIJO || ''}.json`, JSON.stringify({ escenario, base: BASE, fecha: new Date().toISOString(), etapas: etapas.map(e => e.resultado), telemetria: serie, extra }, null, 1));
console.log(`Resultados en ${salida}/${escenario}${process.env.SUFIJO || ''}.json`);
process.exit(0);
