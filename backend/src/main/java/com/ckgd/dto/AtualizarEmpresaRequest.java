package com.ckgd.dto;

public class AtualizarEmpresaRequest {

    @jakarta.validation.constraints.Size(min = 1, max = 120)
    @jakarta.validation.constraints.Pattern(regexp = "(?s).*\\S.*", message = "Nome não pode ficar em branco")
    private String nomeEmpresa;
    @jakarta.validation.constraints.Size(max = 20)
    private String telefone;

    public String getNomeEmpresa() { return nomeEmpresa; }
    public void setNomeEmpresa(String nomeEmpresa) { this.nomeEmpresa = nomeEmpresa; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
