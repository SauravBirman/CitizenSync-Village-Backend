package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "location_subdistricts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subdistrict_district_code", columnNames = {"district_id", "code"})
})
@Data
public class LocationSubdistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private LocationDistrict district;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;
}
