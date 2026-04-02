package com.citizensync.backend.repository;

import com.citizensync.backend.entity.LocationDistrict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationDistrictRepository extends JpaRepository<LocationDistrict, Long> {
    List<LocationDistrict> findByStateIdOrderByNameAsc(Long stateId);
}
