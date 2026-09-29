package com.ckgd.repository;

import com.ckgd.entity.EmpresaCandidato;
import com.ckgd.entity.EmpresaCandidatoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EmpresaCandidatoRepository extends JpaRepository<EmpresaCandidato, EmpresaCandidatoId> {
    @EntityGraph(attributePaths = "candidato")
    List<EmpresaCandidato> findByEmpresa_CnpjAndFavoritoTrue(String cnpj);
    @EntityGraph(attributePaths = "candidato")
    Optional<EmpresaCandidato> findByEmpresa_CnpjAndCandidato_NodeId(String cnpj, Long nodeId);
    long countByEmpresa_CnpjAndFavoritoTrue(String cnpj);

    @EntityGraph(attributePaths = "candidato")
    @Query("select ec from EmpresaCandidato ec where ec.empresa.cnpj = :cnpj and (ec.nota is not null or length(trim(ec.comentario)) > 0) order by ec.dataAvaliacao desc")
    List<EmpresaCandidato> listarAvaliacoes(@Param("cnpj") String cnpj);

    @Query("select count(ec) from EmpresaCandidato ec where ec.empresa.cnpj = :cnpj and (ec.nota is not null or length(trim(ec.comentario)) > 0)")
    long contarAvaliacoes(@Param("cnpj") String cnpj);
}
