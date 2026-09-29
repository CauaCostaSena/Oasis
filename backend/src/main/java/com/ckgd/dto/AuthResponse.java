package com.ckgd.dto;

/** Somente empresas autenticam no Oásis. */
public record AuthResponse(String token, String tipo, String cnpj, String nome, String email) {
    public AuthResponse(String token, String cnpj, String nome, String email) {
        this(token, "EMPRESA", cnpj, nome, email);
    }
}
