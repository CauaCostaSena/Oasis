# Banco e migrações — etapa 1

Nenhuma conexão ou migração foi executada contra o MySQL do usuário nesta etapa. Testes H2 não comprovam DDL, triggers ou procedures em MySQL.

## Instalação nova

Execute 01_schema.sql → 02_views_routines.sql → 03_data_manipulation.sql. O arquivo 00_setup_completo.sql é a concatenação desses arquivos, sem DROP DATABASE e sem dados fictícios. Use uma das alternativas, não ambas como migração. Seed preserva valores/nomes legados e só preenche planos quando a tabela está vazia.

## Banco existente

1. Fazer backup completo de dados e rotinas; restaurar em cópia isolada para validar recuperação.
2. Inspecionar SHOW CREATE TABLE das oito tabelas e comparar com 01_schema e JPA. CREATE IF NOT EXISTS não atualiza tabelas existentes.
3. Inventariar planos. Entrada deve ser ATIVO, preço zero e nome reconhecido (Free, Gratuito, Básico, Oásis Básico) ou configurado via CKGD_FREE_PLAN_NAME. Não escolher Enterprise pelo preço, renomear IDs nem reassociar empresas automaticamente.
4. Se empresa_candidato.nota não existir, adicionar coluna nullable DECIMAL(4,2) na cópia. A entidade agora exige essa coluna.
5. Se já existir, conferir valores antes de mudar restrições:

```sql
SELECT fk_empresa_cnpj, fk_candidato_node_id, nota
FROM empresa_candidato
WHERE nota IS NOT NULL
  AND (nota < 1 OR nota > 5 OR nota <> FLOOR(nota));
```

A escala 1–5 foi confirmada em 27/09/2026. Notas antigas precisam de decisão explícita sobre conversão, sem regra de três, arredondamento ou exclusão silenciosa. Mesmo valores de 1–5 podem ter sido dados numa escala 0–10: confirmar seu significado. Preservar backup e bloquear migração até resolver os registros.

6. Depois de resolver valores e conferir o nome real da constraint, substituir a restrição antiga por `CHECK (nota IS NULL OR (nota >= 1 AND nota <= 5 AND nota = FLOOR(nota)))`.
7. Conferir privada=false. A API não publica esses registros e restringe consultas ao CNPJ autenticado. Normalizar a marca para TRUE na cópia e adicionar/verificar CHECK (privada = TRUE), registrando a mudança.
8. Candidato não autentica na aplicação. Credenciais antigas não são usadas; separar e-mail público do GitHub de e-mail de autenticação antes de remover colunas. Remoção física depende da migração revisada; não apagar informações públicas por nome de coluna.
9. Conferir telefone/foto_url de empresa, tamanhos, FKs, tabela de suporte e restrição EMPRESA. Suportes históricos de candidato precisam de decisão de arquivamento antes de ativar essa constraint.
10. Recriar rotinas com 02_views_routines.sql somente após alinhar tabelas e notas. Validar app com ddl-auto=validate, cadastro gratuito, login, favoritos e isolamento entre empresas.
11. Registrar comandos realmente aplicados e resultados na cópia antes de alterar a base original. DDL MySQL pode realizar commit implícito; não presumir rollback de toda migração.

## Rotinas do TCC

| Rotina | Classificação | Observação |
|---|---|---|
| trg_data_cadastro_empresa | Manter/adicionar se ausente | Preenche somente data não informada. |
| vw_empresa_plano | Manter/adicionar se ausente | Plano associado à empresa. |
| sp_listar_avaliacoes_empresa | Ajustar em bases antigas | Inclui comentário/nota; CNPJ parâmetro não substitui autenticação. |
| fn_pode_avaliar_empresa | Ajustar em bases antigas | NOT DETERMINISTIC; avaliações não são favoritos. |

SQL SECURITY INVOKER não cria isolamento por empresa sozinho. Rotinas administrativas não devem ser endpoints públicos. sp_registrar_busca é acadêmica e independente; a API usa BuscaService com bloqueio e limite.

## Próximas migrações

- node_id físico → github_user_id: inventariar FKs e manter compatibilidade antes de trocar.
- URL PK → github_repository_id: preencher IDs estáveis e conferir unicidade; URL pode permanecer UNIQUE.
- Free/Pro/Enterprise → nomes Oásis: confirmar catálogo, preços, limites e periodicidade.
- Contadores por período e comparação: confirmar regra de consumo/renovação sem inventar reset.
