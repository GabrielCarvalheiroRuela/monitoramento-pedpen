package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Funcionalidade;

import java.time.LocalDateTime;

/** Uma funcionalidade da lista única. Resposta do POST e do PATCH /funcionalidades. */
public record FuncionalidadeDTO(
        Long id,
        String modulo,
        String nome,
        String descricao,
        int ordem,
        String alteradoPor,
        LocalDateTime alteradoEm
) {

    public static FuncionalidadeDTO de(Funcionalidade f) {
        return new FuncionalidadeDTO(f.getId(), f.getModulo(), f.getNome(), f.getDescricao(), f.getOrdem(),
                f.getUsuarioAlteracao(), f.getDataAlteracao());
    }
}
