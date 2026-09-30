# Oásis

O **Oásis** é uma plataforma web B2B de recrutamento técnico baseada em informações públicas do GitHub. O sistema organiza candidatos para apoiar decisões humanas; não é rede social e não cria ranking automático de pessoas.

**Somente empresas possuem conta.** Desenvolvedores aparecem como candidatos obtidos de dados públicos do GitHub: não se cadastram, não possuem senha e não editam perfis no Oásis. Todo cadastro empresarial recebe automaticamente o plano gratuito no backend.

## Estado atual

A base estrutural e de segurança foi recuperada e a interface foi evoluída para um produto multipágina consistente.

Entregas presentes neste repositório:

- landing pública com identidade Oásis, hero, seções, FAQ, ticker e animações leves;
- login e cadastro exclusivos para empresas;
- plano gratuito automático no cadastro;
- dashboard autenticado;
- busca de candidatos e filtros compatíveis com o GitHub;
- cards com favorito, seleção para comparação e acesso ao perfil;
- perfil técnico, repositórios e avaliação privada;
- favoritos e avaliações internas;
- comparação de 2 a 3 candidatos sem vencedor automático;
- histórico recente de pesquisas da empresa;
- Insights privados derivados de dados reais da própria empresa;
- página de assinatura que exibe os planos cadastrados e o plano atual;
- perfil da empresa e configurações;
- responsividade, menu móvel, toasts, modais e `prefers-reduced-motion`.

Ainda permanecem pendentes decisões ou integrações que não devem ser inventadas:

- cobrança real e gateway de pagamento;
- alteração efetiva de plano;
- regra definitiva de consumo/reset do limite de comparações;
- recuperação de senha por canal verificado;
- migração física de identificadores técnicos históricos (`com.ckgd`, schema `ckgd`, `CKGD_*`, chaves `ckgd_*`);
- migração de repositório para ID estável do GitHub;
- validação final em MySQL de produção/homologação.

Consulte `docs/ACOMPANHAMENTO.md`, `docs/MATRIZ-PROMPT.md` e `docs/RELATORIO-ETAPA-2.md`.

## Stack

### Backend
- Java 21
- Spring Boot 3.3.13
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- JWT
- MySQL

### Frontend
- HTML5
- CSS3
- JavaScript sem framework

### Integração
- GitHub REST API

## Estrutura

```text
backend/                 API Java/Spring Boot
database/                schema, rotinas e dados iniciais
docs/                    diagnóstico, migrações e relatórios
css/                     tokens, base e estilos por página
js/                      API client, UI compartilhada e scripts por página
images/                  assets locais
scripts/                 servidor estático e verificador do frontend
*.html                   páginas multipágina do frontend
```

A migração para uma pasta `frontend/` continua opcional e não foi feita de forma massiva para evitar quebra de caminhos relativos.

## Fluxo principal

```text
Landing (index.html)
      ↓
Login / Cadastro
      ↓
Dashboard
      ├── Buscar candidatos
      ├── Favoritos
      ├── Comparar
      ├── Insights
      ├── Assinatura
      ├── Perfil da empresa
      └── Configurações
```

## Executar localmente

### 1. Banco e backend

Use Java 21, Maven e MySQL 8.0.16+.

Em banco novo, execute os scripts:

1. `database/01_schema.sql`
2. `database/02_views_routines.sql`
3. `database/03_data_manipulation.sql`

Configure as variáveis de ambiente necessárias. Exemplo PowerShell:

```powershell
$env:CKGD_DB_USER = 'seu_usuario_mysql'
$env:CKGD_DB_PASSWORD = 'sua_senha_mysql'
$env:CKGD_JWT_SECRET = 'segredo-aleatorio-com-pelo-menos-32-bytes'
$env:CKGD_GITHUB_TOKEN = 'token-opcional-do-github'
cd backend
mvn spring-boot:run
```

O segredo JWT não deve ser commitado.

### 2. Frontend

Na raiz do projeto:

```powershell
node scripts/servir-frontend.cjs
```

Abra `http://127.0.0.1:8090`.

