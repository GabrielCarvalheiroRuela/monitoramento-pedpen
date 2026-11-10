-- Controle de funcionalidades das plataformas de integração. Tudo é cadastrado pelo portal, à mão.
--
-- Req. Altera 474225.
--
-- O hibernate.ddl-auto é "none": a API NÃO cria estas tabelas. Rode este script no banco ANTES de publicar
-- a versão da API que as lê. A API antiga não toca nelas, então publicar fora de ordem não quebra nada.
-- Rode um comando por vez e confira o banco com: SELECT DATABASE(), @@hostname;
--
-- Padrão de nomes: toda coluna é FUN_XXXXXX (prefixo FUN de funcionalidades, underline, 6 caracteres), nas tabelas e
-- nas views. As tabelas seguem o CADINT/CADPLA/LOGINT: 6 caracteres. O AUTOINCREM é o de sempre.
-- Como o prefixo é o mesmo nas três tabelas, o mesmo nome aparece em mais de uma (FUN_MODULO, FUN_NOMFUN, FUN_SISEXT,
-- FUN_USRALT, FUN_DHUALT): num SELECT com JOIN, qualifique sempre com o alias da tabela.
--
-- Modelo:
--   CADFUN  lista única (modelo) de funcionalidades. Vale para todas as plataformas.
--   PLAFUN  situação de cada funcionalidade em cada plataforma. Sem linha = NAO_IMPLEMENTADO.
--   LOGFUN  histórico das alterações (quem, quando, de quê para quê), no mesmo formato do LOGINT.
--   VW_STAFUN / VW_COBFUN / VW_COBPLA  leitura: situação efetiva e cobertura. É só por elas que o MCP lê.

-- 1) A lista de funcionalidades. FUN_NOMFUN é o nome curto; FUN_DESFUN explica a regra com calma.
--    A chave (módulo + nome) impede cadastrar a mesma funcionalidade duas vezes no mesmo módulo.
CREATE TABLE IF NOT EXISTS CADFUN (
    AUTOINCREM  BIGINT AUTO_INCREMENT PRIMARY KEY,
    FUN_MODULO  VARCHAR(80)  NOT NULL,                  -- módulo (ex.: Autenticação e Infra, Produtos - Cadastro)
    FUN_NOMFUN  VARCHAR(120) NOT NULL,                  -- nome curto
    FUN_DESFUN  TEXT NULL,                              -- descrição detalhada: o que faz e qual a regra
    FUN_ORDFUN  INT NOT NULL DEFAULT 0,                 -- posição na lista inteira; a ordem dos módulos é a de quem aparece primeiro
    FUN_USRALT  VARCHAR(255) NULL,                      -- quem fez a última alteração (preenchido pela API)
    FUN_DHUALT  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY UQ_CADFUN_NOME (FUN_MODULO, FUN_NOMFUN)
) ENGINE=INNODB;

-- 2) A situação por plataforma. FUN_SISEXT é o PLA_SISEXT do CADPLA (ex.: tray).
--    Excluir a funcionalidade apaga estas linhas junto (ON DELETE CASCADE), como pede o RF04.
CREATE TABLE IF NOT EXISTS PLAFUN (
    AUTOINCREM  BIGINT AUTO_INCREMENT PRIMARY KEY,
    FUN_CODFUN  BIGINT NOT NULL,                        -- CADFUN.AUTOINCREM
    FUN_SISEXT  VARCHAR(50) NOT NULL,                   -- CADPLA.PLA_SISEXT
    FUN_STATUS  ENUM('IMPLEMENTADO','PARCIAL','NAO_IMPLEMENTADO','NAO_SE_APLICA')
                NOT NULL DEFAULT 'NAO_IMPLEMENTADO',
    -- Implementado não quer dizer validado: o resultado do teste é um controle à parte.
    FUN_RESTES  ENUM('NAO_TESTADO','TESTADO','COM_FALHA') NOT NULL DEFAULT 'NAO_TESTADO',
    FUN_DHTEST  DATETIME NULL,                          -- quando foi testado pela última vez
    FUN_USRTST  VARCHAR(255) NULL,                      -- quem testou
    FUN_OBSERV  TEXT NULL,                              -- observação / regra desta plataforma
    FUN_CFGERP  TEXT NULL,                              -- parâmetros do ERP envolvidos (ex.: CFG_CONKEY, CFG_SECRET)
    FUN_WORKFL  TEXT NULL,                              -- workflow(s) do n8n que implementam (pode ter vários nomes)
    FUN_RESPON  VARCHAR(255) NULL,                      -- responsável pelo desenvolvimento
    FUN_PRIORI  ENUM('ALTA','MEDIA','BAIXA') NULL,      -- prioridade do que ainda falta fazer
    FUN_DTPREV  DATE NULL,                              -- previsão de entrega
    FUN_REQALT  VARCHAR(20) NULL,                       -- nº do requisito no Altera que trata desta funcionalidade
    FUN_USRALT  VARCHAR(255) NULL,                      -- quem fez a última alteração (preenchido pela API)
    FUN_DHUALT  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY UQ_PLAFUN (FUN_CODFUN, FUN_SISEXT),
    KEY IX_PLAFUN_SIS (FUN_SISEXT, FUN_STATUS),
    CONSTRAINT FK_PLAFUN_CADFUN FOREIGN KEY (FUN_CODFUN) REFERENCES CADFUN (AUTOINCREM) ON DELETE CASCADE
) ENGINE=INNODB;

