# Oásis

Plataforma B2B de recrutamento técnico baseada em informações públicas do GitHub. Organiza candidatos para apoiar decisões humanas; não é rede social nem ranking automático.

**Somente empresas possuem conta.** Candidatos não se cadastram, não têm senha e não editam dados. Todo cadastro empresarial recebe plano gratuito escolhido no backend. Favoritos e avaliações são independentes. Avaliações pertencem exclusivamente à empresa e aceitam nota inteira opcional de **1 a 5**, confirmada em 27/09/2026.

## Estado da entrega

A primeira etapa recuperou contratos entre telas e API, autenticação, favoritos, avaliações privadas e instalação SQL não destrutiva.

**O projeto completo ainda não está concluído.** Consulte [acompanhamento](docs/ACOMPANHAMENTO.md), [diagnóstico](docs/DIAGNOSTICO.md) e [relatório da etapa 1](docs/RELATORIO-ETAPA-1.md). `index.html` ainda oferece login. Landing pública, comparação, histórico visível, Insights, recuperação por e-mail e mudança de assinatura continuam pendentes.

## Arquitetura

- Java 21, Spring Boot 3.3.13, Web, JPA, Security, Validation e JWT.
- MySQL para aplicação; H2 em testes isolados.
- HTML/CSS/JavaScript sem framework; GitHub REST consultado pelo backend.

```text
backend/src/main/       controllers → services → repositories → entidades/MySQL
backend/src/test/       testes de cadastro, segurança, avaliações e GitHub
css/ e js/              estilos e lógica compartilhada/por tela
images/                 assets locais
database/               schema, rotinas e planos legados
docs/                   diagnóstico, migrações e acompanhamento
scripts/                validação e servidor estático de desenvolvimento
*.html                  telas existentes, caminhos preservados
```

Nomes técnicos `com.ckgd`, schema `ckgd`, `CKGD_*`, `ckgd.*` e chaves `ckgd_*` foram mantidos para compatibilidade. Marca visível: Oásis. `node_id` armazena ID numérico REST; `githubUserId` é o nome público correto com aliases antigos temporários. URL continua PK de repositório até migração própria.

## Executar

1. Configure Java 21, Maven e MySQL 8.0.16+. Confira `java -version` e `mvn -version`; JAVA_HOME deve apontar ao JDK correto.
2. Em **banco novo**, execute no MySQL Workbench `database/01_schema.sql`, `02_views_routines.sql` e `03_data_manipulation.sql`, nessa ordem. Alternativamente execute `00_setup_completo.sql`, que contém os mesmos três arquivos.
3. Para **banco existente**, siga [BANCO-E-MIGRACOES.md](docs/BANCO-E-MIGRACOES.md). CREATE IF NOT EXISTS não migra tabelas antigas.
4. Configure variáveis no terminal do backend. Exemplo PowerShell:

```powershell
$env:CKGD_DB_USER = 'seu_usuario_mysql'
$env:CKGD_DB_PASSWORD = 'sua_senha_mysql'
$env:CKGD_JWT_SECRET = 'substitua-por-segredo-aleatorio-de-pelo-menos-32-bytes'
cd backend
mvn spring-boot:run
```

Substitua o exemplo do segredo por valor aleatório próprio. Não salve credenciais reais nos arquivos. O segredo JWT é obrigatório. A API usa a porta 8080.

5. Em outro terminal, na raiz:

```powershell
node scripts/servir-frontend.cjs
```

Abra `http://127.0.0.1:8090`. Esse é apenas um servidor **estático local de desenvolvimento**, não substitui o backend Java. Também pode usar Live Server em origem permitida. Não abrir por `file://`.

## Configuração

| Variável | Finalidade/padrão |
|---|---|
| CKGD_DB_URL | JDBC; padrão MySQL local, schema ckgd |
| CKGD_DB_USER | Usuário MySQL; padrão root local |
| CKGD_DB_PASSWORD | Senha do seu MySQL |
| CKGD_JWT_SECRET | Obrigatória, pelo menos 32 bytes aleatórios |
| CKGD_GITHUB_TOKEN | Opcional, leitura pública GitHub, somente backend |
| CKGD_CORS_ORIGINS | Origens exatas separadas por vírgula, sem curingas |
| CKGD_FREE_PLAN_NAME | Nome preferencial gratuito; padrão Free |
| CKGD_DDL_AUTO | Padrão validate, não substituir migrações por update |

