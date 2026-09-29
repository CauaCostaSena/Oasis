-- Oásis — views e rotinas (MySQL 8.0.16+).
-- Em banco antigo, execute antes a migração opt-in documentada em /docs.
-- Recria apenas definições de rotinas; não exclui tabelas ou registros.
-- Todas usam os privilégios de quem as chama (SQL SECURITY INVOKER).
USE ckgd;

CREATE OR REPLACE SQL SECURITY INVOKER VIEW vw_empresa_plano AS
SELECT e.cnpj, e.nome_empresa, e.data_cadastro,
       p.id_plano, p.nome_plano, p.preco_plano, p.periodicidade,
       p.limite_requisicao, p.limite_avaliacao, p.limite_comparacao,
       p.status_plano, p.data_ativacao, p.data_expiracao
FROM empresa e
JOIN plano_de_assinatura p ON p.id_plano = e.fk_plano_id_plano;

CREATE OR REPLACE SQL SECURITY INVOKER VIEW vw_favoritos_empresa AS
SELECT ec.fk_empresa_cnpj AS cnpj_empresa, e.nome_empresa,
       c.node_id, c.node_id AS github_user_id, c.nome_candidato, c.username,
       c.localizacao, c.linguagem_principal, ec.comentario, ec.nota,
       TRUE AS privada, ec.data_avaliacao
FROM empresa_candidato ec
JOIN empresa e ON e.cnpj = ec.fk_empresa_cnpj
JOIN candidato c ON c.node_id = ec.fk_candidato_node_id
WHERE ec.favorito = TRUE;

-- Uso acumulado conforme a implementação existente; sem reset/renovação novos.
-- Favoritar sem comentário nem nota não consome uma avaliação.
CREATE OR REPLACE SQL SECURITY INVOKER VIEW vw_uso_plano AS
SELECT e.cnpj, e.nome_empresa, p.nome_plano, p.limite_requisicao,
       (SELECT COUNT(*) FROM empresa_busca eb
        WHERE eb.fk_empresa_cnpj = e.cnpj) AS buscas_realizadas,
       p.limite_avaliacao,
       (SELECT COUNT(*) FROM empresa_candidato ec
        WHERE ec.fk_empresa_cnpj = e.cnpj
          AND (NULLIF(TRIM(ec.comentario), '') IS NOT NULL OR ec.nota IS NOT NULL)) AS avaliacoes_realizadas,
       p.limite_comparacao, p.status_plano, p.data_expiracao
FROM empresa e
JOIN plano_de_assinatura p ON p.id_plano = e.fk_plano_id_plano;

-- Agregação global para análise administrativa SQL. Não serve como endpoint
-- de Insights privado; o backend deve restringir estatísticas ao CNPJ autenticado.
CREATE OR REPLACE SQL SECURITY INVOKER VIEW vw_ranking_linguagens AS
SELECT filtro_linguagem AS linguagem, COUNT(*) AS total_buscas
FROM busca
WHERE NULLIF(TRIM(filtro_linguagem), '') IS NOT NULL
GROUP BY filtro_linguagem;

CREATE OR REPLACE SQL SECURITY INVOKER VIEW vw_candidato_resumo AS
SELECT c.node_id, c.node_id AS github_user_id, c.nome_candidato, c.username,
       c.localizacao, c.bio, c.linguagem_principal,
       COUNT(r.url_repositorio) AS total_repositorios,
       COALESCE(SUM(r.numero_estrela), 0) AS total_estrelas,
       COALESCE(SUM(r.numero_fork), 0) AS total_forks,
       COUNT(DISTINCT r.linguagem_principal) AS total_linguagens
FROM candidato c
LEFT JOIN repositorio r ON r.fk_candidato_node_id = c.node_id
GROUP BY c.node_id, c.nome_candidato, c.username,
         c.localizacao, c.bio, c.linguagem_principal;

DROP TRIGGER IF EXISTS trg_data_cadastro_empresa;
DROP PROCEDURE IF EXISTS sp_verificar_limite_busca;
DROP PROCEDURE IF EXISTS sp_registrar_busca;
DROP PROCEDURE IF EXISTS sp_listar_avaliacoes_empresa;
DROP FUNCTION IF EXISTS fn_contar_favoritos;
DROP FUNCTION IF EXISTS fn_pode_avaliar_empresa;

DELIMITER $$

