package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.IntegracaoResumoDTO;
import com.citel.monitoramento_n8n.exception.ForbiddenException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Integracao;
import com.citel.monitoramento_n8n.model.LogIntegracao;
import com.citel.monitoramento_n8n.model.SituacaoIntegracao;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.IntegracaoRepository;
import com.citel.monitoramento_n8n.repository.LogIntegracaoRepository;
import com.citel.monitoramento_n8n.repository.PlataformaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.citel.monitoramento_n8n.model.SituacaoIntegracao.ATIVO;
import static com.citel.monitoramento_n8n.model.SituacaoIntegracao.EM_DESENVOLVIMENTO;
import static com.citel.monitoramento_n8n.model.SituacaoIntegracao.INATIVO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Situação da integração: USR define ATIVO e INATIVO; EM DESENVOLVIMENTO é do DEV e do ADM. O LOGINT guarda os
 * rótulos, não as siglas, e só ganha linha quando a situação realmente muda.
 */
@ExtendWith(MockitoExtension.class)
class IntegracaoServiceSituacaoTest {

    @Mock
    IntegracaoRepository repository;
    @Mock
    ClienteRepository clienteRepository;
    @Mock
    PlataformaRepository plataformaRepository;
    @Mock
    LogIntegracaoRepository logRepository;

    IntegracaoService service;
    Integracao integracao;

