# Diagnóstico da pasta recebida — 27/09/2026

## Arquitetura

Aplicação multipágina HTML/CSS/JavaScript → js/api.js → controllers Spring → services → repositories JPA → MySQL. GitHubService usa REST do GitHub. Preservar Java 21 e Spring Boot; não introduzir framework frontend. Há 7 controllers, serviços de empresa, busca, candidato, favoritos, suporte e plano gratuito. Nenhum teste foi entregue.

## Backend

Cadastro já usa PlanoGratuitoService e ignora idPlano pago. Autenticação de candidato já foi retirada. Busca possui bloqueio por empresa e contagem acumulada; cache e erros do GitHub foram parcialmente implementados. Favoritos ainda aceitam privada=false; js/api.js chama /avaliacoes, mas esse controller não existe. DTOs retornados após fechar transação podem acessar relacionamentos lazy de favoritos. Falta aplicação do limite de avaliações e nota na entidade.

## Banco

01_schema e 02_views_routines foram atualizados, mas 00_setup_completo continua destrutivo (DROP DATABASE), recria candidato com autenticação e inclui dados fictícios. O schema novo contém nota de 0–10 sem confirmação no prompt; usuário confirmou 1–5 nesta conversa. CREATE IF NOT EXISTS não atualiza bancos antigos. ddl-auto=validate é adequado para detectar divergências, mas faltam instruções de migração. DECIMAL(10,2) está correto. Nenhum banco real foi alterado nesta auditoria.

Rotinas: manter trg_data_cadastro_empresa e vw_empresa_plano; ajustar sp_listar_avaliacoes_empresa e fn_pode_avaliar_empresa junto ao contrato de notas. NOT DETERMINISTIC já foi corrigido em 02; 00 mantém versão antiga incorreta. sp_registrar_busca não aplica limite por si; não expor como API de negócio.

## GitHub API

Erros são distinguidos de resultado vazio; há timeouts, cache de busca de 60 segundos e perfil de 15 minutos. Cada busca fria pode fazer 25 chamadas: uma busca e até duas por candidato (12). Métricas usam amostra de até 30 repositórios recentes sem forks; não são totais do histórico. Linguagem é frequência nessa amostra. SQL/JPA usam node_id numérico legado; DTO público já oferece githubUserId. URL ainda é PK de repositório. Priorizar futura migração aditiva para IDs estáveis, sem perda de dados.

## Segurança

JWT exige segredo externo e empresa; CORS tem origens explícitas; recuperação por CNPJ/e-mail já está desativada no serviço. P0: innerHTML com nomes, descrições e comentários externos em perfil/favoritos. A interface de recuperação ainda solicita nova senha com GET de formulário possível, embora o JS não implemente envio: remover formulário imediatamente. Upload confia no MIME declarado; validar conteúdo real em etapa posterior. JWT em localStorage demanda cuidado permanente com XSS; mudança de estratégia de sessão exige análise própria.

## Frontend

HTML antigo e CSS novo são incompatíveis. Login JS procura form-login e form-error inexistentes. Cadastro usa OasisUI que não é carregado. Proteção redireciona para login.html inexistente. Suporte também depende de OasisUI ausente. Favoritar chama rota /avaliacoes inexistente. Perfil mistura favorito/avaliação. Conta de candidato persiste em dois HTML e dois JS sem backend correspondente. Configuração de senha ainda exige 6 caracteres, backend exige 8.

## UX/UI

Tokens e componentes visuais já foram criados, mas as telas usam classes antigas. Sidebar fica inacessível no mobile sem botão de abertura. Há ícones/divs/links sem href usados como ações. Identidade visível ainda é CKGD. Ações precisam estados de loading, erro e proteção de envio duplicado. Não existe landing pública, comparação, histórico visível, Insights ou alteração de plano. Não anunciar como prontos.

## Organização

git status falha: não há repositório .git. Sem base confiável para git diff/histórico. Backup externo à árvore ativa será preservado antes das alterações. Dois ZIPs históricos não são referenciados pela aplicação e serão arquivados fora dela. backend/target é gerado e ignorado; não reutilizar build antigo como evidência. .github/modernize não existe. Não reorganizar pastas de frontend nesta etapa.

## Plano de arquivos da etapa 1

- Criar docs de diagnóstico, acompanhamento, relatório e migrações; login.html; testes backend e verificações frontend.
- Alterar HTML existentes para contratos reais e identidade Oásis; js/home, perfil, favoritos, config e ui/api quando necessário; estilos de compatibilidade apenas onde necessário.
- Alterar FavoritoAvaliacaoService, EmpresaCandidato, DTOs, repository; criar AvaliacaoController; incluir limites já existentes e isolamento por empresa.
- Atualizar README, SQL de instalação e validações de dados; preservar nomes técnicos com.ckgd, schema ckgd, CKGD_* e chaves ckgd_*.
- Remover cadastro-candidato.html, meu-perfil.html e seus dois JS após backup; arquivar os dois ZIPs fora do projeto.
- Não executar migrações no banco do usuário; não mudar preços/limites comerciais.

## Backlog priorizado

P0: recuperar login e navegação; corrigir XSS; remover formulário inseguro e SQL destrutivo; assegurar privacidade.

P1: cadastro gratuito testado; favoritos separados de avaliações; notas 1–5; migração MySQL validada; comparação, histórico, Insights; aplicação de limites e assinatura.

P2: landing, responsividade em 1920/1366/tablet/mobile, revisão teclado/contraste, skeletons, loaders SVG e filtros, remover dependências CDN desnecessárias, tornar CSS legível em módulos.

P3: recuperação de senha por token com canal verificado, cobrança real (decisão pendente), tema alternativo, exportação de dados; ID estável de repositório e paginação técnica.
