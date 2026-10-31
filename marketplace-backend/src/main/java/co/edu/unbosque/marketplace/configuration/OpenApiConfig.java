package co.edu.unbosque.marketplace.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Documentación de Swagger.
 * <ul>
 * <li>Define el esquema "bearerAuth" que usan los controladores con
 * {@code @SecurityRequirement(name = "bearerAuth")}: en Swagger UI aparece el
 * botón "Authorize" para pegar el token. El candado de cada endpoint se ve
 * abierto mientras no se ha pegado un token y cerrado cuando ya se pegó; los
 * endpoints públicos no muestran candado ({@code @SecurityRequirements}).</li>
 * <li>Completa las respuestas de cada endpoint con las que comparten todos: 401
 * si exige token, 403 si exige un rol, 400 si recibe un JSON y 500. Cada error
 * lleva el formato real {@code {"message": "..."}} con sus mensajes de ejemplo;
 * en {@code @ApiResponse} varios mensajes posibles se separan con
 * {@value #SEPARADOR}.</li>
 * </ul>
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Marketplace de alojamientos", version = "1.0", description = "API del Marketplace de alojamientos de Inversiones LR. Todos los errores responden "
		+ "{\"message\": \"...\"}. Códigos que puede dar cualquier ruta: 404 ruta inexistente, 405 método no "
		+ "permitido, 406 formato pedido distinto de JSON, 409 otra operación cambió los mismos datos al mismo "
		+ "tiempo, 415 tipo de contenido no soportado, 503 base de datos no disponible."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

	/** Separa los mensajes posibles dentro de la descripción de una respuesta. */
	public static final String SEPARADOR = " | ";

	private static final String ESQUEMA_ERROR = "MensajeError";
	private static final String JSON = "application/json";
	private static final String NO_AUTENTICADO = "No autenticado: token ausente, inválido o expirado";
	private static final String SIN_PERMISO = "No tienes permisos para esta operación";
	private static final String JSON_INVALIDO = "El cuerpo de la petición no es un JSON válido.";
	private static final String ERROR_INTERNO = "Ocurrió un error inesperado. Intenta de nuevo.";

	/** Esquema de todos los errores: {"message": "..."}. */
	@Bean
	public OpenApiCustomizer esquemaDeError() {
		return openApi -> {
			if (openApi.getComponents() == null) {
				openApi.setComponents(new Components());
			}
			Schema<?> esquema = new ObjectSchema().description("Respuesta de error de la API").addProperty("message",
					new StringSchema().description("Causa del error, lista para mostrar"));
			openApi.getComponents().addSchemas(ESQUEMA_ERROR, esquema);
		};
	}

	/** Respuestas comunes y ejemplos de error en cada operación. */
	@Bean
	public OperationCustomizer respuestasComunes() {
		return (operacion, metodo) -> {
			ApiResponses respuestas = operacion.getResponses() == null ? new ApiResponses() : operacion.getResponses();
			quitarExitoGenerico(respuestas, metodo);
			// @SecurityRequirements vacío en el método: es público aunque la clase pida
			// token (sin candado)
			SecurityRequirements publico = AnnotatedElementUtils.findMergedAnnotation(metodo.getMethod(),
					SecurityRequirements.class);
			if (publico != null && publico.value().length == 0) {
				operacion.setSecurity(new ArrayList<>());
			}

			boolean conToken = operacion.getSecurity() != null && !operacion.getSecurity().isEmpty();
			if (conToken) {
				agregar(respuestas, "401", NO_AUTENTICADO);
				if (exigeRol(metodo)) {
					agregar(respuestas, "403", SIN_PERMISO);
				}
			}
			boolean recibeJson = operacion.getRequestBody() != null && operacion.getRequestBody().getContent() != null
					&& operacion.getRequestBody().getContent().containsKey(JSON);
			if (recibeJson) {
				agregar(respuestas, "400", JSON_INVALIDO);
			}
			// Un id, número, fecha o estado mal escrito en la URL responde 400 (tipoInvalido)
			if (operacion.getParameters() != null) {
				operacion.getParameters().stream().filter(OpenApiConfig::puedeSerInvalido)
						.forEach(p -> agregar(respuestas, "400", "El valor de " + p.getName() + " no es válido."));
			}
			agregar(respuestas, "500", ERROR_INTERNO);

			// Ordenadas por código y con el cuerpo de error real
			Map<String, ApiResponse> ordenadas = new TreeMap<>(respuestas);
			ApiResponses resultado = new ApiResponses();
			ordenadas.forEach(
					(codigo, respuesta) -> resultado.addApiResponse(codigo, conCuerpoDeError(codigo, respuesta)));
			operacion.setResponses(resultado);
			return operacion;
		};
	}

	/**
	 * springdoc agrega un "200" genérico; si el endpoint documenta otro éxito (por
	 * ejemplo 201) y no un 200, ese genérico sobra.
	 */
	private static void quitarExitoGenerico(ApiResponses respuestas, HandlerMethod metodo) {
		boolean declara200 = Arrays
				.stream(metodo.getMethod()
						.getAnnotationsByType(io.swagger.v3.oas.annotations.responses.ApiResponse.class))
				.anyMatch(a -> "200".equals(a.responseCode()));
		boolean otroExito = respuestas.keySet().stream().anyMatch(c -> c.startsWith("2") && !"200".equals(c));
		if (otroExito && !declara200) {
			respuestas.remove("200");
		}
	}

	/** Parámetros que Spring convierte (números, fechas, enums) y pueden fallar. */
	private static boolean puedeSerInvalido(Parameter parametro) {
		Schema<?> esquema = parametro.getSchema();
		if (esquema == null || "array".equals(esquema.getType())) {
			return false;
		}
		return !"string".equals(esquema.getType()) || esquema.getFormat() != null || esquema.getEnum() != null;
	}

	private static boolean exigeRol(HandlerMethod metodo) {
		return AnnotatedElementUtils.hasAnnotation(metodo.getMethod(), PreAuthorize.class)
				|| AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), PreAuthorize.class);
	}

	/**
	 * Agrega el mensaje a la respuesta del código, o crea la respuesta si no estaba
	 * documentada.
	 */
	private static void agregar(ApiResponses respuestas, String codigo, String mensaje) {
		ApiResponse existente = respuestas.get(codigo);
		if (existente == null) {
			respuestas.addApiResponse(codigo, new ApiResponse().description(mensaje));
		} else if (existente.getDescription() == null || !existente.getDescription().contains(mensaje)) {
			existente.setDescription(
					existente.getDescription() == null ? mensaje : existente.getDescription() + SEPARADOR + mensaje);
		}
	}

	/**
	 * Errores (4xx y 5xx): esquema {"message"} y un ejemplo por cada mensaje
	 * posible.
	 */
	private static ApiResponse conCuerpoDeError(String codigo, ApiResponse respuesta) {
		if (codigo.isEmpty() || codigo.charAt(0) < '4' || respuesta.getDescription() == null) {
			return respuesta;
		}
		String[] mensajes = respuesta.getDescription().split(java.util.regex.Pattern.quote(SEPARADOR));
		MediaType tipo = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_ERROR));
		for (String mensaje : mensajes) {
			String nombre = mensaje.length() > 60 ? mensaje.substring(0, 57) + "..." : mensaje;
			tipo.addExamples(nombre, new Example().value(Map.of("message", mensaje)));
		}
		respuesta.setContent(new Content().addMediaType(JSON, tipo));
		respuesta.setDescription(mensajes.length == 1 ? mensajes[0] : "- " + String.join("\n- ", mensajes));
		return respuesta;
	}
}
