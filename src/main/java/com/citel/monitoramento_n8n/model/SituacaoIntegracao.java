package com.citel.monitoramento_n8n.model;

import java.util.Locale;

/**
 * Situação de uma integração (CADINT.INT_ATIVO_, char(1)).
 *
 * O código é o que o banco e a API trocam; o rótulo é o que as pessoas leem — a tela e o LOGINT mostram o
 * rótulo, nunca a sigla.
 */
public enum SituacaoIntegracao {
    ATIVO("S", "ATIVO"),
    INATIVO("N", "INATIVO"),
    EM_DESENVOLVIMENTO("D", "EM DESENVOLVIMENTO");

    private final String codigo;
    private final String rotulo;

    SituacaoIntegracao(String codigo, String rotulo) {
        this.codigo = codigo;
        this.rotulo = rotulo;
    }

    public String codigo() {
        return codigo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** O código (S, N ou D) sem diferenciar caixa nem espaços; nulo se não for um dos três. */
    public static SituacaoIntegracao deCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
        for (SituacaoIntegracao situacao : values()) {
            if (situacao.codigo.equals(normalizado)) {
                return situacao;
            }
        }
        return null;
    }
}
