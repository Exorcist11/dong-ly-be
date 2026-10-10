package com.dongly.modules.user.repository;

import com.dongly.modules.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByCodeIn(Collection<String> codes);

    List<Permission> findAllByModule(String module);

    List<Permission> findAllByModuleOrderByCodeAsc(String module);

    List<Permission> findAllByOrderByModuleAscCodeAsc();

    @Query("SELECT DISTINCT p.module FROM Permission p ORDER BY p.module ASC")
    List<String> findDistinctModules();

    boolean existsByCode(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
