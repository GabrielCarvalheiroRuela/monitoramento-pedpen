package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.CoberturaPlataformaDTO;
import com.citel.monitoramento_n8n.DTO.DadosCriacaoFuncionalidade;
import com.citel.monitoramento_n8n.DTO.DadosSituacaoFuncionalidade;
import com.citel.monitoramento_n8n.DTO.EdicaoFuncionalidadeDTO;
import com.citel.monitoramento_n8n.DTO.FuncionalidadeDTO;
import com.citel.monitoramento_n8n.DTO.SituacaoFuncionalidadeDTO;
import com.citel.monitoramento_n8n.model.StatusFuncionalidade;
import com.citel.monitoramento_n8n.model.StatusTeste;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.service.FuncionalidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/funcionalidades")
@Tag(name = "Funcionalidades", description = "Matriz de funcionalidades das plataformas de integração (CADFUN e PLAFUN)")
public class FuncionalidadeController {

    private final FuncionalidadeService service;

    public FuncionalidadeController(FuncionalidadeService service) {
        this.service = service;
    }

    @Operation(summary = "Lista as funcionalidades com a situação em cada plataforma",
            description = """
                    Uma linha por funcionalidade x plataforma cadastrada. Funcionalidade sem situação \
                    gravada vem como NAO_IMPLEMENTADO. Todos os filtros são opcionais e se somam. \
                    Os perfis DEV e ADM podem consultar; o USR recebe 403.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de situações",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SituacaoFuncionalidadeDTO.class)))}),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping
    public ResponseEntity<List<SituacaoFuncionalidadeDTO>> listar(
            @Parameter(description = "Identificador da plataforma, ex.: tray") @RequestParam(required = false) String plataforma,
            @Parameter(description = "Nome exato do módulo") @RequestParam(required = false) String modulo,
            @RequestParam(required = false) StatusFuncionalidade status,
            @Parameter(description = "Situação do teste") @RequestParam(required = false) StatusTeste teste,
            @Parameter(description = "Trecho do nome do responsável") @RequestParam(required = false) String responsavel,
            @Parameter(description = "Trecho do nome, da descrição ou da observação") @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(service.listar(plataforma, modulo, status, teste, responsavel, busca));
    }

    @Operation(summary = "Cobertura por plataforma e por módulo",
            description = """
                    (implementadas + 0,5 x parciais) / (total - não se aplica), em %. O percentual vem nulo \
                    quando nada se aplica. Todas as plataformas cadastradas aparecem, assim que existir ao menos uma funcionalidade.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobertura",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CoberturaPlataformaDTO.class)))}),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/cobertura")
    public ResponseEntity<List<CoberturaPlataformaDTO>> cobertura() {
        return ResponseEntity.ok(service.cobertura());
    }

    @Operation(summary = "Cadastra uma funcionalidade na lista (somente ADM)",
            description = "A lista é única e vale para todas as plataformas. A situação em cada uma vem depois, no PUT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Funcionalidade criada",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = FuncionalidadeDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno ADM"),
            @ApiResponse(responseCode = "409", description = "Já existe essa funcionalidade nesse módulo"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping
    public ResponseEntity<FuncionalidadeDTO> criar(@RequestBody @Valid DadosCriacaoFuncionalidade dados,
                                                   @AuthenticationPrincipal Usuario usuario,
                                                   UriComponentsBuilder uriBuilder) {
        FuncionalidadeDTO criada = service.criar(dados, usuario);
        URI uri = uriBuilder.path("/funcionalidades/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @Operation(summary = "Edita módulo, nome, descrição e ordem de uma funcionalidade (somente ADM)",
            description = """
                    Campo ausente ou nulo preserva o valor atual. Cada campo que realmente mudou gera uma \
                    linha no LOGFUN; se nada mudar, nada é gravado.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Funcionalidade com os valores atuais",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = FuncionalidadeDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno ADM"),
            @ApiResponse(responseCode = "404", description = "Funcionalidade inexistente"),
            @ApiResponse(responseCode = "409", description = "Já existe essa funcionalidade nesse módulo"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<FuncionalidadeDTO> editar(@PathVariable Long id,
                                                    @RequestBody @Valid EdicaoFuncionalidadeDTO dados,
                                                    @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.editar(id, dados, usuario));
    }

    @Operation(summary = "Exclui uma funcionalidade de todas as plataformas (somente ADM)",
            description = """
                    Apaga a funcionalidade e a situação dela em todas as plataformas, e a cobertura se atualiza. \
                    Não dá para desfazer; o histórico (LOGFUN) guarda o nome e quem excluiu. A confirmação é \
                    pedida pela tela.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Funcionalidade excluída"),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno ADM"),
            @ApiResponse(responseCode = "404", description = "Funcionalidade inexistente"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        service.excluir(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Define a situação de uma funcionalidade numa plataforma (DEV e ADM)",
            description = """
                    Substitui a situação inteira: campo ausente é limpo, e só o status é obrigatório. Cria a \
                    linha se ainda não existe. Ao mudar o teste para TESTADO ou COM_FALHA o servidor grava a \
                    data e quem testou. Cada campo que mudou gera uma linha no LOGFUN.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Situação gravada",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = SituacaoFuncionalidadeDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno DEV ou ADM"),
            @ApiResponse(responseCode = "404", description = "Funcionalidade ou plataforma inexistente"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PutMapping("/{id}/plataformas/{plataforma}")
    public ResponseEntity<SituacaoFuncionalidadeDTO> definirSituacao(
            @PathVariable Long id,
            @Parameter(description = "PLA_SISEXT da plataforma, ex.: tray") @PathVariable String plataforma,
            @RequestBody @Valid DadosSituacaoFuncionalidade dados,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.definirSituacao(id, plataforma, dados, usuario));
    }
}
