package com.ckgd.service;

import com.ckgd.dto.CandidatoResponse;
import com.ckgd.dto.RepositorioResponse;
import com.ckgd.dto.github.GitHubDtos.*;
import com.ckgd.entity.Candidato;
import com.ckgd.entity.EmpresaCandidato;
import com.ckgd.entity.Repositorio;
import com.ckgd.exception.ResourceNotFoundException;
import com.ckgd.repository.CandidatoRepository;
import com.ckgd.repository.EmpresaCandidatoRepository;
import com.ckgd.repository.RepositorioRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final RepositorioRepository repositorioRepository;
    private final EmpresaCandidatoRepository empresaCandidatoRepository;
    private final GitHubService gitHubService;
    @Value("${ckgd.github.profile-cache-minutes:15}")
    private long cacheMinutes = 15;

    public CandidatoService(CandidatoRepository candidatoRepository,
                             RepositorioRepository repositorioRepository,
                             EmpresaCandidatoRepository empresaCandidatoRepository,
                             GitHubService gitHubService) {
        this.candidatoRepository = candidatoRepository;
        this.repositorioRepository = repositorioRepository;
        this.empresaCandidatoRepository = empresaCandidatoRepository;
        this.gitHubService = gitHubService;
    }

    /**
     * Busca candidatos no GitHub, persiste/atualiza os dados técnicos localmente
     * (cache) e retorna a lista já pronta para o frontend.
     */
    @Transactional
    public List<CandidatoResponse> buscarESincronizar(String termo, String linguagem, String localizacao, String cnpjEmpresa) {
        List<UserSummary> usuarios = gitHubService.buscarUsuarios(termo, linguagem, localizacao, 12);
        List<CandidatoResponse> resultado = new ArrayList<>();

        for (UserSummary usuario : usuarios) {
            Candidato candidato = candidatoRepository.findById(usuario.id).orElse(null);
            if (!cacheValido(candidato) || !usuario.login.equals(candidato.getUsername())) {
                UserDetail detalhe = gitHubService.buscarDetalhesUsuario(usuario.login);
                List<RepoSummary> repos = gitHubService.buscarRepositorios(usuario.login, 30);
                candidato = sincronizarCandidato(usuario, detalhe, repos);
            }
            // Erros externos propagam: falha não é uma lista vazia nem um resultado parcial silencioso.
            resultado.add(paraResponse(candidato, cnpjEmpresa));
        }

        return resultado;
    }

    /**
     * Busca o perfil de um usuário específico no GitHub pelo username e sincroniza
     * localmente para atualizar o cache público consultado pela empresa.
     */
    @Transactional
    public Candidato sincronizarPorUsername(String usernameGithub) {
        UserDetail detalhe = gitHubService.buscarDetalhesUsuario(usernameGithub);
        if (detalhe == null || detalhe.id == null) {
            throw new ResourceNotFoundException("Usuário do GitHub não encontrado: " + usernameGithub);
        }

        UserSummary resumo = new UserSummary();
        resumo.id = detalhe.id;
        resumo.login = detalhe.login;
        resumo.avatarUrl = detalhe.avatarUrl;

        List<RepoSummary> repos = gitHubService.buscarRepositorios(usernameGithub, 30);
        return sincronizarCandidato(resumo, detalhe, repos);
    }

    private Candidato sincronizarCandidato(UserSummary usuario, UserDetail detalhe, List<RepoSummary> repos) {
        Long nodeId = usuario.id; // REST id. A coluna física node_id é um nome legado, não o GraphQL node_id.

        Candidato candidato = candidatoRepository.findById(nodeId).orElse(new Candidato());
        candidato.setNodeId(nodeId);
        candidato.setUsername(usuario.login);
        candidato.setAvatarUrl(usuario.avatarUrl);

        if (detalhe != null) {
            candidato.setNomeCandidato(detalhe.name != null ? detalhe.name : usuario.login);
            candidato.setLocalizacao(detalhe.location);
            candidato.setBio(detalhe.bio);
            candidato.setNumRepositorios(detalhe.publicRepos != null ? detalhe.publicRepos : 0);
        }

        Map<String, Long> frequencias = repos.stream()
                .filter(r -> !Boolean.TRUE.equals(r.fork))
                .map(r -> r.language)
                .filter(l -> l != null && !l.isBlank())
                .collect(Collectors.groupingBy(l -> l, Collectors.counting()));
        String linguagemMaisUsada = frequencias.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        candidato.setLinguagemPrincipal(linguagemMaisUsada);
        candidato.setDataUltimaSincronizacao(LocalDateTime.now());

        candidato = candidatoRepository.save(candidato);

        // Substitui somente o cache da amostra pública após ambas as consultas terem sido bem-sucedidas.
        Set<String> urlsAtuais = repos.stream().filter(r -> !Boolean.TRUE.equals(r.fork))
                .map(r -> r.htmlUrl).collect(Collectors.toSet());
        List<Repositorio> obsoletos = repositorioRepository.findByCandidato_NodeId(nodeId).stream()
                .filter(r -> !urlsAtuais.contains(r.getUrlRepositorio())).toList();
        repositorioRepository.deleteAll(obsoletos);

        for (RepoSummary repo : repos) {
            if (repo.fork != null && repo.fork) continue; // ignora forks para focar em trabalho autoral

            Repositorio entidade = repositorioRepository.findById(repo.htmlUrl).orElse(new Repositorio());
            entidade.setUrlRepositorio(repo.htmlUrl);
            entidade.setNomeRepositorio(repo.name);
            entidade.setDescricao(repo.description);
            entidade.setLinguagemPrincipal(repo.language);
            entidade.setBranchPadrao(repo.defaultBranch);
            entidade.setNumeroIssue(repo.openIssuesCount != null ? repo.openIssuesCount : 0);
            entidade.setNumeroFork(repo.forksCount != null ? repo.forksCount : 0);
            entidade.setNumeroEstrela(repo.stargazersCount != null ? repo.stargazersCount : 0);
            entidade.setCandidato(candidato);
            if (repo.pushedAt != null) {
                entidade.setUltimoCommit(OffsetDateTime.parse(repo.pushedAt).withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
            } else {
                entidade.setUltimoCommit(null);
            }
            repositorioRepository.save(entidade);
        }

        return candidato;
    }

    private boolean cacheValido(Candidato candidato) {
        return candidato != null && candidato.getDataUltimaSincronizacao() != null
                && candidato.getDataUltimaSincronizacao().isAfter(LocalDateTime.now().minusMinutes(cacheMinutes));
    }

    @Transactional
    public CandidatoResponse buscarPerfilCompleto(Long nodeId, String cnpjEmpresa) {
        Candidato candidato = candidatoRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado"));
        if (!cacheValido(candidato)) candidato = sincronizarPorUsername(candidato.getUsername());
        return paraResponse(candidato, cnpjEmpresa);
    }

    private CandidatoResponse paraResponse(Candidato candidato, String cnpjEmpresa) {
        CandidatoResponse dto = new CandidatoResponse();
        dto.setNodeId(candidato.getNodeId());
        dto.setNomeCandidato(candidato.getNomeCandidato());
        dto.setUsername(candidato.getUsername());
        dto.setLocalizacao(candidato.getLocalizacao());
        dto.setNumRepositorios(candidato.getNumRepositorios());
        dto.setBio(candidato.getBio());
        dto.setAvatarUrl(candidato.getAvatarUrl());
        dto.setLinguagemPrincipal(candidato.getLinguagemPrincipal());
        dto.setDataUltimaSincronizacao(candidato.getDataUltimaSincronizacao());

        List<Repositorio> repos = repositorioRepository.findByCandidato_NodeId(candidato.getNodeId());
        repos.sort(Comparator.comparing(Repositorio::getUltimoCommit, Comparator.nullsLast(Comparator.reverseOrder())));
        dto.setRepositoriosAnalisados(repos.size());
        dto.setLinguagensIdentificadas(repos.stream().map(Repositorio::getLinguagemPrincipal)
                .filter(l -> l != null && !l.isBlank()).distinct().sorted().toList());
        List<RepositorioResponse> reposResponse = new ArrayList<>();
        int totalEstrelas = 0;
        for (Repositorio r : repos) {
            RepositorioResponse rr = new RepositorioResponse();
            rr.setUrlRepositorio(r.getUrlRepositorio());
            rr.setNomeRepositorio(r.getNomeRepositorio());
            rr.setDescricao(r.getDescricao());
            rr.setLinguagemPrincipal(r.getLinguagemPrincipal());
            rr.setBranchPadrao(r.getBranchPadrao());
            rr.setNumeroIssue(r.getNumeroIssue());
            rr.setNumeroFork(r.getNumeroFork());
            rr.setNumeroEstrela(r.getNumeroEstrela());
            rr.setUltimoCommit(r.getUltimoCommit());
            reposResponse.add(rr);
            totalEstrelas += r.getNumeroEstrela() != null ? r.getNumeroEstrela() : 0;
        }
        dto.setRepositorios(reposResponse);
        dto.setTotalEstrelas(totalEstrelas);

        if (cnpjEmpresa != null) {
            Optional<EmpresaCandidato> vinculo = empresaCandidatoRepository
                    .findByEmpresa_CnpjAndCandidato_NodeId(cnpjEmpresa, candidato.getNodeId());
            dto.setFavorito(vinculo.map(EmpresaCandidato::getFavorito).orElse(false));
        }

        return dto;
    }
}