    @BeforeEach
    void preparar() {
        service = new IntegracaoService(repository, clienteRepository, plataformaRepository, logRepository);

        integracao = new Integracao();
        integracao.setCodigoIntegracao("0000004");
        integracao.setCodigoCliente(401L);
        integracao.setCliente(new Cliente("TODO TETO", "todoteto", "x"));
        integracao.setPlataforma("tray");
        integracao.setSlug("todo-teto");
        integracao.setAtivo("S");
        integracao.setUrlWebservice("http://erp.local:25058");

        lenient().when(repository.findByCodigoIntegracaoAndCodigoCliente("0000004", 401L))
                .thenReturn(Optional.of(integracao));
        lenient().when(repository.save(any(Integracao.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Usuario com(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setEmail(perfil.toLowerCase() + "@citelsoftware.com.br");
        usuario.setNome("Pessoa " + perfil);
        usuario.setPerfil(perfil);
        return usuario;
    }

    private LogIntegracao logGravado() {
        ArgumentCaptor<LogIntegracao> captor = ArgumentCaptor.forClass(LogIntegracao.class);
        verify(logRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- quem pode o quê ----

    @Test
    void usuarioComumInativaEReativa() {
        service.alterarSituacao("0000004", 401L, INATIVO, com("USR"));
        assertThat(integracao.getAtivo()).isEqualTo("N");

        service.alterarSituacao("0000004", 401L, ATIVO, com("USR"));
        assertThat(integracao.getAtivo()).isEqualTo("S");
    }

    @Test
    void usuarioComumNaoColocaEmDesenvolvimento() {
        assertThatThrownBy(() -> service.alterarSituacao("0000004", 401L, EM_DESENVOLVIMENTO, com("USR")))
                .isInstanceOf(ForbiddenException.class);

        assertThat(integracao.getAtivo()).isEqualTo("S");
        verify(repository, never()).save(any());
        verifyNoInteractions(logRepository);
    }

    @Test
    void perfilDesconhecidoContaComoUsuarioComum() {
        assertThatThrownBy(() -> service.alterarSituacao("0000004", 401L, EM_DESENVOLVIMENTO, com("ADMIN")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void devDefineAsTresSituacoes() {
        service.alterarSituacao("0000004", 401L, EM_DESENVOLVIMENTO, com("DEV"));
        assertThat(integracao.getAtivo()).isEqualTo("D");

        service.alterarSituacao("0000004", 401L, INATIVO, com("DEV"));
        assertThat(integracao.getAtivo()).isEqualTo("N");

        service.alterarSituacao("0000004", 401L, ATIVO, com("DEV"));
        assertThat(integracao.getAtivo()).isEqualTo("S");
    }

    @Test
    void adminDefineAsTresSituacoes() {
        service.alterarSituacao("0000004", 401L, EM_DESENVOLVIMENTO, com("ADM"));
        assertThat(integracao.getAtivo()).isEqualTo("D");

        service.alterarSituacao("0000004", 401L, INATIVO, com("ADM"));
        assertThat(integracao.getAtivo()).isEqualTo("N");

        service.alterarSituacao("0000004", 401L, ATIVO, com("ADM"));
        assertThat(integracao.getAtivo()).isEqualTo("S");
    }

    @Test
    void usuarioComumNaoMexeEmIntegracaoQueJaEstaEmDesenvolvimento() {
        integracao.setAtivo("D");

        assertThatThrownBy(() -> service.alterarSituacao("0000004", 401L, INATIVO, com("USR")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.alterarSituacao("0000004", 401L, ATIVO, com("USR")))
                .isInstanceOf(ForbiddenException.class);

        assertThat(integracao.getAtivo()).isEqualTo("D");
        verify(repository, never()).save(any());
        verifyNoInteractions(logRepository);
    }

    @Test
    void devEAdminTiramDeEmDesenvolvimento() {
        integracao.setAtivo("D");
        service.alterarSituacao("0000004", 401L, ATIVO, com("DEV"));
        assertThat(integracao.getAtivo()).isEqualTo("S");

        integracao.setAtivo("D");
        service.alterarSituacao("0000004", 401L, INATIVO, com("ADM"));
        assertThat(integracao.getAtivo()).isEqualTo("N");
    }

    // ---- o que vai para o log ----

    @Test
    void oLogGuardaORotuloENaoASigla() {
        service.alterarSituacao("0000004", 401L, EM_DESENVOLVIMENTO, com("DEV"));

        LogIntegracao linha = logGravado();
        assertThat(linha.getCampoAlterado()).isEqualTo("INT_ATIVO_");
        assertThat(linha.getValorAnterior()).isEqualTo("ATIVO");
        assertThat(linha.getValorAtual()).isEqualTo("EM DESENVOLVIMENTO");
    }

    @Test
    void oLogCarregaPlataformaClienteCodigoEUsuario() {
        service.alterarSituacao("0000004", 401L, INATIVO, com("USR"));

        LogIntegracao linha = logGravado();
        assertThat(linha.getNomePlataforma()).isEqualTo("tray");
        assertThat(linha.getNomeCliente()).isEqualTo("TODO TETO");
        assertThat(linha.getCodigoIntegracao()).isEqualTo("0000004");
        assertThat(linha.getNomeUsuario()).isEqualTo("Pessoa USR");
        assertThat(linha.getValorAnterior()).isEqualTo("ATIVO");
        assertThat(linha.getValorAtual()).isEqualTo("INATIVO");
    }

    @Test
    void pedirASituacaoQueJaEstaNaoGravaNada() {
        service.alterarSituacao("0000004", 401L, ATIVO, com("USR"));

        verify(repository, never()).save(any());
        verifyNoInteractions(logRepository);
    }

    // ---- resposta e erros ----

    @Test
    void aRespostaTrazAsituacaoNovaEOBooleanoDeAtivo() {
        IntegracaoResumoDTO emDev = service.alterarSituacao("0000004", 401L, EM_DESENVOLVIMENTO, com("DEV"));
        assertThat(emDev.situacao()).isEqualTo("D");
        assertThat(emDev.ativo()).isFalse();

        IntegracaoResumoDTO ativa = service.alterarSituacao("0000004", 401L, ATIVO, com("DEV"));
        assertThat(ativa.situacao()).isEqualTo("S");
        assertThat(ativa.ativo()).isTrue();
    }

    @Test
    void integracaoInexistenteDaNotFoundESemLog() {
        assertThatThrownBy(() -> service.alterarSituacao("9999999", 1L, INATIVO, com("USR")))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(logRepository);
    }

    // ---- o que não pode ter mudado ----

    @Test
    void soAIntegracaoAtivaContaComoAtiva() {
        integracao.setAtivo("D");
        assertThat(integracao.isAtivo()).isFalse();

        integracao.setAtivo("N");
        assertThat(integracao.isAtivo()).isFalse();

        integracao.setAtivo("s");
        assertThat(integracao.isAtivo()).isTrue();
    }

    @Test
    void valorDesconhecidoNoBancoContaComoInativo() {
        integracao.setAtivo("X");

        assertThat(integracao.situacao()).isEqualTo(INATIVO);
        assertThat(integracao.isAtivo()).isFalse();
    }

    @Test
    void codigoDeSituacaoAceitaCaixaEEspacosEIgnoraOResto() {
        assertThat(SituacaoIntegracao.deCodigo(" d ")).isEqualTo(EM_DESENVOLVIMENTO);
        assertThat(SituacaoIntegracao.deCodigo("n")).isEqualTo(INATIVO);
        assertThat(SituacaoIntegracao.deCodigo("X")).isNull();
        assertThat(SituacaoIntegracao.deCodigo(null)).isNull();
    }
}