-- 3) Liga FUN_SISEXT ao CADPLA, para não existir situação de plataforma que não está cadastrada.
--    Fica em comando separado porque a chave estrangeira exige o mesmo charset/collation do CADPLA.
--    Se der erro 1215/3780, compare com SHOW CREATE TABLE CADPLA e ajuste; as tabelas já funcionam sem ela.
ALTER TABLE PLAFUN
  ADD CONSTRAINT FK_PLAFUN_CADPLA FOREIGN KEY (FUN_SISEXT) REFERENCES CADPLA (PLA_SISEXT);

-- 4) Histórico. Uma linha por campo alterado. Guarda o nome da funcionalidade (e não só o id)
--    para continuar legível depois que ela for excluída, então não tem chave estrangeira.
CREATE TABLE IF NOT EXISTS LOGFUN (
    AUTOINCREM  BIGINT AUTO_INCREMENT PRIMARY KEY,
    FUN_DHUALT  DATETIME DEFAULT CURRENT_TIMESTAMP,     -- data/hora da alteração, preenchida pelo banco
    FUN_TIPACA  ENUM('INCLUSAO','ALTERACAO','EXCLUSAO') NOT NULL,   -- tipo da ação
    FUN_MODULO  VARCHAR(80)  NULL,
    FUN_NOMFUN  VARCHAR(120) NULL,                      -- nome da funcionalidade no momento da alteração
    FUN_SISEXT  VARCHAR(50)  NULL,                      -- plataforma; vazio quando a mudança é na lista (CADFUN)
    FUN_CMPALT  VARCHAR(30)  NULL,                      -- coluna alterada (ex.: FUN_STATUS); vazio em inclusão e exclusão
    FUN_VALANT  TEXT NULL,                              -- valor anterior
    FUN_VALATU  TEXT NULL,                              -- valor atual
    FUN_NOMUSU  VARCHAR(255) NULL,                      -- usuário (CADUSR) que alterou
    KEY IX_LOGFUN_DH (FUN_DHUALT)
) ENGINE=INNODB;

-- 5) Situação efetiva: uma linha por funcionalidade x plataforma, já com "sem registro = NAO_IMPLEMENTADO".
--    Entram todas as plataformas do CADPLA: cadastrou a plataforma e a funcionalidade, já dá para marcar a situação.
--    As colunas têm o mesmo nome das da tabela. As duas que vêm de fora: FUN_DESPLA (nome da plataforma, do CADPLA)
--    e FUN_CODFUN (o AUTOINCREM do CADFUN, igual ao FUN_CODFUN do PLAFUN).
CREATE OR REPLACE VIEW VW_STAFUN AS
SELECT p.PLA_SISEXT                                  AS FUN_SISEXT,
       p.PLA_DESCRI                                  AS FUN_DESPLA,
       f.AUTOINCREM                                  AS FUN_CODFUN,
       f.FUN_MODULO                                  AS FUN_MODULO,
       f.FUN_NOMFUN                                  AS FUN_NOMFUN,
       f.FUN_DESFUN                                  AS FUN_DESFUN,
       f.FUN_ORDFUN                                  AS FUN_ORDFUN,
       COALESCE(x.FUN_STATUS, 'NAO_IMPLEMENTADO')    AS FUN_STATUS,
       COALESCE(x.FUN_RESTES, 'NAO_TESTADO')         AS FUN_RESTES,
       x.FUN_DHTEST                                  AS FUN_DHTEST,
       x.FUN_USRTST                                  AS FUN_USRTST,
       x.FUN_OBSERV                                  AS FUN_OBSERV,
       x.FUN_CFGERP                                  AS FUN_CFGERP,
       x.FUN_WORKFL                                  AS FUN_WORKFL,
       x.FUN_RESPON                                  AS FUN_RESPON,
       x.FUN_PRIORI                                  AS FUN_PRIORI,
       x.FUN_DTPREV                                  AS FUN_DTPREV,
       x.FUN_REQALT                                  AS FUN_REQALT,
       x.FUN_USRALT                                  AS FUN_USRALT,
       x.FUN_DHUALT                                  AS FUN_DHUALT
  FROM CADPLA p
 CROSS JOIN CADFUN f
  LEFT JOIN PLAFUN x ON x.FUN_CODFUN = f.AUTOINCREM AND x.FUN_SISEXT = p.PLA_SISEXT;

