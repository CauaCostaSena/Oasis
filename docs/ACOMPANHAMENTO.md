# Oásis — acompanhamento por etapas

Atualizado em 30/09/2026.

## Estado por fase

| Etapa | Estado | Entrega / pendência |
|---|---|---|
| 0 — Auditoria | Concluída | Diagnóstico, matriz e documentação existentes revisados. |
| 1 — Estrutural e segurança | Concluída no escopo atual | Conta de candidato removida, plano gratuito no backend, segurança/XSS/CORS/JWT endurecidos, SQL e testes preservados. |
| 2 — Design system | Avançada | Tokens, UI compartilhada, shell autenticado, cards, modais, toasts, loaders, foco e reduced-motion. |
| 3 — Landing e autenticação | Implementada | Landing pública, hero, ticker, seções, FAQ, login/cadastro empresarial e redirecionamento para dashboard. |
| 4 — Dashboard / busca / favoritos | Implementada com validação final pendente | Dashboard, busca, filtros, skeletons, filtro mobile, favoritos e navegação completa. |
| 5 — Perfil e comparação | Implementada | Perfil técnico, avaliação privada e comparação de 2–3 candidatos. Limite de comparação ainda não é consumido. |
| 6 — Insights e assinatura | Parcial funcional | Insights usam histórico/favoritos/avaliações reais. Assinatura consulta catálogo real; mudança de plano segue bloqueada por decisão comercial. |
| 7 — Polimento | Avançada, não encerrada | Responsividade e motion implementados; validação visual final em dispositivos reais e performance de produção continuam necessárias. |

## Regras preservadas

- somente empresas possuem conta;
- candidatos vêm de dados públicos do GitHub;
- cadastro empresarial recebe plano gratuito automaticamente no backend;
- favorito e avaliação são conceitos separados;
- avaliações são privadas por empresa;
- comparação não cria vencedor;
- não há preços, limites, cobrança, reset ou gateway inventados;
- identificadores técnicos históricos continuam preservados quando uma migração poderia ser destrutiva.

## Funcionalidades adicionadas nesta etapa

- landing pública independente do backend para renderização;
- dashboard autenticado;
- sidebar única com navegação para todas as áreas;
- seleção de candidatos para comparação;
- comparação lado a lado;
- histórico privado de até 30 buscas via `/api/busca/historico`;
- Insights derivados desse histórico e de favoritos/avaliações;
- página dedicada de assinatura;
- página dedicada de perfil da empresa;
- filtro mobile em drawer;
- skeletons de busca e loader de filtro;
- indicador de comparação;
- CI para frontend + Maven.

## Pendências de decisão

1. cobrança e gateway;
2. ativação real de troca de plano;
3. regra de consumo/reset do limite de comparações;
4. recuperação de senha por canal verificado;
5. migração de IDs técnicos e nomes históricos;
6. migração do repositório para `githubRepositoryId`;
7. validação com MySQL real e GitHub ao vivo em ambiente controlado.

## Validação

Execute:

```powershell
node scripts/verificar-frontend.cjs
mvn -f backend/pom.xml test
```

A workflow de CI executa os mesmos checks no GitHub.
