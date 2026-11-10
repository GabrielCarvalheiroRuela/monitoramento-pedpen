package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Prioridade;
import com.citel.monitoramento_n8n.model.StatusFuncionalidade;
import com.citel.monitoramento_n8n.model.StatusTeste;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request do PUT /funcionalidades/{id}/plataformas/{plataforma}: a situação inteira da funcionalidade naquela
 * plataforma. É uma substituição, não um merge: campo ausente ou nulo limpa o valor (o status é o único
 * obrigatório; o teste, se ausente, volta a NAO_TESTADO).
 */
@Schema(description = "Substitui a situação inteira. Campos ausentes são limpos; só o status é obrigatório.")
public record DadosSituacaoFuncionalidade(

        @NotNull(message = "status é obrigatório")
        StatusFuncionalidade status,

        @Schema(description = "Ausente = NAO_TESTADO. Ao mudar para TESTADO ou COM_FALHA, o servidor grava a data e quem testou.")
        StatusTeste teste,

        @Schema(description = "Observação / regra desta plataforma.")
        String observacao,

        @Schema(example = "CFG_CONKEY, CFG_SECRET", description = "Parâmetros do ERP envolvidos.")
        String configuracaoErp,

        @Schema(example = "SUB_Tray_Token_Manager", description = "Workflow(s) do n8n que implementam.")
        String workflow,

        @Size(max = 255, message = "responsavel deve ter no máximo 255 caracteres")
        String responsavel,

        @Schema(description = "Prioridade do que ainda falta fazer.")
        Prioridade prioridade,

        @Schema(description = "Previsão de entrega.")
        LocalDate previsao,

        @Schema(example = "474225", description = "Nº do requisito no Altera que trata desta funcionalidade.")
        @Size(max = 20, message = "requisitoAltera deve ter no máximo 20 caracteres")
        String requisitoAltera
) {
}
