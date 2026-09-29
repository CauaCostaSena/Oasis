# Oásis — acompanhamento por etapas

Atualizado em 27/09/2026. Este arquivo é o ponto de retomada do projeto.

## Escopo desta execução

Auditar o material recebido e concluir uma etapa de recuperação dos fluxos existentes. Não declarar o projeto inteiro concluído ao terminar esta etapa.

| Etapa | Estado | Entrega / pendência |
|---|---|---|
| 0 — Auditoria | Concluída no código; ambiente real pendente | Ver DIAGNOSTICO.md. Não há .git nesta pasta. |
| 1 — Recuperação estrutural e segurança | Concluída no escopo desta entrega | Fluxos recuperados e 11 testes passaram. Migração MySQL real e demais itens seguem pendentes. |
| 2 — Design system | Parcial | Tokens, CSS e UI conectados às telas; menu móvel, foco e toast validados parcialmente. |
| 3 — Landing e autenticação | Parcial | Autenticação corrigida; landing pública e animações avançadas pendentes. |
| 4 — Dashboard | Parcial | Contratos, favoritos e navegação recuperados; busca ao vivo e filtros avançados pendentes de validação. |
| 5 — Perfil e comparação | Parcial | Perfil/avaliação privada com nota 1–5; comparação ainda não implementada. |
| 6 — Insights e assinatura | Pendente | Dados reais, limites e troca de plano precisam de implementação e decisões. |
| 7 — Polimento | Pendente | Revisão visual completa, performance, loaders de referência e acessibilidade. |

## Decisões

- Confirmado pelo usuário nesta conversa: notas de **1 a 5**.
- Somente empresas autenticam. Candidatos são dados públicos do GitHub.
- Cadastro sempre gratuito, escolhido pelo backend.
- Preços, limites e periodicidades existentes são legados, não novas decisões comerciais.
- Não definir renovação/reset, cobrança, gateway, ranking ou score.
- Imagens, vídeos e BASE de referência citados no prompt não estão nesta pasta; não foram analisados visualmente.

## Próximos passos após esta etapa

1. Validar instalação e migração em cópia do MySQL real.
2. Criar landing pública e concluir shell responsivo utilizando CSS existente.
3. Implementar comparação e histórico; confirmar a forma de consumo do limite de comparação.
4. Implementar Insights privados e assinatura com regras comerciais confirmadas.
5. Validar todas as larguras, teclado e animações com as referências fornecidas.

### Validação realizada

- Maven package: sucesso; 11 testes, 0 falhas, 0 erros.
- Oito HTML: caminhos, referências de IDs, imports CSS e sintaxe JS passaram.
- Login/logout reais no navegador com API Java e banco H2 descartável; nenhum MySQL alterado.
- Menu móvel, modal com Escape/foco, estados vazios de favoritos/avaliações e configurações revisados.
- Cadastro: larguras 1920, 1366, 768 e 390 verificadas sem overflow. Busca: 1920/1366/390. Configurações: 768/390; overflow do upload corrigido.
- Runtime disponível foi Java 25 com compilação release 21; falta repetir em Java 21.
- Ver [matriz do prompt](MATRIZ-PROMPT.md) e [relatório](RELATORIO-ETAPA-1.md).

## Como retomar

Ler este arquivo, DIAGNOSTICO.md, RELATORIO-ETAPA-1.md e BANCO-E-MIGRACOES.md. Conferir o estado dos arquivos e executar os testes antes de novas alterações. Nunca executar scripts sobre um banco existente sem backup e análise de divergências.
