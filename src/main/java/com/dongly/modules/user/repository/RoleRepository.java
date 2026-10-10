package com.dongly.modules.user.repository;

import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.RoleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID>, JpaSpecificationExecutor<Role> {

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByCode(String code);

    @EntityGraph(attributePaths = {"permissions"})
    @Query("SELECT r FROM Role r WHERE r.id = :id")
    Optional<Role> findWithPermissionsById(@Param("id") UUID id);

    List<Role> findAllByCodeIn(Collection<String> codes);

    List<Role> findAllByStatus(RoleStatus status);

    List<Role> findAllByIsSystemTrue();

    Page<Role> findAllByStatus(RoleStatus status, Pageable pageable);

    boolean existsByCode(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE r.id = :roleId")
    boolean isRoleAssignedToAnyUser(@Param("roleId") UUID roleId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId")
    long countUsersByRoleId(@Param("roleId") UUID roleId);
}
