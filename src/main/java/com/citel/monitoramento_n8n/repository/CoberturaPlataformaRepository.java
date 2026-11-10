package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.CoberturaPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoberturaPlataformaRepository extends JpaRepository<CoberturaPlataforma, String> {

    List<CoberturaPlataforma> findAllByOrderByPlataformaNomeAsc();
}
