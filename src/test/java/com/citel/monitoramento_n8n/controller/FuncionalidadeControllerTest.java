package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.FuncionalidadeService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /funcionalidades: consulta e marcação da situação para DEV e ADM; criar, editar e excluir só para ADM. O USR não vê a
 * matriz (RF07, critério de aceite 2).
 * A barreira é do servidor: o botão escondido no portal não protege nada contra Swagger/Postman.
 */
@WebMvcTest(controllers = FuncionalidadeController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class FuncionalidadeControllerTest {

    private static final String CRIAR = "{\"modulo\":\"Preços\",\"nome\":\"Nova\"}";
    private static final String SITUACAO = "{\"status\":\"IMPLEMENTADO\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FuncionalidadeService service;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    Usuario admin;
    Usuario dev;
    Usuario usr;

    @BeforeEach
    void preparar() {
        admin = interno("Gabriel Carvalheiro", "ADM");
        dev = interno("Pessoa Dev", "DEV");
        usr = interno("Pessoa Comum", "USR");
    }

    private static Usuario interno(String nome, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail(nome.toLowerCase().replace(' ', '.') + "@citelsoftware.com.br");
        usuario.setNome(nome);
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private ResultActions json(MockHttpServletRequestBuilder requisicao, RequestPostProcessor quem, String corpo) throws Exception {
        return mvc.perform(requisicao.with(quem).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    // ---- consulta ----

    @Test
    void devEAdmConsultam() throws Exception {
        org.mockito.Mockito.when(service.listar(null, null, null, null, null, null)).thenReturn(List.of());
        org.mockito.Mockito.when(service.cobertura()).thenReturn(List.of());

        for (Usuario quem : List.of(dev, admin)) {
            mvc.perform(get("/funcionalidades").with(user(quem))).andExpect(status().isOk());
            mvc.perform(get("/funcionalidades/cobertura").with(user(quem))).andExpect(status().isOk());
        }
    }

    @Test
    void usrNaoConsulta() throws Exception {
        mvc.perform(get("/funcionalidades").with(user(usr))).andExpect(status().isForbidden());
        mvc.perform(get("/funcionalidades/cobertura").with(user(usr))).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void lojistaNaoConsulta() throws Exception {
        RequestPostProcessor lojista = user(new Cliente("Loja", "loja", "x"));

        mvc.perform(get("/funcionalidades").with(lojista)).andExpect(status().isForbidden());
        mvc.perform(get("/funcionalidades/cobertura").with(lojista)).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void semLoginNaoConsulta() throws Exception {
        mvc.perform(get("/funcionalidades")).andExpect(status().is4xxClientError());

        verifyNoInteractions(service);
    }

    // ---- gravação ----

    @Test
    void usrNaoGravaNada() throws Exception {
        json(post("/funcionalidades"), user(usr), CRIAR).andExpect(status().isForbidden());
        json(patch("/funcionalidades/7"), user(usr), "{\"nome\":\"x\"}").andExpect(status().isForbidden());
        json(put("/funcionalidades/7/plataformas/tray"), user(usr), SITUACAO).andExpect(status().isForbidden());
        mvc.perform(delete("/funcionalidades/7").with(user(usr))).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void devMarcaASituacaoMasNaoCriaNemEditaNemExclui() throws Exception {
        json(put("/funcionalidades/7/plataformas/tray"), user(dev), SITUACAO).andExpect(status().isOk());

        json(post("/funcionalidades"), user(dev), CRIAR).andExpect(status().isForbidden());
        json(patch("/funcionalidades/7"), user(dev), "{\"nome\":\"x\"}").andExpect(status().isForbidden());
        mvc.perform(delete("/funcionalidades/7").with(user(dev))).andExpect(status().isForbidden());

        verify(service).definirSituacao(eq(7L), eq("tray"), any(), eq(dev));
        verifyNoMoreInteractions(service);
    }

    @Test
    void lojistaNaoGrava() throws Exception {
        RequestPostProcessor lojista = user(new Cliente("Loja", "loja", "x"));

        json(post("/funcionalidades"), lojista, CRIAR).andExpect(status().isForbidden());
        json(patch("/funcionalidades/7"), lojista, "{\"nome\":\"x\"}").andExpect(status().isForbidden());
        json(put("/funcionalidades/7/plataformas/tray"), lojista, SITUACAO).andExpect(status().isForbidden());
        mvc.perform(delete("/funcionalidades/7").with(lojista)).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminGravaEPayloadInvalidoDa400() throws Exception {
        // O serviço é mock e devolve null: o que se confere aqui é a validação do corpo, que roda antes dele.
        json(post("/funcionalidades"), user(admin), "{\"modulo\":\"\",\"nome\":\"x\"}").andExpect(status().isBadRequest());
        json(put("/funcionalidades/7/plataformas/tray"), user(admin), "{}").andExpect(status().isBadRequest());
        json(put("/funcionalidades/7/plataformas/tray"), user(admin), "{\"status\":\"QUALQUER\"}").andExpect(status().isBadRequest());
        json(patch("/funcionalidades/7"), user(admin), "{\"nome\":\"   \"}").andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void adminExclui() throws Exception {
        mvc.perform(delete("/funcionalidades/7").with(user(admin))).andExpect(status().isNoContent());
    }
}
