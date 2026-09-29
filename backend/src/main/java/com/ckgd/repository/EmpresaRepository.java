package com.ckgd.repository;

import com.ckgd.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, String> {
    Optional<Empresa> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empresa e where e.cnpj = :cnpj")
    Optional<Empresa> findByCnpjForUpdate(@Param("cnpj") String cnpj);
}

