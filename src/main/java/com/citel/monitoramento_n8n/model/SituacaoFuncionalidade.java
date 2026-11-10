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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A situação de uma funcionalidade numa plataforma (PLAFUN). Uma linha por par (funcionalidade, plataforma);
 * sem linha, a funcionalidade conta como NAO_IMPLEMENTADO (é o que as views VW_* fazem).
 */
@Entity
@Table(name = "PLAFUN")
@Getter
@Setter
public class SituacaoFuncionalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUTOINCREM")
    private Long id;

    /** CADFUN.AUTOINCREM. Sem associação JPA: nada aqui precisa navegar até a funcionalidade. */
    @Column(name = "FUN_CODFUN", nullable = false)
    private Long codigoFuncionalidade;

    /** CADPLA.PLA_SISEXT, ex.: tray. */
    @Column(name = "FUN_SISEXT", nullable = false, length = 50)
    private String plataforma;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUN_STATUS", nullable = false)
    private StatusFuncionalidade status = StatusFuncionalidade.NAO_IMPLEMENTADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUN_RESTES", nullable = false)
    private StatusTeste teste = StatusTeste.NAO_TESTADO;

    @Column(name = "FUN_DHTEST")
    private LocalDateTime dataTeste;

    @Column(name = "FUN_USRTST")
    private String usuarioTeste;

    @Column(name = "FUN_OBSERV", columnDefinition = "text")
    private String observacao;

    /** Parâmetros do ERP envolvidos, ex.: CFG_CONKEY, CFG_SECRET. */
    @Column(name = "FUN_CFGERP", columnDefinition = "text")
    private String configuracaoErp;

    /** Workflow(s) do n8n que implementam. */
    @Column(name = "FUN_WORKFL", columnDefinition = "text")
    private String workflow;

    @Column(name = "FUN_RESPON")
    private String responsavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUN_PRIORI")
    private Prioridade prioridade;

    @Column(name = "FUN_DTPREV")
    private LocalDate previsao;

    /** Nº do requisito no Altera que trata desta funcionalidade. */
    @Column(name = "FUN_REQALT", length = 20)
    private String requisitoAltera;

    @Column(name = "FUN_USRALT")
    private String usuarioAlteracao;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "FUN_DHUALT", insertable = false, updatable = false)
    private LocalDateTime dataAlteracao;
}