Também é possível usar **Live Server** no VS Code. A landing (`index.html`) não depende do backend para renderizar; operações autenticadas precisam da API Java em `http://localhost:8080/api`.

Se o Live Server usar outra porta/origem, ajuste `CKGD_CORS_ORIGINS`.

## Variáveis principais

| Variável | Finalidade |
|---|---|
| `CKGD_DB_URL` | JDBC do MySQL |
| `CKGD_DB_USER` | Usuário do MySQL |
| `CKGD_DB_PASSWORD` | Senha do MySQL |
| `CKGD_JWT_SECRET` | Segredo JWT obrigatório |
| `CKGD_GITHUB_TOKEN` | Token opcional para aumentar limite de leitura pública |
| `CKGD_CORS_ORIGINS` | Origens permitidas, separadas por vírgula |
| `CKGD_FREE_PLAN_NAME` | Nome preferencial do plano gratuito |
| `CKGD_DDL_AUTO` | Estratégia JPA; padrão recomendado `validate` |

Os prefixos `CKGD_*` permanecem temporariamente por compatibilidade técnica. A marca visível é **Oásis**.

## API principal

| Método | Rota | Uso |
|---|---|---|
| POST | `/api/auth/cadastro` | Cria empresa e vincula plano gratuito |
| POST | `/api/auth/login` | Autentica empresa |
| POST | `/api/auth/redefinir-senha` | Indisponível até existir canal verificado |
| GET | `/api/empresas/me` | Dados da empresa autenticada |
| PUT | `/api/empresas/me` | Atualiza dados permitidos |
| PUT | `/api/empresas/me/senha` | Altera senha |
| POST | `/api/empresas/me/foto` | Atualiza imagem da empresa |
| GET | `/api/busca` | Pesquisa candidatos |
| GET | `/api/busca/historico` | Últimas 30 pesquisas da empresa |
| GET | `/api/candidatos/{id}` | Perfil técnico |
| GET | `/api/favoritos` | Favoritos da empresa |
| PUT/DELETE | `/api/favoritos/{id}` | Altera favorito |
| GET | `/api/avaliacoes` | Avaliações privadas da empresa |
| GET/PUT | `/api/avaliacoes/{id}` | Consulta/salva avaliação privada |
| GET | `/api/planos` | Catálogo atual de planos |
| POST | `/api/suporte` | Registra solicitação |

Rotas privadas exigem `Authorization: Bearer <token>`.

## Segurança

- BCrypt para senhas;
- JWT com segredo externo;
- CORS com origens explícitas;
- candidatos não autenticam;
- avaliações isoladas por CNPJ autenticado;
- dados externos renderizados com DOM seguro / `textContent`;
- upload de imagem validado e regravado;
- respostas de erro não devem expor stack trace ao usuário;
- recuperação de senha insegura permanece desativada.

## GitHub e métricas

O Oásis utiliza dados públicos e não transforma seguidores, estrelas ou quantidade de repositórios em um score profissional.

- localização permanece texto livre;
- idade não é inferida;
- senioridade não é inferida;
- linguagem principal é calculada a partir da amostra analisada;
- erros de API e rate limit são diferenciados de resultado vazio;
- cache/TTL reduzem chamadas repetidas.

## Comparação

A comparação é descritiva. Ela mostra dados lado a lado e nunca seleciona automaticamente um vencedor.

O consumo do `limite_comparacao` ainda não foi implementado porque reset, renovação e regra de consumo são decisões de negócio pendentes.

## Assinatura

O cadastro gratuito é funcional. A página de assinatura exibe o plano atual e os registros existentes em `/api/planos`.

A troca efetiva de plano continua desabilitada enquanto cobrança, gateway e regras comerciais não forem definidas.

## Validação

Frontend:

```powershell
node scripts/verificar-frontend.cjs
```

Backend:

```powershell
mvn -f backend/pom.xml test
```

A workflow `.github/workflows/ci.yml` executa essas duas validações em pushes e pull requests.

## Princípio do produto

> Dados públicos. Decisões humanas.

O Oásis organiza contexto técnico para apoiar recrutadores. A decisão de contratação continua pertencendo às pessoas.
