package com.ckgd.service;

import com.ckgd.dto.github.GitHubDtos.*;
import com.ckgd.exception.BusinessException;
import com.ckgd.exception.GitHubApiException;
import com.ckgd.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class GitHubService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final RestClient githubRestClient;
    private final Map<String, SearchCache> searches = new ConcurrentHashMap<>();
    @Value("${ckgd.github.search-cache-seconds:60}")
    private long searchCacheSeconds = 60;

    private record SearchCache(Instant expiresAt, List<UserSummary> users) {}

    public GitHubService(RestClient githubRestClient) { this.githubRestClient = githubRestClient; }

    private <T> T chamar(Supplier<T> chamada) {
        try {
            T result = chamada.get();
            if (result == null) throw new GitHubApiException(502, "O GitHub retornou uma resposta incompleta. Tente novamente.");
            return result;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("Consulta GitHub falhou: HTTP {}", status);
            boolean rateLimited = status == 429 || (status == 403 &&
                    ((e.getResponseHeaders() != null && ("0".equals(e.getResponseHeaders().getFirst("X-RateLimit-Remaining"))
                            || e.getResponseHeaders().containsKey("Retry-After")))
                    || e.getResponseBodyAsString().toLowerCase(java.util.Locale.ROOT).contains("rate limit")));
            if (rateLimited) throw new GitHubApiException(429, "O limite temporário de consultas ao GitHub foi atingido. Aguarde antes de tentar novamente.");
            if (status == 404) throw new ResourceNotFoundException("Perfil ou repositório não encontrado no GitHub.");
            if (status == 401 || status == 403) throw new GitHubApiException(503, "A integração com o GitHub está indisponível. Tente novamente mais tarde.");
            if (status == 422) throw new BusinessException("O GitHub não reconheceu esta pesquisa. Revise os termos e filtros.");
            throw new GitHubApiException(502, "Não foi possível consultar o GitHub. Tente novamente mais tarde.");
        } catch (ResourceAccessException e) {
            log.warn("Consulta GitHub interrompida por timeout ou falha de rede ({})", e.getClass().getSimpleName());
            throw new GitHubApiException(504, "O GitHub não respondeu a tempo. Tente novamente.");
        } catch (RestClientException e) {
            log.warn("Resposta GitHub inválida ({})", e.getClass().getSimpleName());
            throw new GitHubApiException(502, "Não foi possível ler os dados do GitHub. Tente novamente.");
        }
    }

    public List<UserSummary> buscarUsuarios(String termo, String linguagem, String localizacao, int quantidade) {
        StringBuilder query = new StringBuilder("type:user");
        if (termo != null && !termo.isBlank()) query.append(' ').append(termo.trim());
        if (linguagem != null && !linguagem.isBlank()) query.append(" language:").append(qualifier(linguagem));
        if (localizacao != null && !localizacao.isBlank()) query.append(" location:").append(qualifier(localizacao));
        int size = Math.max(1, Math.min(quantidade, 30));
        String key = query + "|" + size;
        SearchCache cached = searches.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.users();

        SearchUsersResponse response = chamar(() -> githubRestClient.get()
                .uri(builder -> builder.path("/search/users").queryParam("q", query.toString())
                        .queryParam("per_page", size).build())
                .retrieve().body(SearchUsersResponse.class));
        if (response.items == null || response.incompleteResults) {
            throw new GitHubApiException(502, "O GitHub não concluiu a pesquisa. Tente uma consulta mais específica.");
        }
        List<UserSummary> users = List.copyOf(response.items);
        searches.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(Instant.now()));
        if (searches.size() >= 128) searches.clear();
        if (searchCacheSeconds > 0) searches.put(key, new SearchCache(Instant.now().plusSeconds(searchCacheSeconds), users));
        return users;
    }

    private String qualifier(String value) {
        return '"' + value.trim().replace("\\", "").replace("\"", "") + '"';
    }

    public UserDetail buscarDetalhesUsuario(String username) {
        return chamar(() -> githubRestClient.get().uri("/users/{username}", username)
                .retrieve().body(UserDetail.class));
    }

    /** Uma página de repositórios atualizados; não representa todo o histórico do perfil. */
    public List<RepoSummary> buscarRepositorios(String username, int quantidade) {
        return chamar(() -> githubRestClient.get()
                .uri(builder -> builder.path("/users/{username}/repos").queryParam("sort", "updated")
                        .queryParam("per_page", Math.max(1, Math.min(quantidade, 100))).build(username))
                .retrieve().body(new ParameterizedTypeReference<List<RepoSummary>>() {}));
    }
}
