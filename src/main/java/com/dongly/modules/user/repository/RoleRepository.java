package com.dongly.modules.user.repository;

import com.dongly.modules.user.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByCode(String code);

    List<Role> findAllByCodeIn(Collection<String> codes);

    boolean existsByCode(String code);
}
