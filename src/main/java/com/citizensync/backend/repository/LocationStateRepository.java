package com.citizensync.backend.repository;

import com.citizensync.backend.entity.LocationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationStateRepository extends JpaRepository<LocationState, Long> {
	List<LocationState> findAllByOrderByNameAsc();
}
