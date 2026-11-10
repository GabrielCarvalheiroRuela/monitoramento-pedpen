package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A lista única (modelo) de funcionalidades das integrações (CADFUN). Vale para todas as plataformas;
 * a situação em cada uma fica em {@link SituacaoFuncionalidade}. Req. Altera 474225.
 *
 * Excluir uma funcionalidade apaga as situações dela junto: quem faz isso é o ON DELETE CASCADE do banco.
 */
@Entity
@Table(name = "CADFUN")
@Getter
@Setter
public class Funcionalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUTOINCREM")
    private Long id;

    @Column(name = "FUN_MODULO", nullable = false, length = 80)
    private String modulo;

    /** Nome curto. */
    @Column(name = "FUN_NOMFUN", nullable = false, length = 120)
    private String nome;

    /** O que faz e qual a regra, com calma. */
    @Column(name = "FUN_DESFUN", columnDefinition = "text")
    private String descricao;

    /** Posição na lista inteira (e não dentro do módulo): a ordem dos módulos é a de quem aparece primeiro. */
    @Column(name = "FUN_ORDFUN", nullable = false)
    private int ordem;

    @Column(name = "FUN_USRALT")
    private String usuarioAlteracao;

    /** DEFAULT CURRENT_TIMESTAMP ON UPDATE do banco; o Hibernate a relê após gravar. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "FUN_DHUALT", insertable = false, updatable = false)
    private LocalDateTime dataAlteracao;
}
