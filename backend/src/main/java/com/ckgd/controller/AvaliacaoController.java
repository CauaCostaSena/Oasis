package com.ckgd.controller;

import com.ckgd.dto.AvaliacaoRequest;
import com.ckgd.dto.AvaliacaoResponse;
import com.ckgd.service.FavoritoAvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** O CNPJ sempre vem da sessão; nunca é escolhido pelo cliente. */
@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {
    private final FavoritoAvaliacaoService service;
    public AvaliacaoController(FavoritoAvaliacaoService service) { this.service = service; }

    @GetMapping
    public List<AvaliacaoResponse> listar(Authentication auth) {
        return service.listarAvaliacoes(auth.getName()).stream().map(AvaliacaoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AvaliacaoResponse buscar(@PathVariable Long id, Authentication auth) {
        return AvaliacaoResponse.from(service.buscarAvaliacao(auth.getName(), id));
    }

    @PutMapping("/{id}")
    public AvaliacaoResponse salvar(@PathVariable Long id, @Valid @RequestBody AvaliacaoRequest req, Authentication auth) {
        // Avaliar não altera favoritos. A rota legada /favoritos mantém compatibilidade.
        req.setFavorito(null);
        return AvaliacaoResponse.from(service.salvarAvaliacao(auth.getName(), id, req));
    }
}
