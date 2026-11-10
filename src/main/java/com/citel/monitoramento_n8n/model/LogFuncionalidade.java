package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

/**
 * Histórico de alterações da matriz de funcionalidades (LOGFUN), no formato do LOGINT: quem, quando, e de quê
 * para quê. Guarda o nome da funcionalidade (e não só o id) para continuar legível depois que ela for excluída,
 * por isso não tem chave estrangeira.
 */
@Entity
@Table(name = "LOGFUN")
@Getter
@Setter
public class LogFuncionalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUTOINCREM")
    private Long id;

    /** Preenchida pelo DEFAULT CURRENT_TIMESTAMP do banco; o Hibernate a relê após o INSERT. */
    @Generated(event = EventType.INSERT)
    @Column(name = "FUN_DHUALT", insertable = false, updatable = false)
    private LocalDateTime dataAlteracao;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUN_TIPACA", nullable = false)
    private AcaoFuncionalidade acao;

    @Column(name = "FUN_MODULO")
    private String modulo;

    /** Nome da funcionalidade no momento da alteração. */
    @Column(name = "FUN_NOMFUN")
    private String nomeFuncionalidade;

    /** Plataforma; vazio quando a mudança é na lista (CADFUN) e não na situação. */
    @Column(name = "FUN_SISEXT")
    private String plataforma;

    /** Coluna alterada (FUN_NOMFUN, FUN_STATUS...). Vazio em inclusão e exclusão. */
    @Column(name = "FUN_CMPALT")
    private String campoAlterado;

    @Column(name = "FUN_VALANT", columnDefinition = "text")
    private String valorAnterior;

    @Column(name = "FUN_VALATU", columnDefinition = "text")
    private String valorAtual;

    /** Nome do usuário interno (CADUSR) que fez a alteração. */
    @Column(name = "FUN_NOMUSU")
    private String nomeUsuario;
}
