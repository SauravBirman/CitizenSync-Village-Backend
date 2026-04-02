package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "location_districts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_district_state_code", columnNames = {"state_id", "code"})
})
@Data
public class LocationDistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private LocationState state;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;
}
