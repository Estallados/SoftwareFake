#!/usr/bin/env bash
# Corre escenarios, cada uno con base nueva y JVM calentada: ./ejecutar.sh estres concurrencia ...
set -uo pipefail
AQUI="$(cd "$(dirname "$0")" && pwd)"; export TRABAJO="${TRABAJO:-/tmp/marketplace-carga}"
[[ -f "$TRABAJO/run.txt" ]] || "$AQUI/entorno.sh" compilar
for e in "$@"; do
  "$AQUI/entorno.sh" detener; "$AQUI/entorno.sh" iniciar | tail -1
  node "$AQUI/carga.mjs" humo http://localhost:8081 "$TRABAJO/usuarios.json" "$TRABAJO/calentamiento" > /dev/null
  node "$AQUI/carga.mjs" "$e" http://localhost:8081 "$TRABAJO/usuarios.json" "$AQUI/resultados"
  if [[ "$e" == concurrencia ]]; then
    h2=$(tr ':' '\n' < "$TRABAJO/cp.txt" | grep -m1 '/h2-2')
    "${JAVA_HOME_PRUEBAS:-$HOME/Prog/JDK}/bin/java" -cp "$h2" org.h2.tools.Shell -url "jdbc:h2:tcp://localhost:9092/mem:carga" -user sa -password "" -sql \
      "select count(*) as cruces_confirmados from reserva a join reserva b on a.id_alojamiento=b.id_alojamiento and a.id_reserva<b.id_reserva and a.fecha_entrada<b.fecha_salida and b.fecha_entrada<a.fecha_salida where a.estado='CONFIRMADA' and b.estado='CONFIRMADA'; select estado,count(*) from reserva where id_reserva>=100000 group by estado; select estado,count(*) from pago where id_pago>=100000 group by estado;" | tee "$AQUI/resultados/concurrencia_sql.txt"
  fi
  grep -c ' ERROR ' "$TRABAJO/app.log" | sed "s/^/$e: lineas ERROR en el log = /"
  grep ' ERROR ' "$TRABAJO/app.log" | sed -E 's/^.* ERROR [0-9]+ --- //' | cut -c1-170 | sort | uniq -c | sort -rn | head -6 | tee "$AQUI/resultados/errores_$e${SUFIJO:-}.txt"
done
"$AQUI/entorno.sh" detener
