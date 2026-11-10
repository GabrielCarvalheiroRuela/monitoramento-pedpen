package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.DadosCriacaoFuncionalidade;
import com.citel.monitoramento_n8n.DTO.DadosSituacaoFuncionalidade;
import com.citel.monitoramento_n8n.DTO.EdicaoFuncionalidadeDTO;
import com.citel.monitoramento_n8n.DTO.FuncionalidadeDTO;
import com.citel.monitoramento_n8n.DTO.SituacaoFuncionalidadeDTO;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.AcaoFuncionalidade;
import com.citel.monitoramento_n8n.model.Funcionalidade;
import com.citel.monitoramento_n8n.model.LogFuncionalidade;
import com.citel.monitoramento_n8n.model.Plataforma;
import com.citel.monitoramento_n8n.model.Prioridade;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Matriz de funcionalidades: o que a gravação faz no CADFUN/PLAFUN e, principalmente, o que vira linha no LOGFUN.
 * A regra de ouro é a do LOGINT: só grava log do que realmente mudou, e nada quando nada mudou.
 */
@ExtendWith(MockitoExtension.class)
class FuncionalidadeServiceTest {

    @Mock
    FuncionalidadeRepository funcionalidadeRepository;
    @Mock
    SituacaoFuncionalidadeRepository situacaoRepository;
    @Mock
    SituacaoEfetivaRepository situacaoEfetivaRepository;
    @Mock
    CoberturaPlataformaRepository coberturaPlataformaRepository;
    @Mock
    CoberturaModuloRepository coberturaModuloRepository;
    @Mock
    PlataformaRepository plataformaRepository;
    @Mock
    LogFuncionalidadeRepository logRepository;

    FuncionalidadeService service;
    Funcionalidade funcionalidade;
    Usuario admin;

