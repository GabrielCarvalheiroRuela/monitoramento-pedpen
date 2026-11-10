package com.citel.monitoramento_n8n.service;

import com.auth0.jwt.JWT;
import com.citel.monitoramento_n8n.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServicePerfilTest {

    private TokenService service;

    @BeforeEach
    void iniciar() {
        service = new TokenService();
        ReflectionTestUtils.setField(service, "secret", "segredo-de-teste-com-mais-de-32-caracteres");
        ReflectionTestUtils.setField(service, "issuer", "API de Teste");
    }

    private static Usuario usuario(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("pessoa@citelsoftware.com.br");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private String perfilDoToken(String perfil) {
        String token = service.gerarTokenUsuario(usuario(perfil));
        return JWT.decode(token).getClaim(TokenService.CLAIM_PERFIL).asString();
    }

    @Test
    void tokenLevaOPerfilDeCadaUm() {
        assertThat(perfilDoToken("ADM")).isEqualTo("ADM");
        assertThat(perfilDoToken("DEV")).isEqualTo("DEV");
        assertThat(perfilDoToken("USR")).isEqualTo("USR");
    }

    @Test
    void oClaimVaiNormalizadoEnaoComoEstaNoBanco() {
        assertThat(perfilDoToken(" adm ")).isEqualTo("ADM");
        assertThat(perfilDoToken(" dev ")).isEqualTo("DEV");
        assertThat(perfilDoToken("ADMIN")).isEqualTo("USR");
        assertThat(perfilDoToken("ADMN")).isEqualTo("USR");
        assertThat(perfilDoToken(null)).isEqualTo("USR");
    }

    @Test
    void oTokenContinuaIdentificandoOUsuarioInterno() {
        String token = service.gerarTokenUsuario(usuario("ADM"));

        TokenService.Identidade identidade = service.decodificar(token);

        assertThat(identidade).isNotNull();
        assertThat(identidade.subject()).isEqualTo("pessoa@citelsoftware.com.br");
        assertThat(identidade.tipo()).isEqualTo(TokenService.TIPO_USUARIO);
    }
}
