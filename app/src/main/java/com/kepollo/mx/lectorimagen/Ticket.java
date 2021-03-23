package com.kepollo.mx.lectorimagen;

public class Ticket {
    private String folio;
    private String provedor;
    private String rfc;
    private String fecha;
    private String hora;
    private String litros;
    private String punit;
    private String importe;
    private String iva;
    private String combustible;
    private int typeCombustible;
    private String noTransaccion;


    public Ticket() {
        this.provedor = "0";
        this.rfc = "0";
        this.fecha = "0";
        this.hora = "0";
        this.litros = "0";
        this.punit = "0";
        this.importe = "0";
        this.iva = "0";
        this.combustible = "0";
        this.typeCombustible = 0;
        this.noTransaccion = "0";
    }

    public String getProvedor() {
        return provedor;
    }

    public void setProvedor(String provedor) {
        this.provedor = provedor;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getLitros() {
        return litros;
    }

    public void setLitros(String litros) {
        this.litros = litros;
    }

    public String getPunit() {
        return punit;
    }

    public void setPunit(String punit) {
        this.punit = punit;
    }

    public String getImporte() {
        return importe;
    }

    public void setImporte(String importe) {
        this.importe = importe;
    }

    public String getIva() {
        return iva;
    }

    public void setIva(String iva) {
        this.iva = iva;
    }

    public String getCombustible() {
        return combustible;
    }

    public void setCombustible(String combustible) {
        this.combustible = combustible;
    }

    public int getTypeCombustible() {
        return typeCombustible;
    }

    public void setTypeCombustible(int typeCombustible) {
        this.typeCombustible = typeCombustible;
    }

    public String getNoTransaccion() {
        return noTransaccion;
    }

    public void setNoTransaccion(String noTransaccion) {
        this.noTransaccion = noTransaccion;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "provedor='" + provedor + '\'' +
                ", rfc='" + rfc + '\'' +
                ", fecha='" + fecha + '\'' +
                ", hora='" + hora + '\'' +
                ", litros=" + litros +
                ", punit=" + punit +
                ", importe=" + importe +
                ", iva=" + iva +
                ", combustible='" + combustible + '\'' +
                ", typeCombustible=" + typeCombustible +
                ", noTransaccion='" + noTransaccion + '\'' +
                '}';
    }
}
