package com.ckgd.service;

import com.ckgd.dto.BuscaHistoricoResponse;
import com.ckgd.dto.CandidatoResponse;
import com.ckgd.entity.Busca;
import com.ckgd.entity.Empresa;
import com.ckgd.entity.EmpresaBusca;
import com.ckgd.exception.BusinessException;
import com.ckgd.repository.BuscaRepository;
import com.ckgd.repository.EmpresaBuscaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class BuscaService {

    private final BuscaRepository buscaRepository;
    private final EmpresaBuscaRepository empresaBuscaRepository;
    private final EmpresaService empresaService;
    private final CandidatoService candidatoService;

    public BuscaService(BuscaRepository buscaRepository,
                        EmpresaBuscaRepository empresaBuscaRepository,
                        EmpresaService empresaService,
                        CandidatoService candidatoService) {
        this.buscaRepository = buscaRepository;
        this.empresaBuscaRepository = empresaBuscaRepository;
        this.empresaService = empresaService;
        this.candidatoService = candidatoService;
    }

    @Transactional
    public List<CandidatoResponse> executarBusca(String cnpj, String termo, String linguagem, String localizacao) {
        validarTamanho(termo, 150, "Termo");
        validarTamanho(linguagem, 60, "Linguagem");
        validarTamanho(localizacao, 120, "Localização");
        empresaService.buscarPorCnpjComLock(cnpj);
        validarLimiteDoPlano(cnpj);
        List<CandidatoResponse> candidatos = candidatoService.buscarESincronizar(termo, linguagem, localizacao, cnpj);
        registrarBusca(cnpj, termo, linguagem, localizacao);
        return candidatos;
    }

    private void validarTamanho(String value, int limite, String campo) {
        if (value != null && value.length() > limite) throw new BusinessException(campo + " excede o tamanho permitido.");
    }

    public void validarLimiteDoPlano(String cnpj) {
        Empresa empresa = empresaService.buscarPorCnpj(cnpj);
        int limite = empresa.getPlano().getLimiteRequisicao();
        if (limite == 0) return;
        long usadas = empresaBuscaRepository.countByEmpresa_Cnpj(cnpj);
        if (usadas >= limite) throw new BusinessException("Limite de buscas do plano '" + empresa.getPlano().getNomePlano() + "' atingido");
    }

    @Transactional
    public void registrarBusca(String cnpj, String termo, String linguagem, String localizacao) {
        Empresa empresa = empresaService.buscarPorCnpj(cnpj);
        Busca busca = new Busca();
        busca.setTermoPesquisado(termo);
        busca.setFiltroLinguagem(linguagem);
        busca.setFiltroLocalizacao(localizacao);
        busca = buscaRepository.save(busca);
        empresaBuscaRepository.save(new EmpresaBusca(busca, empresa));
    }

    @Transactional(readOnly = true)
    public List<BuscaHistoricoResponse> listarHistorico(String cnpj) {
        empresaService.buscarPorCnpj(cnpj);
        return empresaBuscaRepository.findByEmpresa_Cnpj(cnpj).stream()
                .map(EmpresaBusca::getBusca)
                .sorted(Comparator
                        .comparing(Busca::getDataBusca, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Busca::getHoraBusca, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(30)
                .map(BuscaHistoricoResponse::from)
                .toList();
    }
}
