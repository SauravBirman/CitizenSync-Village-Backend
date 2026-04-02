package com.citizensync.backend.repository;

import com.citizensync.backend.entity.LocationSubdistrict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationSubdistrictRepository extends JpaRepository<LocationSubdistrict, Long> {
    List<LocationSubdistrict> findByDistrictIdOrderByNameAsc(Long districtId);
}
