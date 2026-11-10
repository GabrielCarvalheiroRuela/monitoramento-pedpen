package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Leitura da view VW_COBPLA: cobertura da plataforma inteira. Não é a média dos módulos: a view soma tudo e faz a
 * conta uma vez. O percentual vem nulo quando nada se aplica.
 */
@Entity
@Table(name = "VW_COBPLA")
@Immutable
@Getter
public class CoberturaPlataforma {

    @Id
    @Column(name = "FUN_SISEXT")
    private String plataforma;

    @Column(name = "FUN_DESPLA")
    private String plataformaNome;

    @Column(name = "FUN_QTDTOT")
    private long total;

    @Column(name = "FUN_QTDIMP")
    private long implementadas;

    @Column(name = "FUN_QTDPAR")
    private long parciais;

    @Column(name = "FUN_QTDNAO")
    private long naoImplementadas;

    @Column(name = "FUN_QTDNSA")
    private long naoSeAplica;

    @Column(name = "FUN_QTDTST")
    private long testadas;

    @Column(name = "FUN_PERCOB")
    private BigDecimal percentual;
}
