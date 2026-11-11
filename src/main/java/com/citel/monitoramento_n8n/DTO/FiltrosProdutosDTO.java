package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Opções dos filtros da tela de produtos: só o que tem registro no status consultado. */
public record FiltrosProdutosDTO(
        @Schema(description = "Clientes que têm produto no status, sem repetir, em ordem alfabética")
        List<String> clientes,
        @Schema(description = "Plataformas que têm produto no status, sem repetir, em ordem alfabética")
        List<String> plataformas
) {
}
