package co.edu.unbosque.marketplace.configuration;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Documentación de Swagger. Define el esquema "bearerAuth" que usan los
 * controladores con {@code @SecurityRequirement(name = "bearerAuth")}: en
 * Swagger UI aparece el botón "Authorize" para pegar el token de /auth/login o
 * /auth/register.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Marketplace de alojamientos", version = "1.0"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
