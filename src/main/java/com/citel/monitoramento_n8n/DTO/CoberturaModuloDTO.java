package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.CoberturaModulo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Cobertura de uma plataforma num módulo. */
public record CoberturaModuloDTO(
        String modulo,
        long total,
        long implementadas,
        long parciais,
        long naoImplementadas,
        long naoSeAplica,
        @Schema(description = "Funcionalidades implementadas ou parciais que já foram testadas.")
        long testadas,
        @Schema(description = "(implementadas + 0,5 x parciais) / (total - naoSeAplica), em %. Nulo quando nada se aplica: a tela mostra \"-\".")
        BigDecimal percentual
) {

    public static CoberturaModuloDTO de(CoberturaModulo m) {
        return new CoberturaModuloDTO(m.getModulo(), m.getTotal(), m.getImplementadas(), m.getParciais(),
                m.getNaoImplementadas(), m.getNaoSeAplica(), m.getTestadas(), m.getPercentual());
    }
}
