package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.SituacaoEfetiva;
import com.citel.monitoramento_n8n.model.StatusFuncionalidade;
import com.citel.monitoramento_n8n.model.StatusTeste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SituacaoEfetivaRepository extends JpaRepository<SituacaoEfetiva, SituacaoEfetiva.Chave> {

    /**
     * Todos os filtros são opcionais e se somam. {@code busca} e {@code responsavel} chegam como padrão LIKE já em
     * minúsculas e com '!' de escape (ver FuncionalidadeService); {@code modulo} é o nome exato.
     */
    @Query("""
        SELECT s FROM SituacaoEfetiva s
        WHERE (:plataforma IS NULL OR s.plataforma = :plataforma)
          AND (:modulo IS NULL OR s.modulo = :modulo)
          AND (:status IS NULL OR s.status = :status)
          AND (:teste IS NULL OR s.teste = :teste)
          AND (:responsavel IS NULL OR LOWER(s.responsavel) LIKE :responsavel ESCAPE '!')
          AND (:busca IS NULL OR LOWER(s.nome) LIKE :busca ESCAPE '!'
                              OR LOWER(s.descricao) LIKE :busca ESCAPE '!'
                              OR LOWER(s.observacao) LIKE :busca ESCAPE '!')
        ORDER BY s.plataforma, s.ordem, s.nome
        """)
    List<SituacaoEfetiva> buscarPorFiltro(@Param("plataforma") String plataforma,
                                          @Param("modulo") String modulo,
                                          @Param("status") StatusFuncionalidade status,
                                          @Param("teste") StatusTeste teste,
                                          @Param("responsavel") String responsavel,
                                          @Param("busca") String busca);
}
