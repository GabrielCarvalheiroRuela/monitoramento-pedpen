package com.citel.monitoramento_n8n.model;

/** Situação de uma funcionalidade numa plataforma (PLAFUN.FUN_STATUS). Sem linha no banco conta como NAO_IMPLEMENTADO. */
public enum StatusFuncionalidade {
    IMPLEMENTADO,
    PARCIAL,
    NAO_IMPLEMENTADO,
    NAO_SE_APLICA
}
