package com.ckgd.dto;

import com.ckgd.entity.Busca;
import java.time.LocalDate;
import java.time.LocalTime;

public class BuscaHistoricoResponse {
    private Long idBusca;
    private String termo;
    private String linguagem;
    private String localizacao;
    private LocalDate data;
    private LocalTime hora;

    public static BuscaHistoricoResponse from(Busca busca) {
        BuscaHistoricoResponse dto = new BuscaHistoricoResponse();
        dto.idBusca = busca.getIdBusca();
        dto.termo = busca.getTermoPesquisado();
        dto.linguagem = busca.getFiltroLinguagem();
        dto.localizacao = busca.getFiltroLocalizacao();
        dto.data = busca.getDataBusca();
        dto.hora = busca.getHoraBusca();
        return dto;
    }

    public Long getIdBusca() { return idBusca; }
    public String getTermo() { return termo; }
    public String getLinguagem() { return linguagem; }
    public String getLocalizacao() { return localizacao; }
    public LocalDate getData() { return data; }
    public LocalTime getHora() { return hora; }
}
