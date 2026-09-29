package com.ckgd;

import com.ckgd.service.GitHubService;
import com.ckgd.exception.GitHubApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GitHubServiceTest {
    @Test void rateLimitNaoViraResultadoVazio() {
        var builder=RestClient.builder().baseUrl("https://api.github.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .headers(new HttpHeaders() {{ add("X-RateLimit-Remaining","0"); }}).body("rate limit"));
        var service=new GitHubService(builder.build());
        assertThatThrownBy(() -> service.buscarUsuarios("java",null,null,12)).isInstanceOfSatisfying(GitHubApiException.class,e -> assertThat(e.getStatus()).isEqualTo(429));
        server.verify();
    }
    @Test void respostaVaziaRealPodeSerCacheadaSemNovaChamada() {
        var builder=RestClient.builder().baseUrl("https://api.github.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withSuccess("{\"items\":[],\"incomplete_results\":false}",MediaType.APPLICATION_JSON));
        var service=new GitHubService(builder.build());
        assertThat(service.buscarUsuarios("java",null,null,12)).isEmpty();
        assertThat(service.buscarUsuarios("java",null,null,12)).isEmpty();
        server.verify();
    }
    @Test void pesquisaIncompletaNaoViraSucesso() {
        var builder=RestClient.builder().baseUrl("https://api.github.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withSuccess("{\"items\":[],\"incomplete_results\":true}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new GitHubService(builder.build()).buscarUsuarios(null,null,null,12)).isInstanceOf(GitHubApiException.class);
        server.verify();
    }
}
