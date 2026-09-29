-- Oásis — seed de instalação, sem empresas/candidatos/avaliações fictícios.
-- Os nomes, valores e limites abaixo JÁ EXISTIAM no projeto. Não representam
-- definição comercial nova. O cadastro deve selecionar Free, nunca Enterprise
-- apenas por custar zero. Zero nos limites mantém a convenção legada: ilimitado.
-- Se qualquer plano já existir, este script não altera nem acrescenta planos:
-- isso evita duplicar planos equivalentes (Free/Gratuito/Básico) em bases antigas.
USE ckgd;

INSERT INTO plano_de_assinatura (
    nome_plano, preco_plano, periodicidade, limite_requisicao,
    limite_avaliacao, limite_comparacao, status_plano, data_ativacao, data_expiracao
)
SELECT p.nome, p.preco, p.periodicidade, p.buscas, p.avaliacoes, p.comparacoes,
       'ATIVO', CURRENT_DATE, NULL
FROM (
    SELECT 'Free' AS nome, CAST(0.00 AS DECIMAL(10,2)) AS preco,
           'MENSAL' AS periodicidade, 10 AS buscas, 5 AS avaliacoes, 3 AS comparacoes
    UNION ALL
    SELECT 'Pro', 199.90, 'MENSAL', 200, 100, 50
    UNION ALL
    SELECT 'Enterprise', 0.00, 'ANUAL', 0, 0, 0
) AS p
WHERE NOT EXISTS (SELECT 1 FROM plano_de_assinatura);
