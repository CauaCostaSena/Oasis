package com.ckgd.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

public class AvaliacaoRequest {
    private Boolean favorito;
    @Size(max = 5000)
    private String comentario;
    @Min(1)
    @Max(5)
    private Integer nota;
    private boolean notaInformada;

    public Boolean getFavorito() { return favorito; }
    public void setFavorito(Boolean favorito) { this.favorito = favorito; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; this.notaInformada = true; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isNotaInformada() { return notaInformada; }
}
