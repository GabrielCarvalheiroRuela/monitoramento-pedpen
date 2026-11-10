package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.CoberturaModuloDTO;
import com.citel.monitoramento_n8n.DTO.CoberturaPlataformaDTO;
import com.citel.monitoramento_n8n.DTO.DadosCriacaoFuncionalidade;
import com.citel.monitoramento_n8n.DTO.DadosSituacaoFuncionalidade;
import com.citel.monitoramento_n8n.DTO.EdicaoFuncionalidadeDTO;
import com.citel.monitoramento_n8n.DTO.FuncionalidadeDTO;
import com.citel.monitoramento_n8n.DTO.SituacaoFuncionalidadeDTO;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.AcaoFuncionalidade;
import com.citel.monitoramento_n8n.model.CoberturaModulo;
import com.citel.monitoramento_n8n.model.Funcionalidade;
import com.citel.monitoramento_n8n.model.LogFuncionalidade;
import com.citel.monitoramento_n8n.model.Plataforma;
import com.citel.monitoramento_n8n.model.SituacaoFuncionalidade;
import com.citel.monitoramento_n8n.model.StatusFuncionalidade;
import com.citel.monitoramento_n8n.model.StatusTeste;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.CoberturaModuloRepository;
import com.citel.monitoramento_n8n.repository.CoberturaPlataformaRepository;
import com.citel.monitoramento_n8n.repository.FuncionalidadeRepository;
import com.citel.monitoramento_n8n.repository.LogFuncionalidadeRepository;
import com.citel.monitoramento_n8n.repository.PlataformaRepository;
import com.citel.monitoramento_n8n.repository.SituacaoEfetivaRepository;
import com.citel.monitoramento_n8n.repository.SituacaoFuncionalidadeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Matriz de funcionalidades das integrações (Req. Altera 474225): a lista única (CADFUN), a situação em cada
 * plataforma (PLAFUN) e a cobertura. A leitura sai das views VW_*, as mesmas do servidor MCP, para o portal e o
 * Claude verem os mesmos números; a gravação mexe só nas tabelas.
 *
 * Quem pode gravar (só ADMIN) é barrado em SecurityConfigurations, não aqui. Toda gravação e o seu histórico (LOGFUN)
 * andam na mesma transação, então nunca há alteração sem registro, nem registro de alteração que não aconteceu.
 */
@Slf4j
@Service
public class FuncionalidadeService {

    /** Intervalo entre as ordens, para dar para encaixar uma funcionalidade no meio sem renumerar a lista. */
    private static final int PASSO_ORDEM = 10;

    private final FuncionalidadeRepository funcionalidadeRepository;
    private final SituacaoFuncionalidadeRepository situacaoRepository;
    private final SituacaoEfetivaRepository situacaoEfetivaRepository;
    private final CoberturaPlataformaRepository coberturaPlataformaRepository;
    private final CoberturaModuloRepository coberturaModuloRepository;
    private final PlataformaRepository plataformaRepository;
    private final LogFuncionalidadeRepository logRepository;

    public FuncionalidadeService(FuncionalidadeRepository funcionalidadeRepository,
                                 SituacaoFuncionalidadeRepository situacaoRepository,
                                 SituacaoEfetivaRepository situacaoEfetivaRepository,
                                 CoberturaPlataformaRepository coberturaPlataformaRepository,
                                 CoberturaModuloRepository coberturaModuloRepository,
                                 PlataformaRepository plataformaRepository,
                                 LogFuncionalidadeRepository logRepository) {
        this.funcionalidadeRepository = funcionalidadeRepository;
        this.situacaoRepository = situacaoRepository;
        this.situacaoEfetivaRepository = situacaoEfetivaRepository;
        this.coberturaPlataformaRepository = coberturaPlataformaRepository;
        this.coberturaModuloRepository = coberturaModuloRepository;
        this.plataformaRepository = plataformaRepository;
        this.logRepository = logRepository;
    }

    // ---- leitura ----

