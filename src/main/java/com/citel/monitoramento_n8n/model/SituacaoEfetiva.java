package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Leitura da view VW_STAFUN: uma linha por funcionalidade x plataforma cadastrada, já com "sem registro =
 * NAO_IMPLEMENTADO". É a mesma view de onde o servidor MCP lê, então o portal e o Claude veem os mesmos números.
 */
@Entity
@Table(name = "VW_STAFUN")
@Immutable
@IdClass(SituacaoEfetiva.Chave.class)
@Getter
public class SituacaoEfetiva {

    @Id
    @Column(name = "FUN_SISEXT")
    private String plataforma;

    @Id
    @Column(name = "FUN_CODFUN")
    private Long funcionalidadeId;

    @Column(name = "FUN_DESPLA")
    private String plataformaNome;

    @Column(name = "FUN_MODULO")
    private String modulo;

    @Column(name = "FUN_NOMFUN")
    private String nome;

    @Column(name = "FUN_DESFUN", columnDefinition = "text")
    private String descricao;

    @Column(name = "FUN_ORDFUN")
    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUN_STATUS")
    private StatusFuncionalidade status;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUN_RESTES")
    private StatusTeste teste;

    @Column(name = "FUN_DHTEST")
    private LocalDateTime testadoEm;

    @Column(name = "FUN_USRTST")
    private String testadoPor;

    @Column(name = "FUN_OBSERV", columnDefinition = "text")
    private String observacao;

    @Column(name = "FUN_CFGERP", columnDefinition = "text")
    private String configuracaoErp;

    @Column(name = "FUN_WORKFL", columnDefinition = "text")
    private String workflow;

    @Column(name = "FUN_RESPON")
    private String responsavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "FUN_PRIORI")
    private Prioridade prioridade;

    @Column(name = "FUN_DTPREV")
    private LocalDate previsao;

    @Column(name = "FUN_REQALT")
    private String requisitoAltera;

    @Column(name = "FUN_USRALT")
    private String alteradoPor;

    @Column(name = "FUN_DHUALT")
    private LocalDateTime alteradoEm;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Chave implements Serializable {
        private String plataforma;
        private Long funcionalidadeId;
    }
}
