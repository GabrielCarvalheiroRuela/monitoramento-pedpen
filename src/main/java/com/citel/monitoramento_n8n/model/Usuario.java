package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Usuário interno da Citel, autenticado pelo Google Workspace (CADUSR).
 *
 * Não é um Cliente: o CADCLI guarda lojistas, e o idInt de um lojista é o que vai parar em
 * PROERR/PEDPEN. Um implantador não tem código de integração nenhum.
 *
 * Implementa UserDetails para poder ser o principal do SecurityContext, mas getPassword() devolve
 * null — não existe senha local, a prova de identidade é sempre o ID Token do Google.
 */
@Entity
@Table(name = "CADUSR")
@Getter
@Setter
public class Usuario implements UserDetails {

    /** Acesso completo. */
    public static final String PERFIL_ADM = "ADM";
    /** Time de desenvolvimento: vê tudo que é de consulta (inclusive a matriz de funcionalidades e os workflows), sem gravar. */
    public static final String PERFIL_DEV = "DEV";
    /** O padrão: monitoramento, integrações e a lista de plataformas. */
    public static final String PERFIL_USR = "USR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USR_CODUSR")
    private Long id;

    @Column(name = "USR_EMAIL_", nullable = false)
    private String email;

    @Column(name = "USR_NOMUSR", nullable = false)
    private String nome;

    /** claim sub do Google: identificador estável, sobrevive à troca de e-mail. */
    @Column(name = "USR_GOOGID")
    private String googleId;

    /** char(1) 'S'/'N'. */
    @Column(name = "USR_ATIVO_", length = 1, nullable = false)
    private String ativo;

    /**
     * Perfil de acesso: 'ADM', 'DEV' ou 'USR'. O perfil é dado do banco, nunca do código: muda-se
     * com um UPDATE no CADUSR e vale na próxima requisição, porque o SecurityFilter relê o usuário a
     * cada chamada. Todo usuário novo nasce 'USR'.
     */
    @Column(name = "USR_PERFIL", length = 10, nullable = false)
    private String perfil = PERFIL_USR;

    @Column(name = "USR_DHUINC", insertable = false, updatable = false)
    private LocalDateTime dataInclusao;

    @Column(name = "USR_DHUACE")
    private LocalDateTime dataUltimoAcesso;

    /**
     * O perfil em uma das três formas válidas, sem diferenciar caixa nem espaços. Nulo ou qualquer outro valor
     * (inclusive os antigos ADMIN e USUARIO) vira USR: na dúvida o acesso fecha, e um erro de digitação no banco
     * nunca promove ninguém.
     */
    public String perfilEfetivo() {
        String normalizado = perfil == null ? "" : perfil.trim().toUpperCase(Locale.ROOT);
        return PERFIL_ADM.equals(normalizado) || PERFIL_DEV.equals(normalizado) ? normalizado : PERFIL_USR;
    }

    public boolean isAdmin() {
        return PERFIL_ADM.equals(perfilEfetivo());
    }

    public boolean isDev() {
        return PERFIL_DEV.equals(perfilEfetivo());
    }

    /**
     * ROLE_INTERNO vale para todos. ROLE_DEV é de quem tem o perfil DEV e também do ADM, que tem acesso completo, então
     * uma regra "pelo menos DEV" é só hasRole("DEV"). ROLE_ADMIN é só do ADM.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isAdmin()) {
            return List.of(new SimpleGrantedAuthority("ROLE_INTERNO"), new SimpleGrantedAuthority("ROLE_DEV"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (isDev()) {
            return List.of(new SimpleGrantedAuthority("ROLE_INTERNO"), new SimpleGrantedAuthority("ROLE_DEV"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_INTERNO"));
    }

    /** Não há senha local: quem autentica é o Google. */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "S".equalsIgnoreCase(this.ativo);
    }
}