    @BeforeEach
    void preparar() {
        service = new FuncionalidadeService(funcionalidadeRepository, situacaoRepository, situacaoEfetivaRepository,
                coberturaPlataformaRepository, coberturaModuloRepository, plataformaRepository, logRepository);

        funcionalidade = new Funcionalidade();
        funcionalidade.setId(7L);
        funcionalidade.setModulo("Preços");
        funcionalidade.setNome("Envio de preço promocional");
        funcionalidade.setDescricao("Envia o preço de oferta.");
        funcionalidade.setOrdem(50);

        admin = new Usuario();
        admin.setEmail("gabriel@citelsoftware.com.br");
        admin.setNome("Gabriel Carvalheiro");

        // lenient: os testes de funcionalidade inexistente consultam outro id, e o Mockito estrito o trataria como erro.
        lenient().when(funcionalidadeRepository.findById(7L)).thenReturn(Optional.of(funcionalidade));
        lenient().when(funcionalidadeRepository.save(any(Funcionalidade.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(situacaoRepository.save(any(SituacaoFuncionalidade.class))).thenAnswer(i -> i.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private List<LogFuncionalidade> logsGravados() {
        ArgumentCaptor<List<LogFuncionalidade>> captor = ArgumentCaptor.forClass(List.class);
        verify(logRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private static DadosSituacaoFuncionalidade situacao(StatusFuncionalidade status, StatusTeste teste) {
        return new DadosSituacaoFuncionalidade(status, teste, null, null, null, null, null, null, null);
    }

    // ---- criar ----

    @Test
    void criaNoFimDaListaQuandoNaoInformaAOrdem() {
        when(funcionalidadeRepository.existsByModuloAndNome("Preços", "Nova")).thenReturn(false);
        when(funcionalidadeRepository.maiorOrdem()).thenReturn(1250);

        FuncionalidadeDTO criada = service.criar(new DadosCriacaoFuncionalidade("  Preços ", " Nova ", "  ", null), admin);

        assertThat(criada.modulo()).isEqualTo("Preços");
        assertThat(criada.nome()).isEqualTo("Nova");
        assertThat(criada.descricao()).isNull();
        assertThat(criada.ordem()).isEqualTo(1260);
        assertThat(criada.alteradoPor()).isEqualTo("Gabriel Carvalheiro");

        List<LogFuncionalidade> logs = logsGravados();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAcao()).isEqualTo(AcaoFuncionalidade.INCLUSAO);
        assertThat(logs.get(0).getNomeFuncionalidade()).isEqualTo("Nova");
        assertThat(logs.get(0).getNomeUsuario()).isEqualTo("Gabriel Carvalheiro");
    }

    @Test
    void respeitaAOrdemInformada() {
        FuncionalidadeDTO criada = service.criar(new DadosCriacaoFuncionalidade("Preços", "Nova", null, 55), admin);

        assertThat(criada.ordem()).isEqualTo(55);
        verify(funcionalidadeRepository, never()).maiorOrdem();
    }

    @Test
    void naoCriaDuplicadaNoMesmoModulo() {
        when(funcionalidadeRepository.existsByModuloAndNome("Preços", "Nova")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new DadosCriacaoFuncionalidade("Preços", "Nova", null, null), admin))
                .isInstanceOf(ConflictException.class);

        verify(funcionalidadeRepository, never()).save(any());
        verify(logRepository, never()).saveAll(any());
    }

    @Test
    void usaOEmailQuandoOUsuarioNaoTemNome() {
        admin.setNome(" ");

        FuncionalidadeDTO criada = service.criar(new DadosCriacaoFuncionalidade("Preços", "Nova", null, 1), admin);

        assertThat(criada.alteradoPor()).isEqualTo("gabriel@citelsoftware.com.br");
    }

    // ---- editar ----

    @Test
    void renomearGravaSoOCampoQueMudou() {
        FuncionalidadeDTO editada = service.editar(7L, new EdicaoFuncionalidadeDTO(null, " Preço promocional ", null, null), admin);

        assertThat(editada.nome()).isEqualTo("Preço promocional");
        assertThat(editada.modulo()).isEqualTo("Preços");

        List<LogFuncionalidade> logs = logsGravados();
        assertThat(logs).hasSize(1);
        LogFuncionalidade linha = logs.get(0);
        assertThat(linha.getAcao()).isEqualTo(AcaoFuncionalidade.ALTERACAO);
        assertThat(linha.getCampoAlterado()).isEqualTo("FUN_NOMFUN");
        assertThat(linha.getValorAnterior()).isEqualTo("Envio de preço promocional");
        assertThat(linha.getValorAtual()).isEqualTo("Preço promocional");
        assertThat(linha.getPlataforma()).isNull();
        assertThat(funcionalidade.getUsuarioAlteracao()).isEqualTo("Gabriel Carvalheiro");
    }

    @Test
    void reenviarOsMesmosValoresNaoGravaNada() {
        service.editar(7L, new EdicaoFuncionalidadeDTO("Preços", "Envio de preço promocional", "Envia o preço de oferta.", 50), admin);

        verify(funcionalidadeRepository, never()).save(any());
        verify(logRepository, never()).saveAll(any());
        assertThat(funcionalidade.getUsuarioAlteracao()).isNull();
    }

    @Test
    void descricaoVaziaApagaADescricao() {
        service.editar(7L, new EdicaoFuncionalidadeDTO(null, null, "   ", null), admin);

        assertThat(funcionalidade.getDescricao()).isNull();
        assertThat(logsGravados()).extracting(LogFuncionalidade::getCampoAlterado).containsExactly("FUN_DESFUN");
    }

    @Test
    void naoRenomeiaParaUmNomeQueJaExisteNoModulo() {
        when(funcionalidadeRepository.existsByModuloAndNomeAndIdNot("Preços", "Outra", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.editar(7L, new EdicaoFuncionalidadeDTO(null, "Outra", null, null), admin))
                .isInstanceOf(ConflictException.class);

        assertThat(funcionalidade.getNome()).isEqualTo("Envio de preço promocional");
        verify(logRepository, never()).saveAll(any());
    }

    @Test
    void editarInexistenteDa404() {
        assertThatThrownBy(() -> service.editar(999L, new EdicaoFuncionalidadeDTO(null, "x", null, null), admin))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- excluir ----

    @Test
    void excluirApagaEGuardaONomeNoHistorico() {
        service.excluir(7L, admin);

        verify(funcionalidadeRepository).delete(funcionalidade);
        List<LogFuncionalidade> logs = logsGravados();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAcao()).isEqualTo(AcaoFuncionalidade.EXCLUSAO);
        assertThat(logs.get(0).getModulo()).isEqualTo("Preços");
        assertThat(logs.get(0).getNomeFuncionalidade()).isEqualTo("Envio de preço promocional");
    }

    @Test
    void excluirInexistenteDa404() {
        assertThatThrownBy(() -> service.excluir(999L, admin)).isInstanceOf(NotFoundException.class);

        verify(funcionalidadeRepository, never()).delete(any());
        verify(logRepository, never()).saveAll(any());
    }

    // ---- situação por plataforma ----

    private void plataformaTrayCadastrada() {
        when(plataformaRepository.findById("tray")).thenReturn(Optional.of(new Plataforma("Tray", "tray")));
    }

    @Test
    void primeiraSituacaoCriaALinhaELogaSoOQueDifereDoPadrao() {
        plataformaTrayCadastrada();
        when(situacaoRepository.findByCodigoFuncionalidadeAndPlataforma(7L, "tray")).thenReturn(Optional.empty());

        DadosSituacaoFuncionalidade dados = new DadosSituacaoFuncionalidade(StatusFuncionalidade.PARCIAL, null,
                "  Só preço de lista. ", null, "MASTER_Tray_Preco", " Ana ", Prioridade.ALTA, LocalDate.of(2026, 12, 15), "474225");
        SituacaoFuncionalidadeDTO resposta = service.definirSituacao(7L, "tray", dados, admin);

        assertThat(resposta.plataforma()).isEqualTo("tray");
        assertThat(resposta.plataformaNome()).isEqualTo("Tray");
        assertThat(resposta.funcionalidadeId()).isEqualTo(7L);
        assertThat(resposta.status()).isEqualTo(StatusFuncionalidade.PARCIAL);
        assertThat(resposta.teste()).isEqualTo(StatusTeste.NAO_TESTADO);
        assertThat(resposta.observacao()).isEqualTo("Só preço de lista.");
        assertThat(resposta.responsavel()).isEqualTo("Ana");
        assertThat(resposta.alteradoPor()).isEqualTo("Gabriel Carvalheiro");

        List<LogFuncionalidade> logs = logsGravados();
        assertThat(logs.get(0).getAcao()).isEqualTo(AcaoFuncionalidade.INCLUSAO);
        List<LogFuncionalidade> campos = logs.subList(1, logs.size());
        assertThat(campos).allMatch(l -> l.getAcao() == AcaoFuncionalidade.ALTERACAO && "tray".equals(l.getPlataforma()));
        // Teste ficou no padrão (NAO_TESTADO), então não entra; a configuração do ERP, vazia, também não.
        assertThat(campos).extracting(LogFuncionalidade::getCampoAlterado)
                .containsExactly("FUN_STATUS", "FUN_OBSERV", "FUN_WORKFL", "FUN_RESPON", "FUN_PRIORI", "FUN_DTPREV", "FUN_REQALT");
        assertThat(logs.get(1).getValorAnterior()).isEqualTo("NAO_IMPLEMENTADO");
        assertThat(logs.get(1).getValorAtual()).isEqualTo("PARCIAL");
    }

    @Test
    void situacaoComStatusPadraoTambemCriaALinha() {
        plataformaTrayCadastrada();
        when(situacaoRepository.findByCodigoFuncionalidadeAndPlataforma(7L, "tray")).thenReturn(Optional.empty());

        service.definirSituacao(7L, "tray", situacao(StatusFuncionalidade.NAO_IMPLEMENTADO, null), admin);

        // Marcar o status padrão também cria a linha: fica registrado quem mexeu e quando.
        verify(situacaoRepository).save(any(SituacaoFuncionalidade.class));
        assertThat(logsGravados()).extracting(LogFuncionalidade::getAcao).containsExactly(AcaoFuncionalidade.INCLUSAO);
    }

    @Test
    void mudarOTesteCarimbaDataEQuemTestou() {
        plataformaTrayCadastrada();
        SituacaoFuncionalidade existente = situacaoExistente(StatusFuncionalidade.IMPLEMENTADO, StatusTeste.NAO_TESTADO);

        SituacaoFuncionalidadeDTO resposta = service.definirSituacao(7L, "tray",
                situacao(StatusFuncionalidade.IMPLEMENTADO, StatusTeste.TESTADO), admin);

        assertThat(resposta.teste()).isEqualTo(StatusTeste.TESTADO);
        assertThat(resposta.testadoPor()).isEqualTo("Gabriel Carvalheiro");
        assertThat(resposta.testadoEm()).isNotNull();
        assertThat(existente.getDataTeste()).isNotNull();
        assertThat(logsGravados()).extracting(LogFuncionalidade::getCampoAlterado).containsExactly("FUN_RESTES");
    }

    @Test
    void reenviarOMesmoTesteNaoRenovaOCarimbo() {
        plataformaTrayCadastrada();
        SituacaoFuncionalidade existente = situacaoExistente(StatusFuncionalidade.IMPLEMENTADO, StatusTeste.TESTADO);
        LocalDateTime antes = LocalDateTime.of(2026, 11, 1, 9, 0);
        existente.setDataTeste(antes);
        existente.setUsuarioTeste("Ana");

        service.definirSituacao(7L, "tray", situacao(StatusFuncionalidade.IMPLEMENTADO, StatusTeste.TESTADO), admin);

        assertThat(existente.getDataTeste()).isEqualTo(antes);
        assertThat(existente.getUsuarioTeste()).isEqualTo("Ana");
        // Nada mudou: nem a linha nem o log são gravados.
        verify(situacaoRepository, never()).save(any());
        verify(logRepository, never()).saveAll(any());
    }

    @Test
    void voltarParaNaoTestadoLimpaOCarimbo() {
        plataformaTrayCadastrada();
        SituacaoFuncionalidade existente = situacaoExistente(StatusFuncionalidade.IMPLEMENTADO, StatusTeste.COM_FALHA);
        existente.setDataTeste(LocalDateTime.of(2026, 11, 1, 9, 0));
        existente.setUsuarioTeste("Ana");

        service.definirSituacao(7L, "tray", situacao(StatusFuncionalidade.IMPLEMENTADO, null), admin);

        assertThat(existente.getTeste()).isEqualTo(StatusTeste.NAO_TESTADO);
        assertThat(existente.getDataTeste()).isNull();
        assertThat(existente.getUsuarioTeste()).isNull();
    }

    @Test
    void substituiASituacaoInteiraEntaoCampoAusenteLimpa() {
        plataformaTrayCadastrada();
        SituacaoFuncionalidade existente = situacaoExistente(StatusFuncionalidade.PARCIAL, StatusTeste.NAO_TESTADO);
        existente.setObservacao("Só preço de lista.");
        existente.setResponsavel("Ana");

        service.definirSituacao(7L, "tray", situacao(StatusFuncionalidade.IMPLEMENTADO, null), admin);

        assertThat(existente.getStatus()).isEqualTo(StatusFuncionalidade.IMPLEMENTADO);
        assertThat(existente.getObservacao()).isNull();
        assertThat(existente.getResponsavel()).isNull();
        assertThat(logsGravados()).extracting(LogFuncionalidade::getCampoAlterado)
                .containsExactly("FUN_STATUS", "FUN_OBSERV", "FUN_RESPON");
    }

    @Test
    void plataformaNaoCadastradaDa404EAFuncionalidadeNaoEhTocada() {
        when(plataformaRepository.findById("magalu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.definirSituacao(7L, "magalu", situacao(StatusFuncionalidade.IMPLEMENTADO, null), admin))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("magalu");

        verify(situacaoRepository, never()).save(any());
    }

    @Test
    void situacaoDeFuncionalidadeInexistenteDa404() {
        assertThatThrownBy(() -> service.definirSituacao(999L, "tray", situacao(StatusFuncionalidade.IMPLEMENTADO, null), admin))
                .isInstanceOf(NotFoundException.class);

        verify(plataformaRepository, never()).findById(anyString());
    }

    // ---- listar ----

    @Test
    void listarEscapaCuringasDoLikeEIgnoraFiltroVazio() {
        service.listar("  ", null, null, null, "", "100%_x!");

        verify(situacaoEfetivaRepository).buscarPorFiltro(isNull(), isNull(), isNull(), isNull(), isNull(), eq("%100!%!_x!!%"));
    }

    private SituacaoFuncionalidade situacaoExistente(StatusFuncionalidade status, StatusTeste teste) {
        SituacaoFuncionalidade s = new SituacaoFuncionalidade();
        s.setId(1L);
        s.setCodigoFuncionalidade(7L);
        s.setPlataforma("tray");
        s.setStatus(status);
        s.setTeste(teste);
        when(situacaoRepository.findByCodigoFuncionalidadeAndPlataforma(anyLong(), eq("tray"))).thenReturn(Optional.of(s));
        return s;
    }
}
