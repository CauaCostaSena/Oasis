package com.ckgd.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CadastroEmpresaRequest {

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter exatamente 14 dígitos numéricos")
    private String cnpj;

    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(min = 2, max = 120, message = "Nome da empresa deve ter entre 2 e 120 caracteres")
    private String nomeEmpresa;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String senha;

    @NotBlank(message = "País é obrigatório")
    @Size(max = 60)
    private String pais;

    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 60)
    private String estado;

    @NotBlank(message = "Cidade é obrigatória")
    @Size(min = 2, max = 60)
    private String cidade;

    @NotBlank(message = "Bairro é obrigatório")
    @Size(min = 2, max = 60)
    private String bairro;

    @NotBlank(message = "Endereço é obrigatório")
    @Size(min = 5, max = 150)
    private String endereco;

    private Integer idPlano;

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getNomeEmpresa() { return nomeEmpresa; }
    public void setNomeEmpresa(String nomeEmpresa) { this.nomeEmpresa = nomeEmpresa; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public Integer getIdPlano() { return idPlano; }
    public void setIdPlano(Integer idPlano) { this.idPlano = idPlano; }
}
