package com.example.gastroreservabackend1.model;

public enum EstadoPedido {
    ABIERTO, EN_PREPARACION, SERVIDO, CERRADO, CANCELADO;

    public boolean esFinal() {
        return this == CERRADO || this == CANCELADO;
    }

    public boolean permite(EstadoPedido siguiente) {
        return switch (this) {
            case ABIERTO -> siguiente == EN_PREPARACION || siguiente == CANCELADO;
            case EN_PREPARACION -> siguiente == SERVIDO || siguiente == CANCELADO;
            case SERVIDO -> siguiente == CERRADO;
            case CERRADO, CANCELADO -> false;
        };
    }
}
