package com.ckgd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SuporteRequest {

    @NotBlank(message = "Assunto é obrigatório")
    @Size(max = 150)
    private String assunto;

    @NotBlank(message = "Mensagem é obrigatória")
    @Size(max = 5000)
    private String mensagem;

    public String getAssunto() { return assunto; }
    public void setAssunto(String assunto) { this.assunto = assunto; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
}
