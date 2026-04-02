package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "location_panchayats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_panchayat_subdistrict_code", columnNames = {"subdistrict_id", "code"})
})
@Data
public class LocationPanchayat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subdistrict_id")
    private LocationSubdistrict subdistrict;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;
}
