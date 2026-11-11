package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Uma página de resultados mais o total que atende aos filtros, mesmo o que não veio na página.
 * O total vem de um COUNT no banco, sem carregar os registros.
 */
public record PaginaDTO<T>(
        @Schema(description = "Registros da página pedida")
        List<T> itens,
        @Schema(description = "Número da página devolvida, a partir de 1")
        int pagina,
        @Schema(description = "Registros por página")
        int tamanho,
        @Schema(description = "Total de registros que atendem aos filtros, em todas as páginas")
        long totalItens,
        @Schema(description = "Total de páginas para esse tamanho (0 quando não há registros)")
        int totalPaginas
) {
    /** Converte a página do Spring Data, cuja numeração começa em 0, para a da API, que começa em 1. */
    public static <T> PaginaDTO<T> de(Page<T> page) {
        return new PaginaDTO<>(page.getContent(), page.getNumber() + 1, page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
