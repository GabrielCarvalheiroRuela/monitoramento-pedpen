package com.citel.monitoramento_n8n.exception;

/**
 * O usuário está autenticado, mas o perfil dele não permite esta operação. Resulta em 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String mensagem) {
        super(mensagem);
    }
}
