# Guía de la prueba de estrés

Cómo funciona la prueba de estrés del backend, cómo correrla y cómo leer sus resultados.

## Cómo funciona

La prueba aumenta el tráfico poco a poco hasta que el sistema se rompe, y luego comprueba si se recupera.

1. **Crea un entorno aislado (`entorno.sh`):** levanta una base H2 en memoria y el backend. Stripe, el correo, Cloudinary y Nominatim quedan apagados, y no se usa la base de Neon.
2. **Carga datos de prueba (`SembrarDatos.java`):** crea 2.200 usuarios de prueba, 5.000 alojamientos y 30.000 reservas, con sus reseñas, notificaciones y auditoría. También genera tokens para los usuarios, porque el login normal de un huésped pide un código por correo.
3. **Genera el tráfico (`carga.mjs`):** mezcla búsquedas, detalles, perfil, cotizaciones y reservas, parecido a un uso real.
4. **Sube por escalones:** cada uno dura 60 s y va de 50 a 2.000 peticiones por segundo (rps).
   - La tasa no baja aunque el servidor se ponga lento, igual que pasa con usuarios reales.
   - Una petición que tarda más de 10 s cuenta como `TIMEOUT`.
   - La subida se detiene al superar el 20 % de errores, o si se descartan demasiadas peticiones.
5. **Mide la recuperación:** vuelve a 50 rps durante 2 minutos para ver si el sistema se normaliza.

Cada 5 s también guarda la CPU del backend y de H2, los hilos de Tomcat, las conexiones en cola y la memoria.

## Cómo correrla

Antes de empezar, comprueba que nada esté usando los puertos 8081 y 9092. Si otra prueba está corriendo, espera a que termine: si no, chocan y se dañan las mediciones de ambas.

Desde `marketplace-backend`:

```bash
cd docs/rendimiento
SUFIJO=_mio ./ejecutar.sh estres
```

- **Qué hace:** compila si hace falta, prepara todo, calienta la JVM 30 s, ejecuta la prueba y apaga todo al final.
- **Cuánto tarda:** unos 10 minutos.
- **Qué necesitas:** Node 22 y un JDK en `~/Prog/JDK`. Si tu JDK está en otra ruta, antepón `JAVA_HOME_PRUEBAS=/ruta/al/jdk` al comando.
- **Para qué es `SUFIJO=_mio`:** guarda el resultado aparte, en `resultados/estres_mio.json`, y así no sobrescribe `resultados/estres.json`.

Si cambias el código, borra `/tmp/marketplace-carga` antes de correrla; si no, usará la compilación anterior.

## Cómo ver los resultados

Mientras corre, la terminal imprime una línea por escalón. Por ejemplo:

```text
escalon_225  obj=225 rps  rps=227.1  p50=30.2     p95=122.3    err=0%
escalon_250  obj=250 rps  rps=215.9  p50=282.6    p95=3455.7   err=0%
escalon_275  obj=275 rps  rps=235.7  p50=10001.2  p95=10004.9  err=100%
```

| Dato | Qué significa | Señal de problema |
|---|---|---|
| `obj` y `rps` | Tráfico pedido y tráfico atendido | `rps` queda por debajo de `obj`: ya no da abasto |
| `p50` | Tiempo de una petición típica, en ms | Sube de golpe |
| `p95` | El 95 % de las peticiones tarda menos que esto | Supera 500 ms, el objetivo asumido |
| `err` | Porcentaje de peticiones fallidas | Supera el 1 % |
| `desc` | Peticiones descartadas por el generador | Mayor que 0 |

En ese ejemplo, 225 rps va bien, 250 ya se degrada y 275 colapsa. Esos números son de H2 en una sola máquina, así que no equivalen a la capacidad real en producción.

El objetivo de p95 < 500 ms y errores < 1 % es una convención: el repositorio no define objetivos numéricos de rendimiento.

Para ver el resumen después, incluida la telemetría:

```bash
python3 - <<'E'
import json
d = json.load(open('resultados/estres_mio.json'))
for e in d['etapas']:
    t = [x for x in d['telemetria'] if x.get('etapa') == e['nombre']]
    m = lambda k: max([x.get(k) or 0 for x in t] or [0])
    print(f"{e['nombre']:16} rps={e['rps']:<7} p95={e['p95']:<8} err={e['errPct']}%"
          f"  en_cola={m('hikPend')}  hilos={m('hilos')}"
          f"  cpu_backend={m('cpuBackend')}%  cpu_h2={m('cpuH2')}%")
E
```

- **`en_cola`:** peticiones esperando una conexión a la base.
- **`hilos`:** hilos ocupados de Tomcat, cuyo máximo es 200.
- **CPU:** 100 % equivale a un núcleo.

Si `en_cola` se dispara y `hilos` llega a 200, el atasco está en el acceso a la base.

## Prueba de estrés y batería completa

`./ejecutar.sh estres` corre solo la prueba de estrés, unos 10 minutos. La batería completa encadena varios escenarios, cada uno con base nueva y JVM calentada, y tarda cerca de 1 h 20 min:

| Escenario | Qué comprueba | Duración aproximada |
|---|---|---|
| `concurrencia` | Doble reserva e idempotencia | 2 min |
| `estres` | Punto de quiebre y recuperación | 10 min |
| `carga` | 50, 200 y 400 usuarios simultáneos | 13 min |
| `pico` | Subida brusca a 600 rps y recuperación | 4 min |
| `login` | Inicios de sesión por segundo | 4 min |
| `login_fallido` | Contraseñas incorrectas masivas | 2 min |
| `resistencia` | 150 rps sostenidos | 15 min |
| `estres` con 30 conexiones | Si el límite se mueve con un pool mayor | 10 min |
| `estres` sin `open-in-view` | Si el límite se mueve al liberar antes las conexiones | 10 min |
| Preparación | Arranque, datos y calentamiento de los 9 escenarios | 10 min |

## Cómo correr cada escenario por separado

Cada escenario se puede correr solo. Los comandos se lanzan desde `docs/rendimiento`, uno a la vez, y cada uno prepara su entorno y lo apaga al terminar.

```bash
# 1. Concurrencia
./ejecutar.sh concurrencia

# 2. Estrés
./ejecutar.sh estres

# 3. Carga
./ejecutar.sh carga

# 4. Pico
BASE_RPS=50 PICO_RPS=600 ./ejecutar.sh pico

# 5. Login
./ejecutar.sh login

# 6. Login fallido
./ejecutar.sh login_fallido

# 7. Resistencia en frío (150 rps de golpe, tras un arranque)
RESISTENCIA_RPS=150 RESISTENCIA_MIN=15 ./ejecutar.sh resistencia

# 7b. Resistencia con rampa (sube en 5 minutos, como un día normal)
RESISTENCIA_RPS=150 RESISTENCIA_MIN=15 RESISTENCIA_RAMPA=1 SUFIJO=_con_rampa ./ejecutar.sh resistencia

# 8. Estrés con 30 conexiones
CARGA_POOL=30 SUFIJO=_pool30 ./ejecutar.sh estres

# 9. Estrés sin open-in-view
CARGA_OSIV=false SUFIJO=_sin_osiv ./ejecutar.sh estres
```

Cada comando guarda su resultado en `resultados/<escenario>.json`, y reemplaza el anterior si ya existe. Para conservarlo, añade un sufijo al inicio del comando, por ejemplo:

```bash
SUFIJO=_mio ./ejecutar.sh carga
```

Ese comando guarda en `resultados/carga_mio.json`. Los archivos `errores_<escenario>.txt` también llevan el sufijo, pero `concurrencia_sql.txt` se sobrescribe siempre.
