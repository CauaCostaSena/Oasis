package com.ckgd.service;

import com.ckgd.dto.SuporteRequest;
import com.ckgd.entity.Empresa;
import com.ckgd.entity.SolicitacaoSuporte;
import com.ckgd.repository.SolicitacaoSuporteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuporteService {
    private final SolicitacaoSuporteRepository repository;
    private final EmpresaService empresaService;

    public SuporteService(SolicitacaoSuporteRepository repository, EmpresaService empresaService) {
        this.repository = repository;
        this.empresaService = empresaService;
    }

    @Transactional
    public SolicitacaoSuporte registrar(Authentication authentication, SuporteRequest request) {
        Empresa empresa = empresaService.buscarPorCnpj(authentication.getName());
        SolicitacaoSuporte solicitacao = new SolicitacaoSuporte();
        solicitacao.setAssunto(request.getAssunto().trim());
        solicitacao.setMensagem(request.getMensagem().trim());
        solicitacao.setTipoSolicitante("EMPRESA");
        solicitacao.setNomeSolicitante(empresa.getNomeEmpresa());
        solicitacao.setEmailSolicitante(empresa.getEmail());
        return repository.save(solicitacao);
    }
}
