package com.ckgd.service;

import com.ckgd.entity.PlanoDeAssinatura;
import com.ckgd.exception.BusinessException;
import com.ckgd.repository.PlanoDeAssinaturaRepository;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlanoGratuitoService {
    private static final Set<String> NOMES_LEGADOS = Set.of("free", "gratuito", "basico", "oasis basico");
    private final PlanoDeAssinaturaRepository repository;
    private final String nomeConfigurado;

    public PlanoGratuitoService(PlanoDeAssinaturaRepository repository,
                               @Value("${ckgd.plano.gratuito-nome:Free}") String nomeConfigurado) {
        this.repository = repository;
        this.nomeConfigurado = normalizar(nomeConfigurado);
    }

    public PlanoDeAssinatura buscar() {
        List<PlanoDeAssinatura> gratuitos = repository.findAll().stream()
                .filter(p -> p.getStatusPlano() == PlanoDeAssinatura.StatusPlano.ATIVO)
                .filter(p -> p.getPrecoPlano() != null && p.getPrecoPlano().signum() == 0)
                .filter(p -> NOMES_LEGADOS.contains(normalizar(p.getNomePlano()))
                        || normalizar(p.getNomePlano()).equals(nomeConfigurado))
                .sorted(Comparator.comparing(PlanoDeAssinatura::getIdPlano))
                .toList();
        // Enterprise pode ter preço zero para orçamento; não ? o plano de entrada.
        return gratuitos.stream().filter(p -> normalizar(p.getNomePlano()).equals(nomeConfigurado))
                .findFirst().or(() -> gratuitos.stream().findFirst())
                .orElseThrow(() -> new BusinessException("Plano gratuito indisponível. Entre em contato com o suporte."));
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
