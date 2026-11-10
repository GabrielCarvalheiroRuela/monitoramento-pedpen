package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Funcionalidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FuncionalidadeRepository extends JpaRepository<Funcionalidade, Long> {

    boolean existsByModuloAndNome(String modulo, String nome);

    boolean existsByModuloAndNomeAndIdNot(String modulo, String nome, Long id);

    /** Maior ordem da lista inteira; a funcionalidade nova entra depois dela. */
    @Query("SELECT COALESCE(MAX(f.ordem), 0) FROM Funcionalidade f")
    int maiorOrdem();
}
