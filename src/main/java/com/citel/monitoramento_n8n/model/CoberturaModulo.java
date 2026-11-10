package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Leitura da view VW_COBFUN: cobertura de uma plataforma num módulo.
 * (IMPLEMENTADO + 0,5 x PARCIAL) / (total - NAO_SE_APLICA), em %. O percentual vem nulo quando nada se aplica.
 */
@Entity
@Table(name = "VW_COBFUN")
@Immutable
@IdClass(CoberturaModulo.Chave.class)
@Getter
public class CoberturaModulo {

    @Id
    @Column(name = "FUN_SISEXT")
    private String plataforma;

    @Id
    @Column(name = "FUN_MODULO")
    private String modulo;

    /** Menor ordem das funcionalidades do módulo: dá a ordem de exibição dos módulos. */
    @Column(name = "FUN_ORDFUN")
    private int ordem;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Chave implements Serializable {
        private String plataforma;
        private String modulo;
    }
}
