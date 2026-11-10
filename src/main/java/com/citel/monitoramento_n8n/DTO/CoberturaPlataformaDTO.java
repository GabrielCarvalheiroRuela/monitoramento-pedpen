package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.CoberturaPlataforma;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** Cobertura de uma plataforma inteira, com o detalhe por módulo (na ordem da lista). */
public record CoberturaPlataformaDTO(
        String plataforma,
        String plataformaNome,
        long total,
        long implementadas,
        long parciais,
        long naoImplementadas,
        long naoSeAplica,
        @Schema(description = "Funcionalidades implementadas ou parciais que já foram testadas.")
        long testadas,
        @Schema(description = "(implementadas + 0,5 x parciais) / (total - naoSeAplica), em %. Nulo quando nada se aplica: a tela mostra \"-\".")
        BigDecimal percentual,
        List<CoberturaModuloDTO> modulos
) {

    public static CoberturaPlataformaDTO de(CoberturaPlataforma p, List<CoberturaModuloDTO> modulos) {
        return new CoberturaPlataformaDTO(p.getPlataforma(), p.getPlataformaNome(), p.getTotal(), p.getImplementadas(),
                p.getParciais(), p.getNaoImplementadas(), p.getNaoSeAplica(), p.getTestadas(), p.getPercentual(), modulos);
    }
}
