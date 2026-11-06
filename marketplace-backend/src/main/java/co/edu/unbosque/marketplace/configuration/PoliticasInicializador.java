package co.edu.unbosque.marketplace.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.entity.PoliticaCancelacion;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;

/**
 * Crea al arrancar las políticas de cancelación base si no existen:
 * <ul>
 * <li>FLEXIBLE: 100 % con 1 o más días de anticipación.</li>
 * <li>MODERADA: 100 % con 5 o más días; 50 % con 1 a 4 días.</li>
 * <li>ESTRICTA: 50 % con 7 o más días; 0 % después.</li>
 * </ul>
 * Los anfitriones eligen una de estas políticas para cada alojamiento, y de
 * ella sale cuánto se devuelve cuando un huésped cancela.
 */
@Component
public class PoliticasInicializador implements CommandLineRunner {

	/** Porcentaje que se devuelve en el reembolso parcial. */
	private static final int PARCIAL = 50;

	/** Repositorio de las políticas. */
	private final PoliticaCancelacionRepository politicaRepo;

	/**
	 * Crea el inicializador.
	 *
	 * @param politicaRepo repositorio de políticas de cancelación
	 */
	public PoliticasInicializador(PoliticaCancelacionRepository politicaRepo) {
		this.politicaRepo = politicaRepo;
	}

	/**
	 * Crea las tres políticas base que falten.
	 *
	 * @param args argumentos de la línea de comandos (no se usan)
	 */
	@Override
	public void run(String... args) {
		crearSiFalta("FLEXIBLE", "Devolución del 100 % si cancelas con al menos 1 día de anticipación.", 1, null, null);
		crearSiFalta("MODERADA", "Devolución del 100 % con 5 o más días de anticipación y del 50 % con 1 a 4 días.", 5,
				1, PARCIAL);
		crearSiFalta("ESTRICTA", "Devolución del 50 % con 7 o más días de anticipación; después no hay devolución.",
				null, 7, PARCIAL);
	}

	/**
	 * Crea una política si todavía no existe una con ese nombre. Así se puede
	 * arrancar la aplicación muchas veces sin duplicarlas.
	 *
	 * @param nombre            nombre de la política
	 * @param condiciones       texto que se le muestra al huésped
	 * @param diasTotal         días mínimos de anticipación para devolver el
	 *                          100 %; {@code null} si no aplica
	 * @param diasParcial       días mínimos para la devolución parcial;
	 *                          {@code null} si no aplica
	 * @param porcentajeParcial porcentaje de la devolución parcial
	 */
	private void crearSiFalta(String nombre, String condiciones, Integer diasTotal, Integer diasParcial,
			Integer porcentajeParcial) {
		if (politicaRepo.findByNombre(nombre).isPresent()) {
			return;
		}
		PoliticaCancelacion politica = new PoliticaCancelacion();
		politica.setNombre(nombre);
		politica.setCondiciones(condiciones);
		politica.setDiasReembolsoTotal(diasTotal);
		politica.setDiasReembolsoParcial(diasParcial);
		politica.setPorcentajeParcial(porcentajeParcial);
		politicaRepo.save(politica);
	}
}