-- 6) Cobertura por plataforma e módulo:
--      (IMPLEMENTADO + 0,5 x PARCIAL) / (total - NAO_SE_APLICA), em %.
--    FUN_PERCOB vem NULL quando nada se aplica (a tela mostra "-"). O peso 0,5 aparece aqui e em VW_COBPLA;
--    para mudar, recrie as duas views.
--    Contagens: QTDTOT total, QTDIMP implementadas, QTDPAR parciais, QTDNAO não implementadas, QTDNSA não se aplicam,
--    QTDTST implementadas ou parciais já testadas.
CREATE OR REPLACE VIEW VW_COBFUN AS
SELECT FUN_SISEXT,
       FUN_DESPLA,
       FUN_MODULO,
       MIN(FUN_ORDFUN)                                   AS FUN_ORDFUN,   -- dá a ordem de exibição dos módulos
       COUNT(*)                                          AS FUN_QTDTOT,
       SUM(FUN_STATUS = 'IMPLEMENTADO')                  AS FUN_QTDIMP,
       SUM(FUN_STATUS = 'PARCIAL')                       AS FUN_QTDPAR,
       SUM(FUN_STATUS = 'NAO_IMPLEMENTADO')              AS FUN_QTDNAO,
       SUM(FUN_STATUS = 'NAO_SE_APLICA')                 AS FUN_QTDNSA,
       SUM(FUN_STATUS IN ('IMPLEMENTADO','PARCIAL') AND FUN_RESTES = 'TESTADO') AS FUN_QTDTST,
       ROUND(100 * (SUM(FUN_STATUS = 'IMPLEMENTADO') + 0.5 * SUM(FUN_STATUS = 'PARCIAL'))
             / NULLIF(COUNT(*) - SUM(FUN_STATUS = 'NAO_SE_APLICA'), 0), 1) AS FUN_PERCOB
  FROM VW_STAFUN
 GROUP BY FUN_SISEXT, FUN_DESPLA, FUN_MODULO;

-- 7) Cobertura da plataforma inteira (não é a média dos módulos: soma tudo e faz a conta uma vez, como o Resumo).
CREATE OR REPLACE VIEW VW_COBPLA AS
SELECT FUN_SISEXT,
       FUN_DESPLA,
       COUNT(*)                                          AS FUN_QTDTOT,
       SUM(FUN_STATUS = 'IMPLEMENTADO')                  AS FUN_QTDIMP,
       SUM(FUN_STATUS = 'PARCIAL')                       AS FUN_QTDPAR,
       SUM(FUN_STATUS = 'NAO_IMPLEMENTADO')              AS FUN_QTDNAO,
       SUM(FUN_STATUS = 'NAO_SE_APLICA')                 AS FUN_QTDNSA,
       SUM(FUN_STATUS IN ('IMPLEMENTADO','PARCIAL') AND FUN_RESTES = 'TESTADO') AS FUN_QTDTST,
       ROUND(100 * (SUM(FUN_STATUS = 'IMPLEMENTADO') + 0.5 * SUM(FUN_STATUS = 'PARCIAL'))
             / NULLIF(COUNT(*) - SUM(FUN_STATUS = 'NAO_SE_APLICA'), 0), 1) AS FUN_PERCOB
  FROM VW_STAFUN
 GROUP BY FUN_SISEXT, FUN_DESPLA;

-- 8) Usuário do MCP: só lê, e só as views. Não enxerga CADUSR, CADINT nem as tabelas acima.
--    Troque NOME_DO_BANCO (confira com SELECT DATABASE()) e a senha. Troque '%' pelo IP de onde o servidor do
--    Docker acessa o banco, se der. As views rodam com os direitos de quem as criou (SQL SECURITY DEFINER),
--    então esse usuário não precisa de permissão nenhuma nas tabelas.
-- CREATE USER IF NOT EXISTS 'mcp_func'@'%' IDENTIFIED BY 'TROQUE_ESTA_SENHA';
-- GRANT SELECT ON NOME_DO_BANCO.VW_STAFUN TO 'mcp_func'@'%';
-- GRANT SELECT ON NOME_DO_BANCO.VW_COBFUN TO 'mcp_func'@'%';
-- GRANT SELECT ON NOME_DO_BANCO.VW_COBPLA TO 'mcp_func'@'%';

-- 9) Conferir.
-- SHOW GRANTS FOR 'mcp_func'@'%';
-- SELECT * FROM VW_COBPLA;
-- SELECT * FROM VW_STAFUN WHERE FUN_SISEXT = 'tray' AND FUN_STATUS = 'PARCIAL';

-- Desfazer tudo (só se a API nova ainda não estiver no ar; apaga os dados):
-- DROP USER IF EXISTS 'mcp_func'@'%';
-- DROP VIEW IF EXISTS VW_COBPLA;
-- DROP VIEW IF EXISTS VW_COBFUN;
-- DROP VIEW IF EXISTS VW_STAFUN;
-- DROP TABLE IF EXISTS LOGFUN;
-- DROP TABLE IF EXISTS PLAFUN;
-- DROP TABLE IF EXISTS CADFUN;