Propriedades adicionais: `ckgd.jwt.expiration-ms` (86400000), `ckgd.github.search-cache-seconds` (60), `ckgd.github.profile-cache-minutes` (15), `ckgd.upload.dir` (uploads, relativo ao diretório de execução). A URL JDBC padrão é local; configure TLS e credenciais apropriadas antes de implantação externa.

`js/api.js` usa `http://localhost:8080/api`. Para outro endereço, defina `window.OASIS_API_BASE_URL` antes de carregar o script e ajuste CORS.

## API

| Método | Rota | Finalidade |
|---|---|---|
| POST | /api/auth/cadastro | Público; cria empresa no gratuito; ignora idPlano enviado |
| POST | /api/auth/login | Público; sessão JWT empresarial |
| POST | /api/auth/redefinir-senha | Indisponível, 503 para payload válido; não altera senha |
| GET | /api/planos | Público; catálogo legado sem cobrança |
| GET / PUT | /api/empresas/me | Empresa; ler/editar nome e telefone |
| PUT | /api/empresas/me/senha | Empresa; exige senha atual |
| POST | /api/empresas/me/foto | Empresa; multipart arquivo JPEG/PNG, até 5 MB e 16 MP |
| GET | /api/busca?termo=&linguagem=&localizacao= | Empresa; pesquisa e uso registrado |
| GET | /api/candidatos/{id} | Empresa; perfil público técnico |
| GET | /api/favoritos | Favoritos da empresa autenticada |
| PUT / DELETE | /api/favoritos/{id} | Alterar favorito sem apagar avaliação |
| GET | /api/avaliacoes | Avaliações da empresa autenticada |
| GET / PUT | /api/avaliacoes/{id} | Ler/salvar nota e comentário; não altera favorito |
| POST | /api/suporte | Registra solicitação; não envia e-mail |

Rotas privadas exigem `Authorization: Bearer <token>`. CNPJ vem da sessão. Nota null remove nota; comentário vazio limpa comentário; campos omitidos preservam valores. A rota antiga de favoritos aceita nota/comentário por compatibilidade; a interface usa ações separadas.

## Segurança e limitações

- BCrypt, segredo JWT externo, CORS explícito, erros sem stack trace para usuário.
- Dados externos entram por DOM/textContent, sem innerHTML; URLs e avatares são restritos a destinos GitHub esperados.
- Upload valida conteúdo e regrava PNG; WebP antigo continua legível, novos uploads usam JPEG/PNG.
- Recuperação por dados públicos desativada; falta token temporário, expiração, uso único e canal verificado.
- JWT fica em localStorage (legado); estratégia de sessão e revogação após troca de senha ficam pendentes.
- GitHub possui cache/TTL e erros distintos de resultado vazio. Linguagens/estrelas usam amostra de até 30 repositórios recentes sem forks; não representam todo o histórico. Não inferimos idade/senioridade. Localização é texto livre.
- Free/Pro/Enterprise, preços e limites são legados, não novas decisões comerciais. Enterprise de preço zero não é selecionado como plano gratuito de entrada.
- Uso de buscas/avaliações permanece acumulado sem reset inventado. Novo candidato avaliado consome limite; editar não adiciona unidade; favorito não consome avaliação. Comparação e sua contagem estão pendentes.

## Testes

```powershell
mvn -f backend/pom.xml package
node scripts/verificar-frontend.cjs
```

Testes usam H2 e GitHub simulado, sem MySQL real ou token GitHub. Nesta execução, Maven compilou para release 21 e os testes rodaram no JDK 25 disponível. Validar runtime Java 21 e MySQL ainda é necessário. Consulte o relatório para resultados e limites da revisão visual.

Não há `.git` nesta cópia. Não houve commit/publicação. Compare com o repositório original antes de conectar o histórico; não descarte alterações anteriores.
