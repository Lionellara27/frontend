package com.nakel.frontend.model;

import java.util.List;

public class Venta {

    private Long id;
    private Cliente cliente; // Ahora es un objeto
    private Double total;
    private Boolean esFiscal;
    private Boolean esTicketCambio;
    private List<DetalleVenta> detalles;
    private List<Pago> pagos;
    private String fechaHora;


    // 🔥 CONSTRUCTOR VACÍO (Obligatorio para que Gson pueda leer el Historial)
    public Venta() {
    }

    // CONSTRUCTOR PARA EL MOSTRADOR (El que venís usando)
    public Venta(Cliente cliente, Double total, Boolean esFiscal, Boolean esTicketCambio,
                 List<DetalleVenta> detalles, List<Pago> pagos) {
        this.cliente = cliente;
        this.total = total;
        this.esFiscal = esFiscal;
        this.esTicketCambio = esTicketCambio;
        this.detalles = detalles;
        this.pagos = pagos;
    }

    // 🔥 ACÁ ESTÁ LA CORRECCIÓN: Ahora devuelve Cliente, no String
    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    // El resto de los Getters
    public Double getTotal() { return total; }
    public Boolean getEsFiscal() { return esFiscal; }
    public Boolean getEsTicketCambio() { return esTicketCambio; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public List<Pago> getPagos() { return pagos; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // Agregá esto al final con los demás getters
    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    // 🔥 AGREGAR ESTO EN Venta.java para que Gson pueda setearlo desde el JSON
    public void setEsTicketCambio(Boolean esTicketCambio) {
        this.esTicketCambio = esTicketCambio;
    }
}