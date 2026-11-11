package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.IntegracaoResumoDTO;
import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.exception.ForbiddenException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.SituacaoIntegracao;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.IntegracaoService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PATCH /integracoes/{codigoIntegracao}/{codigoCliente}/situacao: qualquer usuário interno (USR, DEV, ADM) chega ao
 * service, que decide qual situação o perfil pode escolher. Lojista (o n8n) e quem não fez login levam 403.
 */
@WebMvcTest(controllers = IntegracaoController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class SituacaoIntegracaoControllerTest {

    private static final String CAMINHO = "/integracoes/0000004/401/situacao";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    IntegracaoService service;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    @BeforeEach
    void preparar() {
        when(service.alterarSituacao(eq("0000004"), eq(401L), any(), any()))
                .thenReturn(resposta("N"));
    }

    private static IntegracaoResumoDTO resposta(String situacao) {
        return new IntegracaoResumoDTO("0000004", 401L, "TODO TETO", "tray", "todo-teto", "S".equals(situacao),
                situacao, "https://loja.commercesuite.com.br/web_api", "http://erp.local:25058", null, null);
    }

    private static Usuario interno(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("pessoa." + perfil.toLowerCase() + "@citelsoftware.com.br");
        usuario.setNome("Pessoa " + perfil);
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private ResultActions fazPatch(RequestPostProcessor quem, String situacao) throws Exception {
        return mvc.perform(patch(CAMINHO).with(quem).contentType(MediaType.APPLICATION_JSON)
                .content("{\"situacao\":" + situacao + "}"));
    }

    // ---- quem chega ao service ----

    @Test
    void usuarioComumDevEAdminChegamAoService() throws Exception {
        for (String perfil : new String[]{"USR", "DEV", "ADM"}) {
            fazPatch(user(interno(perfil)), "\"N\"")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("N"))
                    .andExpect(jsonPath("$.ativo").value(false));
        }
    }

    @Test
    void oServiceRecebeASituacaoEOUsuarioLogado() throws Exception {
        fazPatch(user(interno("DEV")), "\"d\"").andExpect(status().isOk());

        ArgumentCaptor<Usuario> quem = ArgumentCaptor.forClass(Usuario.class);
        verify(service).alterarSituacao(eq("0000004"), eq(401L), eq(SituacaoIntegracao.EM_DESENVOLVIMENTO),
                quem.capture());
        assertThat(quem.getValue().getNome()).isEqualTo("Pessoa DEV");
    }

    @Test
    void oPerfilSemPermissaoParaDLevaOs403DoService() throws Exception {
        when(service.alterarSituacao(any(), any(), eq(SituacaoIntegracao.EM_DESENVOLVIMENTO), any()))
                .thenThrow(new ForbiddenException("Só os perfis DEV e ADM podem colocar uma integração em desenvolvimento."));

        fazPatch(user(interno("USR")), "\"D\"")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Só os perfis DEV e ADM podem colocar uma integração em desenvolvimento."));
    }

    // ---- quem não passa ----

    @Test
    void lojistaNaoAlteraASituacao() throws Exception {
        fazPatch(user(new Cliente("Loja", "loja", "x")), "\"N\"").andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void semLoginNaoAltera() throws Exception {
        mvc.perform(patch(CAMINHO).contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"N\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // ---- validação ----

    @Test
    void situacaoForaDeSNDOuAusenteERecusada() throws Exception {
        fazPatch(user(interno("DEV")), "\"X\"").andExpect(status().isBadRequest());
        fazPatch(user(interno("DEV")), "\"\"").andExpect(status().isBadRequest());
        fazPatch(user(interno("DEV")), "\"SN\"").andExpect(status().isBadRequest());
        fazPatch(user(interno("DEV")), "null").andExpect(status().isBadRequest());
        mvc.perform(patch(CAMINHO).with(user(interno("DEV"))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).alterarSituacao(any(), any(), any(), any());
    }
}
