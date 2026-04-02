package com.citizensync.backend.repository;

import com.citizensync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAadhar(String aadhar);
    boolean existsByEmail(String email);
    boolean existsByAadhar(String aadhar);
    boolean existsByRole(String role);
    boolean existsByRoleAndSelectedVillage(String role, String selectedVillage);
    boolean existsByRoleAndPanchayatId(String role, Long panchayatId);
    boolean existsByRoleAndSubdistrictId(String role, Long subdistrictId);
    List<User> findByRole(String role);
}