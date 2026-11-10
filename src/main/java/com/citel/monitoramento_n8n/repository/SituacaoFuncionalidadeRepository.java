package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.SituacaoFuncionalidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SituacaoFuncionalidadeRepository extends JpaRepository<SituacaoFuncionalidade, Long> {

    Optional<SituacaoFuncionalidade> findByCodigoFuncionalidadeAndPlataforma(Long codigoFuncionalidade, String plataforma);
}
