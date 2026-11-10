package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Funcionalidade;
import com.citel.monitoramento_n8n.model.Prioridade;
import com.citel.monitoramento_n8n.model.SituacaoEfetiva;
import com.citel.monitoramento_n8n.model.SituacaoFuncionalidade;
import com.citel.monitoramento_n8n.model.StatusFuncionalidade;
import com.citel.monitoramento_n8n.model.StatusTeste;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Uma funcionalidade numa plataforma: item do GET /funcionalidades e resposta do PUT .../plataformas/{plataforma}.
 * Funcionalidade sem situação gravada vem como NAO_IMPLEMENTADO, com os demais campos vazios.
 */
public record SituacaoFuncionalidadeDTO(
        String plataforma,
        String plataformaNome,
        Long funcionalidadeId,
        String modulo,
        String nome,
        String descricao,
        int ordem,
        StatusFuncionalidade status,
        StatusTeste teste,
        LocalDateTime testadoEm,
        String testadoPor,
        String observacao,
        String configuracaoErp,
        String workflow,
        String responsavel,
        Prioridade prioridade,
        LocalDate previsao,
        String requisitoAltera,
        String alteradoPor,
        LocalDateTime alteradoEm
) {

    public static SituacaoFuncionalidadeDTO de(SituacaoEfetiva s) {
        return new SituacaoFuncionalidadeDTO(s.getPlataforma(), s.getPlataformaNome(), s.getFuncionalidadeId(),
                s.getModulo(), s.getNome(), s.getDescricao(), s.getOrdem(), s.getStatus(), s.getTeste(),
                s.getTestadoEm(), s.getTestadoPor(), s.getObservacao(), s.getConfiguracaoErp(), s.getWorkflow(),
                s.getResponsavel(), s.getPrioridade(), s.getPrevisao(), s.getRequisitoAltera(), s.getAlteradoPor(),
                s.getAlteradoEm());
    }

    /** Monta a resposta do PUT direto das entidades gravadas, sem reler a view (que o Hibernate não sabe se precisa de flush). */
    public static SituacaoFuncionalidadeDTO de(Funcionalidade f, SituacaoFuncionalidade s, String plataformaNome) {
        return new SituacaoFuncionalidadeDTO(s.getPlataforma(), plataformaNome, f.getId(),
                f.getModulo(), f.getNome(), f.getDescricao(), f.getOrdem(), s.getStatus(), s.getTeste(),
                s.getDataTeste(), s.getUsuarioTeste(), s.getObservacao(), s.getConfiguracaoErp(), s.getWorkflow(),
                s.getResponsavel(), s.getPrioridade(), s.getPrevisao(), s.getRequisitoAltera(),
                s.getUsuarioAlteracao(), s.getDataAlteracao());
    }
}
