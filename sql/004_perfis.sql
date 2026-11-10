-- Novos perfis de acesso dos usuários internos (CADUSR.USR_PERFIL): USR, DEV e ADM.
--
--   USR  o padrão. Monitoramento, Integrações e a lista de plataformas cadastradas.
--   DEV  o time de desenvolvimento. Tudo do USR, mais a matriz de funcionalidades e os workflows, só para consulta.
--   ADM  acesso completo, inclusive cadastrar e editar.
--
-- Os valores antigos viram: ADMIN -> ADM, USUARIO -> USR. Ninguém vira DEV sozinho.
--
-- ORDEM: rode este script e publique a API nova na mesma janela. A API nova só reconhece ADM, DEV e USR (qualquer outro
-- valor conta como USR, o acesso fecha), e a antiga só reconhece ADMIN. Entre uma coisa e outra, quem era admin fica
-- sem as telas de admin: não quebra nada, só incomoda.
--
-- ATENÇÃO, n8n: o workflow que decide quem exporta workflows lê o perfil em GET /usuarios/me. Se ele compara com 'ADMIN',
-- passa a negar todo mundo com o perfil novo. Ajuste lá para aceitar 'ADM' (e 'DEV', se for o caso) antes de publicar.
--
-- Rode um comando por vez e confira o banco com: SELECT DATABASE(), @@hostname;

-- 1) Converte quem já existe.
UPDATE CADUSR SET USR_PERFIL = 'ADM' WHERE USR_PERFIL = 'ADMIN';
UPDATE CADUSR SET USR_PERFIL = 'USR' WHERE USR_PERFIL = 'USUARIO';

-- 2) O default da coluna passa a ser USR: quem entra pela primeira vez com o Google nasce sem poder nenhum.
ALTER TABLE CADUSR
  MODIFY COLUMN USR_PERFIL VARCHAR(10) NOT NULL DEFAULT 'USR';

-- 3) Só aceita os três valores. Falha, de propósito, se sobrou algum valor fora deles: confira com
--    SELECT USR_EMAIL_, USR_PERFIL FROM CADUSR WHERE USR_PERFIL NOT IN ('USR','DEV','ADM');
--    (o CHECK só é aplicado no MySQL 8.0.16 ou mais novo; antes disso o comando passa e a regra vale só na API.)
ALTER TABLE CADUSR
  ADD CONSTRAINT CK_CADUSR_PERFIL CHECK (USR_PERFIL IN ('USR', 'DEV', 'ADM'));

-- 4) Mudar o perfil de alguém. Vale na próxima requisição; o token já emitido só reflete na tela no próximo login,
--    mas a API passa a barrar (ou liberar) na hora. O usuário só existe no CADUSR depois do primeiro login com o Google.
-- UPDATE CADUSR SET USR_PERFIL = 'DEV' WHERE USR_EMAIL_ IN ('pessoa@citelsoftware.com.br');
-- UPDATE CADUSR SET USR_PERFIL = 'ADM' WHERE USR_EMAIL_ IN ('pessoa@citelsoftware.com.br');
-- UPDATE CADUSR SET USR_PERFIL = 'USR' WHERE USR_EMAIL_ IN ('pessoa@citelsoftware.com.br');

-- 5) Conferir.
-- SELECT USR_EMAIL_, USR_PERFIL FROM CADUSR ORDER BY USR_PERFIL, USR_EMAIL_;

-- Desfazer (volta aos nomes antigos; só se a API nova ainda não estiver no ar):
-- ALTER TABLE CADUSR DROP CONSTRAINT CK_CADUSR_PERFIL;
-- UPDATE CADUSR SET USR_PERFIL = 'ADMIN' WHERE USR_PERFIL = 'ADM';
-- UPDATE CADUSR SET USR_PERFIL = 'USUARIO' WHERE USR_PERFIL IN ('USR', 'DEV');
-- ALTER TABLE CADUSR MODIFY COLUMN USR_PERFIL VARCHAR(10) NOT NULL DEFAULT 'USUARIO';
