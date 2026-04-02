package com.citizensync.backend.repository;

import com.citizensync.backend.entity.LocationPanchayat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationPanchayatRepository extends JpaRepository<LocationPanchayat, Long> {
    List<LocationPanchayat> findBySubdistrictIdOrderByNameAsc(Long subdistrictId);
}
