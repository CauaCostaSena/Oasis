# Relatório — Etapa 2: produto visual e fluxos

Data: 30/09/2026

## Objetivo

Dar continuidade ao prompt mestre do Oásis após a etapa estrutural, priorizando as fases 2–7 sem reconstruir o projeto do zero e sem inventar regras comerciais.

## Arquivos criados

- `dashboard.html`, `css/dashboard.css`, `js/dashboard.js`
- `comparacao.html`, `css/comparacao.css`, `js/comparacao.js`
- `insights.html`, `css/insights.css`, `js/insights.js`
- `assinatura.html`, `css/assinatura.css`, `js/assinatura.js`
- `empresa.html`, `css/empresa.css`, `js/empresa.js`
- `css/landing.css`, `js/landing.js`
- `css/app-extra.css`
- `backend/src/main/java/com/ckgd/dto/BuscaHistoricoResponse.java`
- `.github/workflows/ci.yml`
- este relatório

## Arquivos alterados

- `index.html`
- `login.html`, `cadastro.html`, `recuperar-senha.html`
- `home.html`, `favoritos.html`, `perfil.html`, `config.html`
- `css/tokens.css`, `css/home.css`
- `js/api.js`, `js/ui.js`, `js/login.js`, `js/cadastro.js`, `js/home.js`, `js/favoritos.js`, `js/perfil.js`
- `backend/src/main/java/com/ckgd/service/BuscaService.java`
- `backend/src/main/java/com/ckgd/controller/BuscaController.java`
- `README.md`
- `docs/ACOMPANHAMENTO.md`
- `docs/MATRIZ-PROMPT.md`

## Funcionalidades alteradas

### Landing
`index.html` deixa de duplicar o login e passa a ser uma página pública real, renderizável mesmo se o backend estiver desligado.

### Autenticação
Login e cadastro redirecionam para `dashboard.html`. Cadastro continua empresarial e plano gratuito continua definido pelo backend.

### Dashboard
Adicionada visão geral com favoritos, avaliações, histórico recente e plano atual.

### Busca
Mantidos filtros compatíveis com GitHub. Adicionados skeletons, loader de filtros, drawer mobile e seleção para comparação.

### Comparação
Até 3 candidatos podem ser selecionados a partir da busca, perfil ou favoritos. A comparação exibe dados lado a lado sem score ou vencedor.

### Histórico / Insights
Novo endpoint privado `GET /api/busca/historico` retorna no máximo 30 buscas da empresa autenticada. Insights de linguagem e localização usam somente esses dados reais.

### Assinatura
A página consulta `/api/planos` e `/api/empresas/me`. Não existe botão falso de cobrança: planos diferentes aparecem com alteração indisponível enquanto as regras comerciais não forem definidas.

### Perfil da empresa
Criada tela própria para consulta dos dados empresariais; edição permanece em Configurações.

## Correções de UX/UI

- identidade visual consistente entre dark public/auth e light app;
- sidebar completa e centralizada no `OasisUI`;
- navegação entre todas as telas;
- card hover discreto;
- motion com `transform`/`opacity`;
- `prefers-reduced-motion`;
- filtro mobile;
- toasts e modais existentes preservados;
- comparação flutuante quando existem candidatos selecionados;
- nenhum dado externo inserido com `innerHTML`.

## Segurança

Nenhuma regra de autenticação de candidato foi reintroduzida. A avaliação continua privada. O novo histórico deriva o CNPJ da autenticação e não aceita CNPJ enviado pelo frontend.

## O que não foi implementado de propósito

- pagamento/gateway;
- alteração real de plano;
- reset/renovação dos limites;
- consumo do limite de comparação;
- ranking/score de candidato;
- recuperação de senha por CNPJ/e-mail;
- migração destrutiva de schema/pacotes/IDs legados.

Esses itens dependem de decisões de produto ou migração segura.

## Validação

A etapa inclui CI com:

```text
node scripts/verificar-frontend.cjs
mvn -f backend/pom.xml test
```

Antes do merge, conferir o resultado da workflow no Pull Request e fazer validação visual em desktop, tablet e smartphone.
