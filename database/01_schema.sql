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
