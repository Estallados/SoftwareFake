#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Batería de pruebas de inyección SQL contra el backend marketplace (Spring Boot).
Ataca el backend REAL corriendo en http://localhost:8081 (conectado a PostgreSQL).
No usa dependencias externas: solo la librería estándar de Python.

Objetivo: verificar si los endpoints públicos son vulnerables a SQLi
(bypass de autenticación, boolean-based, error-based, UNION-based y time-based ciego).

Uso:
    python sqli_test.py [base_url]
    (por defecto base_url = http://localhost:8081)

Genera resultados en:  resultados_sqli.json
"""
import json
import sys
import time
import urllib.request
import urllib.error
import urllib.parse
from datetime import datetime, timezone

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081"
TIMEOUT = 30  # segundos; alto para detectar time-based (pg_sleep)

resultados = []


def peticion(metodo, ruta, *, params=None, cuerpo=None, headers=None):
    """Hace una petición HTTP y devuelve (status, tiempo_seg, cuerpo_texto, error)."""
    url = BASE + ruta
    if params:
        url += "?" + urllib.parse.urlencode(params, doseq=True)
    datos = None
    hdrs = {"Accept": "application/json"}
    if cuerpo is not None:
        datos = json.dumps(cuerpo).encode("utf-8")
        hdrs["Content-Type"] = "application/json"
    if headers:
        hdrs.update(headers)
    req = urllib.request.Request(url, data=datos, headers=hdrs, method=metodo)
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
            texto = r.read().decode("utf-8", "replace")
            return r.status, time.perf_counter() - t0, texto, None
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8", "replace")
        return e.code, time.perf_counter() - t0, texto, None
    except Exception as e:  # noqa: BLE001
        return None, time.perf_counter() - t0, "", repr(e)


def registrar(caso, endpoint, vector, payload, status, t, cuerpo, error, veredicto, nota):
    recorte = (cuerpo or "")[:400]
    resultados.append({
        "caso": caso, "endpoint": endpoint, "vector": vector, "payload": payload,
        "status": status, "tiempo_seg": round(t, 3),
        "cuerpo_recortado": recorte, "error_red": error,
        "veredicto": veredicto, "nota": nota,
    })
    marca = {"SEGURO": "[OK]", "VULNERABLE": "[!!]", "INFO": "[..]", "ERROR_RED": "[xx]"}.get(veredicto, "[??]")
    print(f"{marca} {caso:<42} status={status} t={t:5.2f}s {nota}")


# ---------------------------------------------------------------------------
# 0. Baseline de conectividad y de tiempo
# ---------------------------------------------------------------------------
print("=" * 78)
print(f"Objetivo: {BASE}   ({datetime.now(timezone.utc).isoformat()})")
print("=" * 78)

s, t, b, e = peticion("GET", "/alojamientos", params={"pagina": 0, "tamanio": 1})
if e:
    print(f"\n*** No se pudo conectar al backend en {BASE}: {e}")
    print("*** Asegúrate de que el backend esté corriendo (mvn spring-boot:run) y conectado a Postgres.\n")
    sys.exit(2)
BASELINE_T = t
registrar("baseline-conectividad", "GET /alojamientos", "-", "-", s, t, b, e, "INFO",
          f"baseline de tiempo normal = {t:.2f}s")

# Login baseline con credenciales inexistentes (control negativo)
s, t, b, e = peticion("POST", "/auth/login",
                      cuerpo={"correo": "noexiste@test.local", "contrasenia": "x"})
registrar("baseline-login-fallido", "POST /auth/login", "-", "(credenciales falsas)", s, t, b, e, "INFO",
          "control: login con credenciales que no existen")

# ---------------------------------------------------------------------------
# 1. Bypass de autenticación (POST /auth/login) - el ataque SQLi más clásico
# ---------------------------------------------------------------------------
payloads_login = [
    "' OR '1'='1",
    "' OR '1'='1'-- ",
    "' OR 1=1-- ",
    "admin'-- ",
    "' OR ''='",
    "') OR ('1'='1",
    "' UNION SELECT NULL-- ",
    "'; DROP TABLE usuario-- ",
    "' OR 1=1#",
    "\" OR \"1\"=\"1",
]
for p in payloads_login:
    # Inyección en el campo correo
    s, t, b, e = peticion("POST", "/auth/login", cuerpo={"correo": p, "contrasenia": "x"})
    if e:
        v, nota = "ERROR_RED", e
    elif s == 200 and ("token" in (b or "")):
        v, nota = "VULNERABLE", "¡Bypass! Se obtuvo token sin credenciales válidas"
    else:
        v, nota = "SEGURO", "rechazado (payload tratado como texto literal)"
    registrar(f"login/correo: {p[:28]}", "POST /auth/login", "correo", p, s, t, b, e, v, nota)

    # Inyección en el campo contrasenia
    s, t, b, e = peticion("POST", "/auth/login", cuerpo={"correo": "admin@marketplace.local", "contrasenia": p})
    if e:
        v, nota = "ERROR_RED", e
    elif s == 200 and ("token" in (b or "")):
        v, nota = "VULNERABLE", "¡Bypass! Token obtenido manipulando la contraseña"
    else:
        v, nota = "SEGURO", "rechazado (payload tratado como texto literal)"
    registrar(f"login/contrasenia: {p[:23]}", "POST /auth/login", "contrasenia", p, s, t, b, e, v, nota)

# ---------------------------------------------------------------------------
# 2. Búsqueda pública (GET /alojamientos) - filtros string: ciudad, tipo, orden, amenidades
# ---------------------------------------------------------------------------
payloads_busqueda = [
    "' OR '1'='1",
    "' OR 1=1-- ",
    "x' UNION SELECT NULL,NULL,NULL-- ",
    "'; DROP TABLE alojamiento-- ",
    "Bogota' OR '1'='1",
    "') OR ('1'='1",
]
for campo in ["ciudad", "tipo", "orden"]:
    for p in payloads_busqueda:
        s, t, b, e = peticion("GET", "/alojamientos", params={campo: p, "pagina": 0, "tamanio": 5})
        if e:
            v, nota = "ERROR_RED", e
        elif s == 500:
            v, nota = "INFO", "error 500: revisar si filtra SQL en el mensaje (posible error-based)"
        else:
            v, nota = "SEGURO", f"status {s}: filtro aplicado como valor, sin inyección"
        registrar(f"buscar/{campo}: {p[:26]}", "GET /alojamientos", campo, p, s, t, b, e, v, nota)

# amenidades es List<String>
for p in payloads_busqueda[:3]:
    s, t, b, e = peticion("GET", "/alojamientos", params={"amenidades": p, "pagina": 0, "tamanio": 5})
    v = "ERROR_RED" if e else ("INFO" if s == 500 else "SEGURO")
    nota = e if e else f"status {s}"
    registrar(f"buscar/amenidades: {p[:20]}", "GET /alojamientos", "amenidades", p, s, t, b, e, v, nota)

# ---------------------------------------------------------------------------
# 3. Parámetro de ruta tipado (GET /alojamientos/{id}) - debe ser Long
# ---------------------------------------------------------------------------
for p in ["1 OR 1=1", "1;DROP TABLE usuario", "1' OR '1'='1", "0 UNION SELECT NULL"]:
    s, t, b, e = peticion("GET", "/alojamientos/" + urllib.parse.quote(p))
    if e:
        v, nota = "ERROR_RED", e
    elif s == 400:
        v, nota = "SEGURO", "400: Spring rechaza el valor no numérico antes de tocar la BD"
    elif s in (200, 404):
        v, nota = "SEGURO", f"status {s}: tratado como id, sin inyección"
    else:
        v, nota = "INFO", f"status {s}"
    registrar(f"detalle/id: {p[:30]}", "GET /alojamientos/{id}", "id (path)", p, s, t, b, e, v, nota)

# ---------------------------------------------------------------------------
# 4. Blind basada en tiempo (time-based) - pg_sleep de PostgreSQL
# ---------------------------------------------------------------------------
# Si el backend fuera vulnerable, pg_sleep(5) retrasaría la respuesta ~5s.
payloads_tiempo = [
    ("login/correo", "POST", "/auth/login", None,
     {"correo": "x'; SELECT pg_sleep(5)-- ", "contrasenia": "x"}),
    ("login/correo-or", "POST", "/auth/login", None,
     {"correo": "x' OR (SELECT 1 FROM pg_sleep(5))='1", "contrasenia": "x"}),
    ("buscar/ciudad", "GET", "/alojamientos",
     {"ciudad": "x'; SELECT pg_sleep(5)-- ", "pagina": 0, "tamanio": 1}, None),
    ("buscar/tipo", "GET", "/alojamientos",
     {"tipo": "x' AND pg_sleep(5)-- ", "pagina": 0, "tamanio": 1}, None),
]
for nombre, metodo, ruta, params, cuerpo in payloads_tiempo:
    s, t, b, e = peticion(metodo, ruta, params=params, cuerpo=cuerpo)
    payload_txt = json.dumps(cuerpo) if cuerpo else json.dumps(params)
    if e:
        v, nota = "ERROR_RED", e
    elif t >= 4.5:
        v, nota = "VULNERABLE", f"respuesta tardó {t:.2f}s (~5s): pg_sleep se ejecutó -> inyección time-based"
    else:
        v, nota = "SEGURO", f"respuesta en {t:.2f}s (sin retraso): pg_sleep NO se ejecutó"
    registrar(f"time-based {nombre}", f"{metodo} {ruta}", "varios", payload_txt, s, t, b, e, v, nota)

# ---------------------------------------------------------------------------
# Resumen
# ---------------------------------------------------------------------------
vuln = [r for r in resultados if r["veredicto"] == "VULNERABLE"]
seguros = [r for r in resultados if r["veredicto"] == "SEGURO"]
info = [r for r in resultados if r["veredicto"] in ("INFO", "ERROR_RED")]

resumen = {
    "objetivo": BASE,
    "fecha_utc": datetime.now(timezone.utc).isoformat(),
    "baseline_tiempo_seg": round(BASELINE_T, 3),
    "total_pruebas": len(resultados),
    "vulnerables": len(vuln),
    "seguros": len(seguros),
    "informativos": len(info),
    "veredicto_global": "VULNERABLE" if vuln else "NO VULNERABLE A SQLi",
    "resultados": resultados,
}

with open("resultados_sqli.json", "w", encoding="utf-8") as f:
    json.dump(resumen, f, ensure_ascii=False, indent=2)

print("=" * 78)
print(f"TOTAL: {len(resultados)} pruebas | VULNERABLES: {len(vuln)} | "
      f"SEGUROS: {len(seguros)} | INFO: {len(info)}")
print(f"VEREDICTO GLOBAL: {resumen['veredicto_global']}")
print("Resultados guardados en resultados_sqli.json")
print("=" * 78)
