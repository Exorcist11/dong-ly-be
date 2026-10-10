package com.dongly.modules.route.repository;

import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    Optional<Location> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Location> findAllByStatusOrderByProvinceAscNameAsc(CommonStatus status);
}
