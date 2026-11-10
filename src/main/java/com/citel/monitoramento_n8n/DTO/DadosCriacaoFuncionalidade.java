package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request do POST /funcionalidades. A situação em cada plataforma vem depois, no PUT .../plataformas/{plataforma}. */
public record DadosCriacaoFuncionalidade(

        @Schema(example = "Pedidos - Pagamento")
        @NotBlank(message = "modulo é obrigatório")
        @Size(max = 80, message = "modulo deve ter no máximo 80 caracteres")
        String modulo,

        @Schema(example = "Atualizar status do pedido na plataforma após pagamento", description = "Nome curto.")
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String nome,

        @Schema(description = "O que a funcionalidade faz e qual a regra.")
        String descricao,

        @Schema(description = "Posição na lista inteira. Ausente: entra no fim.")
        @PositiveOrZero(message = "ordem não pode ser negativa")
        Integer ordem
) {
}
