package com.ckgd.dto;

import java.util.List;
import java.time.LocalDateTime;

public class CandidatoResponse {
    private Long nodeId;
    private String nomeCandidato;
    private String username;
    private String localizacao;
    private Integer numRepositorios;
    private String bio;
    private String avatarUrl;
    private String linguagemPrincipal;
    private List<RepositorioResponse> repositorios;
    private Integer totalEstrelas;
    private Boolean favorito;
    private List<String> linguagensIdentificadas;
    private Integer repositoriosAnalisados;
    private LocalDateTime dataUltimaSincronizacao;

    /** REST id; nodeId permanece apenas como alias de compatibilidade da API antiga. */
    public Long getGithubUserId() { return nodeId; }
    public List<String> getLinguagensIdentificadas() { return linguagensIdentificadas; }
    public void setLinguagensIdentificadas(List<String> value) { this.linguagensIdentificadas = value; }
    public Integer getRepositoriosAnalisados() { return repositoriosAnalisados; }
    public void setRepositoriosAnalisados(Integer value) { this.repositoriosAnalisados = value; }
    public LocalDateTime getDataUltimaSincronizacao() { return dataUltimaSincronizacao; }
    public void setDataUltimaSincronizacao(LocalDateTime value) { this.dataUltimaSincronizacao = value; }
    public boolean isMetricasParciais() { return true; }

    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }

    public String getNomeCandidato() { return nomeCandidato; }
    public void setNomeCandidato(String nomeCandidato) { this.nomeCandidato = nomeCandidato; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public Integer getNumRepositorios() { return numRepositorios; }
    public void setNumRepositorios(Integer numRepositorios) { this.numRepositorios = numRepositorios; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getLinguagemPrincipal() { return linguagemPrincipal; }
    public void setLinguagemPrincipal(String linguagemPrincipal) { this.linguagemPrincipal = linguagemPrincipal; }

    public List<RepositorioResponse> getRepositorios() { return repositorios; }
    public void setRepositorios(List<RepositorioResponse> repositorios) { this.repositorios = repositorios; }

    public Integer getTotalEstrelas() { return totalEstrelas; }
    public void setTotalEstrelas(Integer totalEstrelas) { this.totalEstrelas = totalEstrelas; }

    public Boolean getFavorito() { return favorito; }
    public void setFavorito(Boolean favorito) { this.favorito = favorito; }
}
