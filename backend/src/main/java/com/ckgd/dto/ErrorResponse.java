package com.ckgd.dto;

import java.time.LocalDateTime;

/** Campos em portugues mantidos durante a transicao do frontend. */
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path);
    }
    public String getErro() { return error; }
    public String getMensagem() { return message; }
}
