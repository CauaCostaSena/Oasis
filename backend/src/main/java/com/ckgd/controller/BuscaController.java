package com.ckgd.controller;

import com.ckgd.dto.CandidatoResponse;
import com.ckgd.service.BuscaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/busca")
public class BuscaController {

    private final BuscaService buscaService;

    public BuscaController(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    @GetMapping
    public ResponseEntity<List<CandidatoResponse>> buscar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String linguagem,
            @RequestParam(required = false) String localizacao,
            Authentication authentication) {

        String cnpj = (String) authentication.getPrincipal();

        List<CandidatoResponse> resultado = buscaService.executarBusca(cnpj, termo, linguagem, localizacao);

        return ResponseEntity.ok(resultado);
    }
}
