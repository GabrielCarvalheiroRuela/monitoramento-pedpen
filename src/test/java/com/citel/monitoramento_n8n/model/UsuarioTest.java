package com.citel.monitoramento_n8n.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private static Usuario comPerfil(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setEmail("pessoa@citelsoftware.com.br");
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static java.util.List<String> autoridades(Usuario usuario) {
        return usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void usuarioNovoNasceComoUsr() {
        assertThat(new Usuario().getPerfil()).isEqualTo(Usuario.PERFIL_USR);
        assertThat(new Usuario().isAdmin()).isFalse();
        assertThat(new Usuario().isDev()).isFalse();
    }

    @Test
    void admRecebeInternoDevEAdmin() {
        assertThat(autoridades(comPerfil("ADM"))).containsExactly("ROLE_INTERNO", "ROLE_DEV", "ROLE_ADMIN");
    }

    @Test
    void devRecebeInternoEDevMasNaoAdmin() {
        Usuario dev = comPerfil("DEV");

        assertThat(autoridades(dev)).containsExactly("ROLE_INTERNO", "ROLE_DEV");
        assertThat(dev.isDev()).isTrue();
        assertThat(dev.isAdmin()).isFalse();
    }

    @Test
    void usrRecebeSoInterno() {
        assertThat(autoridades(comPerfil("USR"))).containsExactly("ROLE_INTERNO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADM", "adm", " Adm "})
    void admSeReconheceSemCaixaNemEspacos(String perfil) {
        assertThat(comPerfil(perfil).isAdmin()).isTrue();
        assertThat(comPerfil(perfil).perfilEfetivo()).isEqualTo("ADM");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEV", "dev", " Dev "})
    void devSeReconheceSemCaixaNemEspacos(String perfil) {
        assertThat(comPerfil(perfil).isDev()).isTrue();
        assertThat(comPerfil(perfil).perfilEfetivo()).isEqualTo("DEV");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"USR", "ADMIN", "USUARIO", "ADMN", "SUPERADM", "ADM;", "DEVELOPER", "1"})
    void qualquerOutroValorContaComoUsr(String perfil) {
        Usuario usuario = comPerfil(perfil);

        assertThat(usuario.perfilEfetivo()).isEqualTo("USR");
        assertThat(usuario.isAdmin()).isFalse();
        assertThat(usuario.isDev()).isFalse();
        assertThat(autoridades(usuario)).containsExactly("ROLE_INTERNO");
    }

    @Test
    void perfilNaoMudaAContaAtivaNemInativa() {
        Usuario admin = comPerfil("ADM");
        assertThat(admin.isEnabled()).isTrue();

        admin.setAtivo("N");
        assertThat(admin.isEnabled()).isFalse();
    }
}