-- Mantém a data histórica se ela tiver sido informada explicitamente.
CREATE TRIGGER trg_data_cadastro_empresa
BEFORE INSERT ON empresa
FOR EACH ROW
BEGIN
    IF NEW.data_cadastro IS NULL THEN
        SET NEW.data_cadastro = CURRENT_DATE;
    END IF;
END$$

CREATE PROCEDURE sp_verificar_limite_busca(IN p_cnpj CHAR(14), OUT p_pode_buscar BOOLEAN)
READS SQL DATA
SQL SECURITY INVOKER
BEGIN
    DECLARE v_limite INT DEFAULT NULL;
    DECLARE v_usadas BIGINT DEFAULT 0;
    SELECT p.limite_requisicao INTO v_limite
    FROM empresa e JOIN plano_de_assinatura p ON p.id_plano = e.fk_plano_id_plano
    WHERE e.cnpj = p_cnpj;
    SELECT COUNT(*) INTO v_usadas FROM empresa_busca WHERE fk_empresa_cnpj = p_cnpj;
    SET p_pode_buscar = COALESCE(v_limite = 0 OR v_usadas < v_limite, FALSE);
END$$

-- Rotina autônoma: gerencia a própria transação. Não chamar dentro de outra
-- transação; o backend usa seu serviço transacional, não esta procedure.
CREATE PROCEDURE sp_registrar_busca(
    IN p_cnpj CHAR(14), IN p_termo VARCHAR(150),
    IN p_linguagem VARCHAR(60), IN p_localizacao VARCHAR(120)
)
MODIFIES SQL DATA
SQL SECURITY INVOKER
BEGIN
    DECLARE v_id_busca BIGINT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    START TRANSACTION;
    INSERT INTO busca (filtro_localizacao, filtro_linguagem, termo_pesquisado)
    VALUES (p_localizacao, p_linguagem, p_termo);
    SET v_id_busca = LAST_INSERT_ID();
    INSERT INTO empresa_busca (fk_busca_id_busca, fk_empresa_cnpj)
    VALUES (v_id_busca, p_cnpj);
    COMMIT;
END$$

-- O chamador deve passar o CNPJ obtido da sessão autenticada. A procedure não
-- substitui autenticação nem oferece isolamento entre usuários de um cliente SQL.
CREATE PROCEDURE sp_listar_avaliacoes_empresa(IN p_cnpj CHAR(14))
READS SQL DATA
SQL SECURITY INVOKER
BEGIN
    SELECT ec.fk_empresa_cnpj AS cnpj_empresa,
           c.node_id AS github_user_id, c.username, c.nome_candidato,
           ec.comentario, ec.nota, TRUE AS privada, ec.data_avaliacao, ec.favorito
    FROM empresa_candidato ec
    JOIN candidato c ON c.node_id = ec.fk_candidato_node_id
    WHERE ec.fk_empresa_cnpj = p_cnpj
      AND (NULLIF(TRIM(ec.comentario), '') IS NOT NULL OR ec.nota IS NOT NULL)
    ORDER BY ec.data_avaliacao DESC, c.username;
END$$

CREATE FUNCTION fn_contar_favoritos(p_cnpj CHAR(14))
RETURNS BIGINT
NOT DETERMINISTIC
READS SQL DATA
SQL SECURITY INVOKER
BEGIN
    DECLARE v_total BIGINT;
    SELECT COUNT(*) INTO v_total FROM empresa_candidato
    WHERE fk_empresa_cnpj = p_cnpj AND favorito = TRUE;
    RETURN v_total;
END$$

-- Permissão para criar uma NOVA avaliação. Editar uma existente não deve
-- consumir nova unidade. Zero mantém a convenção legada de limite ilimitado.
CREATE FUNCTION fn_pode_avaliar_empresa(p_cnpj CHAR(14))
RETURNS BOOLEAN
NOT DETERMINISTIC
READS SQL DATA
SQL SECURITY INVOKER
BEGIN
    DECLARE v_limite INT DEFAULT NULL;
    DECLARE v_usadas BIGINT DEFAULT 0;
    SELECT p.limite_avaliacao INTO v_limite
    FROM empresa e JOIN plano_de_assinatura p ON p.id_plano = e.fk_plano_id_plano
    WHERE e.cnpj = p_cnpj;
    SELECT COUNT(*) INTO v_usadas FROM empresa_candidato
    WHERE fk_empresa_cnpj = p_cnpj
      AND (NULLIF(TRIM(comentario), '') IS NOT NULL OR nota IS NOT NULL);
    RETURN COALESCE(v_limite = 0 OR v_usadas < v_limite, FALSE);
END$$

DELIMITER ;
