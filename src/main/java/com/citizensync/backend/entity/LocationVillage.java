package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "location_villages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_village_panchayat_code", columnNames = {"panchayat_id", "code"})
})
@Data
public class LocationVillage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "panchayat_id")
    private LocationPanchayat panchayat;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;
}
