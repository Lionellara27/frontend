package com.nakel.frontend.model;

import java.util.List;

public class Venta {
    // Estos campos deben coincidir con lo que esperas enviar al JSON
    private String cliente;
    private Double total;
    private Boolean esFiscal;
    private Boolean esTicketCambio;
    private List<DetalleVenta> detalles;
    private List<Pago> pagos;

    // ESTE ES EL CONSTRUCTOR CON LOS 6 ARGUMENTOS QUE NECESITA TU CONTROLLER
    public Venta(String cliente, Double total, Boolean esFiscal, Boolean esTicketCambio,
                 List<DetalleVenta> detalles, List<Pago> pagos) {
        this.cliente = cliente;
        this.total = total;
        this.esFiscal = esFiscal;
        this.esTicketCambio = esTicketCambio;
        this.detalles = detalles;
        this.pagos = pagos;
    }

    // Getters necesarios para que GSON cree el JSON correctamente
    public String getCliente() { return cliente; }
    public Double getTotal() { return total; }
    public Boolean getEsFiscal() { return esFiscal; }
    public Boolean getEsTicketCambio() { return esTicketCambio; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public List<Pago> getPagos() { return pagos; }
}