    /** Funcionalidade x plataforma, com "sem registro = NAO_IMPLEMENTADO". Todos os filtros são opcionais e se somam. */
    @Transactional(readOnly = true)
    public List<SituacaoFuncionalidadeDTO> listar(String plataforma, String modulo, StatusFuncionalidade status,
                                                  StatusTeste teste, String responsavel, String busca) {
        return situacaoEfetivaRepository
                .buscarPorFiltro(texto(plataforma), texto(modulo), status, teste, padraoLike(responsavel), padraoLike(busca))
                .stream()
                .map(SituacaoFuncionalidadeDTO::de)
                .toList();
    }

    /** Cobertura de cada plataforma cadastrada, com o detalhe por módulo. */
    @Transactional(readOnly = true)
    public List<CoberturaPlataformaDTO> cobertura() {
        Map<String, List<CoberturaModuloDTO>> modulosPorPlataforma = coberturaModuloRepository
                .findAllByOrderByPlataformaAscOrdemAsc().stream()
                .collect(Collectors.groupingBy(CoberturaModulo::getPlataforma, LinkedHashMap::new,
                        Collectors.mapping(CoberturaModuloDTO::de, Collectors.toList())));

        return coberturaPlataformaRepository.findAllByOrderByPlataformaNomeAsc().stream()
                .map(p -> CoberturaPlataformaDTO.de(p, modulosPorPlataforma.getOrDefault(p.getPlataforma(), List.of())))
                .toList();
    }

    // ---- a lista de funcionalidades ----

    @Transactional
    public FuncionalidadeDTO criar(DadosCriacaoFuncionalidade dados, Usuario usuario) {
        String modulo = dados.modulo().trim();
        String nome = dados.nome().trim();
        if (funcionalidadeRepository.existsByModuloAndNome(modulo, nome)) {
            throw new ConflictException("Já existe a funcionalidade '" + nome + "' no módulo '" + modulo + "'");
        }

        Funcionalidade funcionalidade = new Funcionalidade();
        funcionalidade.setModulo(modulo);
        funcionalidade.setNome(nome);
        funcionalidade.setDescricao(texto(dados.descricao()));
        funcionalidade.setOrdem(dados.ordem() != null ? dados.ordem() : funcionalidadeRepository.maiorOrdem() + PASSO_ORDEM);
        funcionalidade.setUsuarioAlteracao(nomeDe(usuario));

        Funcionalidade salva = funcionalidadeRepository.save(funcionalidade);

        Historico historico = new Historico(salva, null, nomeDe(usuario));
        historico.registrar(AcaoFuncionalidade.INCLUSAO, null, null, null);
        logRepository.saveAll(historico.linhas());

        log.info("Funcionalidade criada - id: {}, módulo: {}, nome: {}, por: {}", salva.getId(), modulo, nome, nomeDe(usuario));
        return FuncionalidadeDTO.de(salva);
    }

    /** Campo ausente ou nulo preserva o valor atual. Só vira linha no LOGFUN o que realmente mudou. */
    @Transactional
    public FuncionalidadeDTO editar(Long id, EdicaoFuncionalidadeDTO dados, Usuario usuario) {
        Funcionalidade funcionalidade = buscarOuFalhar(id);

        String moduloAnterior = funcionalidade.getModulo();
        String nomeAnterior = funcionalidade.getNome();
        String descricaoAnterior = funcionalidade.getDescricao();
        int ordemAnterior = funcionalidade.getOrdem();

        String modulo = dados.modulo() != null ? dados.modulo().trim() : moduloAnterior;
        String nome = dados.nome() != null ? dados.nome().trim() : nomeAnterior;
        String descricao = dados.descricao() != null ? texto(dados.descricao()) : descricaoAnterior;
        int ordem = dados.ordem() != null ? dados.ordem() : ordemAnterior;

        boolean mudouChave = !modulo.equals(moduloAnterior) || !nome.equals(nomeAnterior);
        if (mudouChave && funcionalidadeRepository.existsByModuloAndNomeAndIdNot(modulo, nome, id)) {
            throw new ConflictException("Já existe a funcionalidade '" + nome + "' no módulo '" + modulo + "'");
        }

        funcionalidade.setModulo(modulo);
        funcionalidade.setNome(nome);
        funcionalidade.setDescricao(descricao);
        funcionalidade.setOrdem(ordem);

        // O histórico já sai com o nome novo; o valor antigo de cada campo vai na própria linha.
        Historico historico = new Historico(funcionalidade, null, nomeDe(usuario));
        historico.campo("FUN_MODULO", moduloAnterior, modulo);
        historico.campo("FUN_NOMFUN", nomeAnterior, nome);
        historico.campo("FUN_DESFUN", descricaoAnterior, descricao);
        historico.campo("FUN_ORDFUN", ordemAnterior, ordem);

        if (historico.vazio()) {
            log.info("Edição de funcionalidade sem alterações - id: {}", id);
            return FuncionalidadeDTO.de(funcionalidade);
        }

        funcionalidade.setUsuarioAlteracao(nomeDe(usuario));
        Funcionalidade salva = funcionalidadeRepository.save(funcionalidade);
        logRepository.saveAll(historico.linhas());

        log.info("Funcionalidade editada - id: {}, campos: {}, por: {}", id, historico.campos(), nomeDe(usuario));
        return FuncionalidadeDTO.de(salva);
    }

