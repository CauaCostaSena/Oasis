# Relatório — recuperação dos fluxos existentes

Data: 27/09/2026. Esta é uma etapa concluída do trabalho, não a conclusão de todo o prompt.

## O que mudou

- Login/cadastro conectados ao formulário correto, ao cliente API e aos componentes compartilhados; login.html criado, index mantido como acesso provisório.
- Cadastro sem seleção de plano, com gratuito atribuído no backend e testado mesmo quando o cliente envia ID pago.
- HTML alinhado aos estilos recebidos; marca Oásis, navegação acessível, campos rotulados, links e ações reais.
- Favoritar/remover favorito separados de avaliar; tela permite consultar também avaliações não favoritas.
- Avaliações restritas ao CNPJ da sessão, sempre privadas; nota inteira opcional 1–5 conforme decisão do usuário. Limite existente aplicado com bloqueio por empresa; editar não conta novamente.
- Conteúdo externo renderizado por DOM/textContent, URLs verificadas. Removido innerHTML dos scripts ativos.
- Recuperação indisponível informa o estado sem formulário de senha. Conta de candidato retirada das telas/scripts; rotas antigas seguem negadas.
- Upload confere conteúdo real e dimensão, regrava PNG; JPEG/PNG aceitos. Fotos WebP antigas continuam legíveis. Nome/telefone/endereço e notas têm validações alinhadas.
- Setup SQL não apaga banco e não insere pessoas fictícias; schema novo usa nota 1–5. Nenhuma migração executada na base real.

## Arquivos

Relação completa de criados/alterados/removidos: [MANIFESTO-ETAPA-1.md](MANIFESTO-ETAPA-1.md), comparada com o backup anterior. A pasta recebida não tem .git, por isso não foi possível fazer git diff contra histórico ou criar um commit confiável.

Removidos após backup: cadastro-candidato.html, meu-perfil.html, js/cadastro-candidato.js e js/meu-perfil.js.

Movidos para arquivo histórico externo: atividade80%_anderson.zip e Capture-and-Keep-Good-Devs---CKGD-main.zip. backend/target permanece gerado e ignorado. .github/modernize não existia.

Backup dos fontes anteriores: `C:/Users/cauac/.codex/visualizations/2026/09/27/01a0e4a7-ac9d-7380-89d1-51fd93c31401/oasis-antes-etapa1.zip`.

No ZIP, `src/` corresponde a `backend/src/`, e `pom.xml` a `backend/pom.xml`; demais pastas mantêm nomes. Os ZIPs históricos estão em `historico-oasis/` ao lado desse backup. Não houve descarte de alterações anteriores.

## Visual e animações

Reutilizados tokens, temas e componentes CSS já recebidos. Login/cadastro escuros; área empresarial clara. Shell móvel com menu, toasts, foco visível e modal nativo; transições curtas de entrada/hover existentes conectadas às telas e reduced-motion preservado. Não foram implementados os loaders, hero ou animações dos vídeos de referência, que não estavam disponíveis.

## Testes e resultados

| Verificação | Resultado |
|---|---|
| Maven package | Sucesso, pacote backend/target/ckgd-backend.jar |
| FluxosEmpresaTest | 6 testes passaram |
| GitHubServiceTest | 3 testes passaram, sem rede real |
| PlanoGratuitoServiceTest | 2 testes passaram |
| Total | 11 testes, 0 falhas, 0 erros, 0 ignorados |
| Sintaxe JS, IDs usados, paths HTML e imports CSS | Passaram nas 8 páginas; nenhum innerHTML ativo |
| Login/logout no navegador | Sucesso com API Java e conta de teste em H2 temporário |
| Favoritos/avaliações vazios | Mensagens distintas e navegação verificadas |
| Menu móvel e suporte | Abertura, Escape e devolução de foco verificados |
| Responsividade | Cadastro: 1920/1366/768/390; busca: 1920/1366/390; configurações: 768/390 |

Foram detectados e corrigidos z-index de filtros acima do menu e overflow do seletor de arquivo no mobile. Configurações em 390 px passaram a ter conteúdo de 375 px (com barra de rolagem), sem excesso horizontal.

Testes verificam: cadastro HTTP gratuito sem frontend, senha em hash, login correto/incorreto, isolamento entre empresas, nota, limite de avaliação, preservação de avaliação ao remover favorito, bloqueio de candidato/recuperação insegura, CORS, arquivo falso/PNG válido, rate limit, resposta vazia legítima/cache, resposta incompleta e nomes legados de plano.

## Limites da validação

- Java disponível: JDK 25; Maven compilou com release 21. Falta executar em runtime Java 21.
- MySQL real não foi acessado. H2 não valida todas as particularidades dos scripts/rotinas MySQL.
- Não houve consulta real ao GitHub nesta etapa; o serviço foi testado com respostas simuladas.
- Perfil com dados reais, cards extensos, falhas de rede no navegador e todas as combinações de telas/breakpoints precisam de revisão adicional.
- As imagens/vídeos/BASE citados no prompt não estão na pasta e não foram analisados.
- Sem publicação, commit, cobrança ou mudança de plano comercial.

## Problemas e decisões pendentes

P1: migração MySQL revisada, credenciais legadas de candidato no banco antigo, validação Java 21, busca GitHub ao vivo, comparação, histórico, Insights e assinatura. Confirmar consumo/reset de limites de comparação, preços, periodicidades e cobrança antes de implementá-los.

P2: landing pública, drawer de filtros, loaders/skeletons, revisão visual completa, contraste/teclado e performance. Reformatar progressivamente CSS minificado recebido para manutenção.

P3: recuperação por token/canal verificado, estratégia de sessão/revogação, exportação, tema alternativo, IDs estáveis de repositórios.

Próxima etapa recomendada: validar migração em cópia do MySQL e concluir landing/autenticação visual, seguindo [ACOMPANHAMENTO.md](ACOMPANHAMENTO.md). Nunca converter notas legadas automaticamente.
