#!/usr/bin/env bash
# Levanta el entorno aislado de las pruebas: H2 (proceso propio) + backend con los servicios externos apagados.
#   ./entorno.sh compilar | iniciar [--sql] | detener
# Variables: TRABAJO (carpeta temporal, por defecto /tmp/marketplace-carga), JAVA_HOME_PRUEBAS (JDK 17+ con javac),
#            CARGA_XMX (heap del backend, por defecto 1g), CARGA_POOL (conexiones, por defecto 10 como Hikari),
#            CARGA_OSIV (open-in-view, por defecto true como Spring Boot).
set -euo pipefail
AQUI="$(cd "$(dirname "$0")" && pwd)"; RAIZ="$(cd "$AQUI/../.." && pwd)"
TRABAJO="${TRABAJO:-/tmp/marketplace-carga}"; mkdir -p "$TRABAJO"
JDK="${JAVA_HOME_PRUEBAS:-$HOME/Prog/JDK}"; PATH="$JDK/bin:$PATH"
CLAVE_PRUEBA="bWFya2V0cGxhY2Utc2VtaWxsYS1kZW1vLTIwMjYhISE="   # solo desarrollo: la misma de 05_seed_data.sql
URL="jdbc:h2:tcp://localhost:9092/mem:carga;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"

compilar() {
  # El classpath sale de .factorypath (dependencias ya descargadas). Se compila fuera de target/.
  python3 -c "
import re,os
t=open('$RAIZ/.factorypath',encoding='utf8').read()
print(':'.join(i.replace('M2_REPO',os.path.expanduser('~/.m2/repository')) for i in re.findall(r'id=\"([^\"]+\.jar)\"',t)))" > "$TRABAJO/cp.txt"
  rm -rf "$TRABAJO/out"; mkdir -p "$TRABAJO/out"
  javac -nowarn -proc:full -parameters -encoding UTF-8 --release 17 -cp "$(cat "$TRABAJO/cp.txt")" \
    -processorpath "$(tr ':' '\n' < "$TRABAJO/cp.txt" | grep -m1 lombok)" -d "$TRABAJO/out" $(find "$RAIZ/src/main/java" -name '*.java')
  cp "$RAIZ/src/main/resources/application.properties" "$TRABAJO/out/"
  echo "out:$(tr ':' '\n' < "$TRABAJO/cp.txt" | grep -v -E 'devtools|lombok' | paste -sd:)" > "$TRABAJO/run.txt"
}

iniciar() {
  cd "$TRABAJO"
  local h2; h2=$(tr ':' '\n' < cp.txt | grep -m1 '/h2-2')
  nohup java -Xmx4g -cp "$h2" org.h2.tools.Server -tcp -tcpPort 9092 -ifNotExists > h2.log 2>&1 &
  echo $! > h2.pid; sleep 2
  local log=app.log; local extra=()
  if [[ "${1:-}" == "--sql" ]]; then log=app-sql.log; extra=(--logging.level.org.hibernate.SQL=DEBUG); fi
  env -u DB_URL -u DB_USERNAME -u DB_PASSWORD -u STRIPE_SECRET_KEY -u CLOUDINARY_URL \
    AES_SECRET_KEY="$CLAVE_PRUEBA" MAIL_PASSWORD= ADMIN_EMAIL=admin@carga.local ADMIN_PASSWORD='Carga#2026' \
    nohup java "-Xms${CARGA_XMX:-1g}" "-Xmx${CARGA_XMX:-1g}" -cp "$(cat run.txt)" co.edu.unbosque.marketplace.MarketplaceApplication \
    --spring.datasource.url="$URL" --spring.datasource.driver-class-name=org.h2.Driver \
    --spring.datasource.username=sa --spring.datasource.password= \
    --spring.datasource.hikari.maximum-pool-size="${CARGA_POOL:-10}" \
    --spring.jpa.hibernate.ddl-auto=create --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
    --spring.jpa.open-in-view="${CARGA_OSIV:-true}" --spring.mail.host=127.0.0.1 --spring.mail.port=1 --management.health.mail.enabled=false \
    --app.geocoding.habilitado=false --app.stripe.secret-key= --app.demo.poblar=false \
    --management.endpoints.web.exposure.include=health,metrics --server.tomcat.mbeanregistry.enabled=true \
    --logging.level.root=WARN --logging.level.AUDITORIA=OFF --logging.file.name= \
    "${extra[@]}" > "$log" 2>&1 &
  echo $! > app.pid
  for _ in $(seq 1 60); do
    [[ "$(curl -s -m 2 localhost:8081/actuator/health || true)" == *UP* ]] && break; sleep 1
  done
  curl -s -m 2 localhost:8081/actuator/health; echo
  AES_SECRET_KEY="$CLAVE_PRUEBA" java -cp "$(cat run.txt)" "$AQUI/SembrarDatos.java" "$URL" "$RAIZ/database/02_indexes.sql" usuarios.json
}

detener() {
  cd "$TRABAJO"
  for f in app.pid h2.pid; do [[ -f $f ]] && kill "$(cat $f)" 2>/dev/null || true; rm -f "$f"; done
  sleep 2
}
"${1:?compilar|iniciar|detener}" "${@:2}"
