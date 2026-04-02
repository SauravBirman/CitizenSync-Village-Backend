package com.citizensync.backend.repository;

import com.citizensync.backend.entity.LocationVillage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationVillageRepository extends JpaRepository<LocationVillage, Long> {
    List<LocationVillage> findByPanchayatIdOrderByNameAsc(Long panchayatId);
}
