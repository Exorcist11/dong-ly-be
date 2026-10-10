package com.dongly.modules.route.repository;

import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.StopPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StopPointRepository extends JpaRepository<StopPoint, UUID> {

    Optional<StopPoint> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @Query("""
        SELECT sp FROM StopPoint sp
        JOIN FETCH sp.location l
        WHERE (:locationId IS NULL OR l.id = :locationId)
          AND (:status IS NULL OR sp.status = :status)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(sp.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(sp.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(sp.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<StopPoint> searchStopPoints(
            @Param("keyword") String keyword,
            @Param("locationId") UUID locationId,
            @Param("status") CommonStatus status,
            Pageable pageable
    );
}
