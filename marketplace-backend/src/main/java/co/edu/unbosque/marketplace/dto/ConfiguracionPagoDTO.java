package co.edu.unbosque.marketplace.dto;

import java.util.List;

/**
 * Lo que el front necesita para cobrar: si los pagos van a Stripe (modo
 * prueba) o al simulador, la clave publicable para Stripe Elements, la moneda
 * y las tarjetas de prueba con su resultado.
 *
 * @see "RF-028"
 */
public class ConfiguracionPagoDTO {
    /** STRIPE_TEST o SIMULADO. */
    private String modo;
    /** Clave publicable de Stripe (pk_test_...); null en modo SIMULADO. */
    private String clavePublica;
    private String moneda;
    private List<MetodoPagoPruebaDTO> metodosPrueba;

    public ConfiguracionPagoDTO() {
    }

    public String getModo() {
        return modo;
    }
    public void setModo(String modo) {
        this.modo = modo;
    }

    public String getClavePublica() {
        return clavePublica;
    }
    public void setClavePublica(String clavePublica) {
        this.clavePublica = clavePublica;
    }

    public String getMoneda() {
        return moneda;
    }
    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public List<MetodoPagoPruebaDTO> getMetodosPrueba() {
        return metodosPrueba;
    }
    public void setMetodosPrueba(List<MetodoPagoPruebaDTO> metodosPrueba) {
        this.metodosPrueba = metodosPrueba;
    }
}
