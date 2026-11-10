package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.CoberturaModulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoberturaModuloRepository extends JpaRepository<CoberturaModulo, CoberturaModulo.Chave> {

    List<CoberturaModulo> findAllByOrderByPlataformaAscOrdemAsc();
}
