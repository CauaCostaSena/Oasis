package com.ckgd.service;

import com.ckgd.dto.AvaliacaoRequest;
import com.ckgd.entity.Candidato;
import com.ckgd.entity.Empresa;
import com.ckgd.entity.EmpresaCandidato;
import com.ckgd.exception.ResourceNotFoundException;
import com.ckgd.exception.BusinessException;
import java.math.BigDecimal;
import com.ckgd.repository.CandidatoRepository;
import com.ckgd.repository.EmpresaCandidatoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoritoAvaliacaoService {

    private final EmpresaCandidatoRepository empresaCandidatoRepository;
    private final CandidatoRepository candidatoRepository;
    private final EmpresaService empresaService;

    public FavoritoAvaliacaoService(EmpresaCandidatoRepository empresaCandidatoRepository,
                                     CandidatoRepository candidatoRepository,
                                     EmpresaService empresaService) {
        this.empresaCandidatoRepository = empresaCandidatoRepository;
        this.candidatoRepository = candidatoRepository;
        this.empresaService = empresaService;
    }

    @Transactional
    public EmpresaCandidato salvarAvaliacao(String cnpj, Long nodeIdCandidato, AvaliacaoRequest req) {
        Empresa empresa = empresaService.buscarPorCnpjComLock(cnpj);
        Candidato candidato = candidatoRepository.findById(nodeIdCandidato)
                .orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado"));

        EmpresaCandidato vinculo = empresaCandidatoRepository
                .findByEmpresa_CnpjAndCandidato_NodeId(cnpj, nodeIdCandidato)
                .orElseGet(() -> new EmpresaCandidato(empresa, candidato));

        boolean tinhaAvaliacao = vinculo.getNota() != null ||
                (vinculo.getComentario() != null && !vinculo.getComentario().isBlank());
        boolean avaliando = req.isNotaInformada() || req.getComentario() != null;
        if (req.getNota() != null && (req.getNota() < 1 || req.getNota() > 5)) {
            throw new BusinessException("A nota deve estar entre 1 e 5.");
        }
        boolean teraAvaliacao = (req.isNotaInformada() ? req.getNota() != null : vinculo.getNota() != null) ||
                (req.getComentario() != null ? !req.getComentario().isBlank() :
                        vinculo.getComentario() != null && !vinculo.getComentario().isBlank());
        int limite = empresa.getPlano().getLimiteAvaliacao();
        if (avaliando && !tinhaAvaliacao && teraAvaliacao && limite > 0 &&
                empresaCandidatoRepository.contarAvaliacoes(cnpj) >= limite) {
            throw new BusinessException("Limite de avaliações do plano atingido.");
        }
        if (req.getFavorito() != null) {
            vinculo.setFavorito(req.getFavorito());
        }
        if (req.getComentario() != null) {
            vinculo.setComentario(req.getComentario().trim());
        }
        if (req.isNotaInformada()) vinculo.setNota(req.getNota() == null ? null : BigDecimal.valueOf(req.getNota()));
        if (avaliando) vinculo.setDataAvaliacao(teraAvaliacao ? LocalDateTime.now() : null);
        vinculo.setPrivada(true);

        return empresaCandidatoRepository.save(vinculo);
    }

    @Transactional
    public void removerFavorito(String cnpj, Long nodeIdCandidato) {
        empresaService.buscarPorCnpjComLock(cnpj);
        empresaCandidatoRepository.findByEmpresa_CnpjAndCandidato_NodeId(cnpj, nodeIdCandidato)
                .ifPresent(v -> {
                    v.setFavorito(false);
                    empresaCandidatoRepository.save(v);
                });
    }

    public List<EmpresaCandidato> listarFavoritos(String cnpj) {
        return empresaCandidatoRepository.findByEmpresa_CnpjAndFavoritoTrue(cnpj);
    }

    public List<EmpresaCandidato> listarAvaliacoes(String cnpj) {
        return empresaCandidatoRepository.listarAvaliacoes(cnpj);
    }

    public EmpresaCandidato buscarAvaliacao(String cnpj, Long id) {
        return empresaCandidatoRepository.findByEmpresa_CnpjAndCandidato_NodeId(cnpj, id)
                .orElseThrow(() -> new ResourceNotFoundException("Ainda não há avaliação deste candidato na sua empresa."));
    }
}
