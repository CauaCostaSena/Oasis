-- Oásis: instalação nova, sem exclusão de tabelas ou dados.
-- NÃO é migração. Consulte docs/BANCO-E-MIGRACOES.md para bancos existentes.
-- Gerado pela concatenação de 01 + 02 + 03; manter sincronizado.

-- Oásis — modelo físico para uma instalação nova (MySQL 8.0.16+).
-- Não apaga dados. CREATE TABLE IF NOT EXISTS não migra tabelas existentes:
-- consulte docs/BANCO-E-MIGRACOES.md antes de atualizar um banco já utilizado.
-- O schema ckgd e as colunas node_id são identificadores técnicos legados.
CREATE DATABASE IF NOT EXISTS ckgd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ckgd;

CREATE TABLE IF NOT EXISTS plano_de_assinatura (
    id_plano INT AUTO_INCREMENT PRIMARY KEY,
    nome_plano VARCHAR(60) NOT NULL,
    preco_plano DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    periodicidade ENUM('MENSAL', 'ANUAL') NOT NULL DEFAULT 'MENSAL',
    limite_requisicao INT NOT NULL DEFAULT 0,
    limite_avaliacao INT NOT NULL DEFAULT 0,
    limite_comparacao INT NOT NULL DEFAULT 0,
    status_plano ENUM('ATIVO', 'INATIVO') NOT NULL DEFAULT 'ATIVO',
    data_ativacao DATE NULL,
    data_expiracao DATE NULL
) ENGINE=InnoDB;

-- Somente empresas possuem conta e senha (hash BCrypt).
CREATE TABLE IF NOT EXISTS empresa (
    cnpj CHAR(14) PRIMARY KEY,
    nome_empresa VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    pais VARCHAR(60) NULL,
    estado VARCHAR(60) NULL,
    cidade VARCHAR(60) NULL,
    bairro VARCHAR(60) NULL,
    endereco VARCHAR(150) NULL,
    telefone VARCHAR(20) NULL,
    foto_url VARCHAR(255) NULL,
    data_cadastro DATE NOT NULL DEFAULT (CURRENT_DATE),
    fk_plano_id_plano INT NOT NULL,
    CONSTRAINT fk_empresa_plano FOREIGN KEY (fk_plano_id_plano)
        REFERENCES plano_de_assinatura(id_plano) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Dados públicos do GitHub. Não há campos de autenticação de candidato.
-- node_id armazena o id NUMÉRICO da REST API, não o node_id textual do GitHub.
-- O nome físico é preservado para evitar uma migração destrutiva de PKs/FKs.
CREATE TABLE IF NOT EXISTS candidato (
    node_id BIGINT PRIMARY KEY,
    nome_candidato VARCHAR(150) NULL,
    username VARCHAR(60) NOT NULL UNIQUE,
    localizacao VARCHAR(120) NULL,
    num_repositorios INT NOT NULL DEFAULT 0,
    bio TEXT NULL,
    avatar_url VARCHAR(255) NULL,
    linguagem_principal VARCHAR(60) NULL,
    data_ultima_sincronizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_candidato_localizacao (localizacao),
    INDEX idx_candidato_linguagem (linguagem_principal)
) ENGINE=InnoDB;

-- A URL continua como PK nesta fase. Um futuro github_repository_id exige
-- preenchimento e validação dos dados existentes antes de trocar a chave.
CREATE TABLE IF NOT EXISTS repositorio (
    url_repositorio VARCHAR(255) PRIMARY KEY,
    nome_repositorio VARCHAR(150) NOT NULL,
    descricao TEXT NULL,
    ultimo_commit DATETIME NULL,
    linguagem_principal VARCHAR(60) NULL,
    branch_padrao VARCHAR(60) NULL DEFAULT 'main',
    numero_issue INT NOT NULL DEFAULT 0,
    numero_fork INT NOT NULL DEFAULT 0,
    numero_estrela INT NOT NULL DEFAULT 0,
    fk_candidato_node_id BIGINT NOT NULL,
    CONSTRAINT fk_repositorio_candidato FOREIGN KEY (fk_candidato_node_id)
        REFERENCES candidato(node_id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_repositorio_linguagem (linguagem_principal)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS busca (
    id_busca BIGINT AUTO_INCREMENT PRIMARY KEY,
    filtro_localizacao VARCHAR(120) NULL,
    filtro_linguagem VARCHAR(60) NULL,
    termo_pesquisado VARCHAR(150) NULL,
    data_busca DATE NOT NULL DEFAULT (CURRENT_DATE),
    hora_busca TIME NOT NULL DEFAULT (CURRENT_TIME),
    INDEX idx_busca_termo (termo_pesquisado)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS empresa_busca (
    fk_busca_id_busca BIGINT NOT NULL,
    fk_empresa_cnpj CHAR(14) NOT NULL,
    PRIMARY KEY (fk_busca_id_busca, fk_empresa_cnpj),
    CONSTRAINT fk_empresabusca_busca FOREIGN KEY (fk_busca_id_busca)
        REFERENCES busca(id_busca) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_empresabusca_empresa FOREIGN KEY (fk_empresa_cnpj)
        REFERENCES empresa(cnpj) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Favorito e avaliação são conceitos separados no mesmo vínculo.
-- Nota opcional inteira de 1 a 5, confirmada pelo responsável em 27/09/2026.
-- A coluna privada permanece por compatibilidade; nenhuma avaliação é pública.
CREATE TABLE IF NOT EXISTS empresa_candidato (
    fk_empresa_cnpj CHAR(14) NOT NULL,
    fk_candidato_node_id BIGINT NOT NULL,
    favorito BOOLEAN NOT NULL DEFAULT FALSE,
    comentario TEXT NULL,
    nota DECIMAL(4,2) NULL,
    privada BOOLEAN NOT NULL DEFAULT TRUE,
    data_avaliacao DATETIME NULL,
    PRIMARY KEY (fk_empresa_cnpj, fk_candidato_node_id),
    CONSTRAINT chk_empresa_candidato_nota CHECK (nota IS NULL OR (nota >= 1 AND nota <= 5 AND nota = FLOOR(nota))),
    CONSTRAINT chk_empresa_candidato_privada CHECK (privada = TRUE),
    CONSTRAINT fk_empresacandidato_empresa FOREIGN KEY (fk_empresa_cnpj)
        REFERENCES empresa(cnpj) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_empresacandidato_candidato FOREIGN KEY (fk_candidato_node_id)
        REFERENCES candidato(node_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solicitacao_suporte (
    id_solicitacao BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_solicitante VARCHAR(20) NOT NULL,
    nome_solicitante VARCHAR(150) NOT NULL,
    email_solicitante VARCHAR(150) NOT NULL,
    assunto VARCHAR(150) NOT NULL,
    mensagem TEXT NOT NULL,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_suporte_empresa CHECK (tipo_solicitante = 'EMPRESA')
) ENGINE=InnoDB;


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
