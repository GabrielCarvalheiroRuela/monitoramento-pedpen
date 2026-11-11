package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request do PATCH /integracoes/{codigoIntegracao}/{codigoCliente}/situacao.
 *
 * O código vai direto para o CADINT.INT_ATIVO_. Quem pode escolher cada um depende do perfil: USR define S ou N;
 * DEV e ADM também definem D.
 */
public record AlteracaoSituacaoDTO(

        @Schema(example = "S", allowableValues = {"S", "N", "D"},
                description = "S = ATIVO, N = INATIVO, D = EM DESENVOLVIMENTO (só DEV e ADM).")
        @NotNull(message = "situacao é obrigatória")
        @Pattern(regexp = "[SNDsnd]", message = "situacao deve ser S (ativo), N (inativo) ou D (em desenvolvimento)")
        String situacao
) {
}
