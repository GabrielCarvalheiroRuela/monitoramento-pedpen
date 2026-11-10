package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request do PATCH /funcionalidades/{id}. Campo ausente ou nulo preserva o valor atual. Módulo e nome não podem
 * ficar vazios; a descrição, ao contrário, pode ser apagada enviando texto vazio.
 */
@Schema(description = "Campos ausentes ou nulos preservam o valor atual.")
public record EdicaoFuncionalidadeDTO(

        @Pattern(regexp = "(?s).*\\S.*", message = "modulo não pode ser vazio")
        @Size(max = 80, message = "modulo deve ter no máximo 80 caracteres")
        String modulo,

        @Pattern(regexp = "(?s).*\\S.*", message = "nome não pode ser vazio")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String nome,

        @Schema(description = "Texto vazio apaga a descrição.")
        String descricao,

        @PositiveOrZero(message = "ordem não pode ser negativa")
        Integer ordem
) {
}