    /** Apaga a funcionalidade e, pelo ON DELETE CASCADE do banco, a situação dela em todas as plataformas. */
    @Transactional
    public void excluir(Long id, Usuario usuario) {
        Funcionalidade funcionalidade = buscarOuFalhar(id);

        Historico historico = new Historico(funcionalidade, null, nomeDe(usuario));
        historico.registrar(AcaoFuncionalidade.EXCLUSAO, null, null, null);

        funcionalidadeRepository.delete(funcionalidade);
        logRepository.saveAll(historico.linhas());

        log.info("Funcionalidade excluída - id: {}, módulo: {}, nome: {}, por: {}",
                id, funcionalidade.getModulo(), funcionalidade.getNome(), nomeDe(usuario));
    }

    // ---- a situação em cada plataforma ----

    /**
     * Substitui a situação da funcionalidade na plataforma (cria a linha se ainda não existe). Sem linha, a
     * funcionalidade conta como NAO_IMPLEMENTADO na plataforma.
     *
     * O teste é carimbado pelo servidor: ao mudar para TESTADO ou COM_FALHA grava a data e quem testou; ao voltar
     * para NAO_TESTADO, limpa os dois. Reenviar o mesmo valor não renova o carimbo.
     */
    @Transactional
    public SituacaoFuncionalidadeDTO definirSituacao(Long id, String plataforma, DadosSituacaoFuncionalidade dados,
                                                     Usuario usuario) {
        Funcionalidade funcionalidade = buscarOuFalhar(id);
        Plataforma plataformaCadastrada = plataformaRepository.findById(plataforma.trim())
                .orElseThrow(() -> new NotFoundException("Plataforma não cadastrada: " + plataforma));
        String sistemaExterno = plataformaCadastrada.getSistemaExterno();

        SituacaoFuncionalidade situacao = situacaoRepository
                .findByCodigoFuncionalidadeAndPlataforma(id, sistemaExterno).orElse(null);
        boolean nova = situacao == null;
        if (nova) {
            situacao = new SituacaoFuncionalidade();
            situacao.setCodigoFuncionalidade(id);
            situacao.setPlataforma(sistemaExterno);
        }

        String quem = nomeDe(usuario);
        Historico historico = new Historico(funcionalidade, sistemaExterno, quem);
        if (nova) {
            historico.registrar(AcaoFuncionalidade.INCLUSAO, null, null, null);
        }

        // A linha nova nasce com os padrões (NAO_IMPLEMENTADO, NAO_TESTADO, resto vazio): só o que difere vira log.
        historico.campo("FUN_STATUS", situacao.getStatus(), dados.status());
        situacao.setStatus(dados.status());

        StatusTeste teste = dados.teste() != null ? dados.teste() : StatusTeste.NAO_TESTADO;
        if (historico.campo("FUN_RESTES", situacao.getTeste(), teste)) {
            boolean testado = teste != StatusTeste.NAO_TESTADO;
            situacao.setTeste(teste);
            situacao.setDataTeste(testado ? LocalDateTime.now() : null);
            situacao.setUsuarioTeste(testado ? quem : null);
        }

        String observacao = texto(dados.observacao());
        historico.campo("FUN_OBSERV", situacao.getObservacao(), observacao);
        situacao.setObservacao(observacao);

        String configuracaoErp = texto(dados.configuracaoErp());
        historico.campo("FUN_CFGERP", situacao.getConfiguracaoErp(), configuracaoErp);
        situacao.setConfiguracaoErp(configuracaoErp);

        String workflow = texto(dados.workflow());
        historico.campo("FUN_WORKFL", situacao.getWorkflow(), workflow);
        situacao.setWorkflow(workflow);

        String responsavel = texto(dados.responsavel());
        historico.campo("FUN_RESPON", situacao.getResponsavel(), responsavel);
        situacao.setResponsavel(responsavel);

        historico.campo("FUN_PRIORI", situacao.getPrioridade(), dados.prioridade());
        situacao.setPrioridade(dados.prioridade());

        historico.campo("FUN_DTPREV", situacao.getPrevisao(), dados.previsao());
        situacao.setPrevisao(dados.previsao());

        String requisito = texto(dados.requisitoAltera());
        historico.campo("FUN_REQALT", situacao.getRequisitoAltera(), requisito);
        situacao.setRequisitoAltera(requisito);

        if (nova || !historico.vazio()) {
            situacao.setUsuarioAlteracao(quem);
            situacao = situacaoRepository.save(situacao);
            logRepository.saveAll(historico.linhas());
            log.info("Situação gravada - funcionalidade: {}, plataforma: {}, status: {}, por: {}",
                    id, sistemaExterno, dados.status(), quem);
        } else {
            log.info("Situação sem alterações - funcionalidade: {}, plataforma: {}", id, sistemaExterno);
        }

        return SituacaoFuncionalidadeDTO.de(funcionalidade, situacao, plataformaCadastrada.getDescricao());
    }

    // ---- apoio ----

    private Funcionalidade buscarOuFalhar(Long id) {
        return funcionalidadeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Funcionalidade não encontrada com o id " + id));
    }

    /** O nome do CADUSR; se o cadastro vier sem nome, o e-mail, para o histórico nunca ficar anônimo. */
    private String nomeDe(Usuario usuario) {
        return StringUtils.hasText(usuario.getNome()) ? usuario.getNome() : usuario.getEmail();
    }

    /** Sem espaços nas pontas; vazio vira nulo (campo limpo). */
    private String texto(String valor) {
        return StringUtils.hasText(valor) ? valor.strip() : null;
    }

    /** Texto digitado vira trecho de LIKE em minúsculas, com '!' de escape para % e _ não virarem curinga. */
    private String padraoLike(String valor) {
        String limpo = texto(valor);
        if (limpo == null) {
            return null;
        }
        return "%" + limpo.toLowerCase(Locale.ROOT).replaceAll("[!%_]", "!$0") + "%";
    }

    /** As linhas de LOGFUN de uma operação, com o contexto (funcionalidade, plataforma, quem) que todas repetem. */
    private static final class Historico {

        private final Funcionalidade funcionalidade;
        private final String plataforma;
        private final String usuario;
        private final List<LogFuncionalidade> linhas = new ArrayList<>();

        Historico(Funcionalidade funcionalidade, String plataforma, String usuario) {
            this.funcionalidade = funcionalidade;
            this.plataforma = plataforma;
            this.usuario = usuario;
        }

        /** Registra a alteração se o valor mudou; devolve se mudou. */
        boolean campo(String coluna, Object anterior, Object atual) {
            if (Objects.equals(anterior, atual)) {
                return false;
            }
            registrar(AcaoFuncionalidade.ALTERACAO, coluna, anterior, atual);
            return true;
        }

        void registrar(AcaoFuncionalidade acao, String coluna, Object anterior, Object atual) {
            LogFuncionalidade linha = new LogFuncionalidade();
            linha.setAcao(acao);
            linha.setModulo(funcionalidade.getModulo());
            linha.setNomeFuncionalidade(funcionalidade.getNome());
            linha.setPlataforma(plataforma);
            linha.setCampoAlterado(coluna);
            linha.setValorAnterior(anterior == null ? null : anterior.toString());
            linha.setValorAtual(atual == null ? null : atual.toString());
            linha.setNomeUsuario(usuario);
            linhas.add(linha);
        }

        boolean vazio() {
            return linhas.isEmpty();
        }

        List<LogFuncionalidade> linhas() {
            return linhas;
        }

        List<String> campos() {
            return linhas.stream().map(LogFuncionalidade::getCampoAlterado).toList();
        }
    }
}
