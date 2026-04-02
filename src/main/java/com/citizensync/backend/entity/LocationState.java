package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "location_states")
@Data
public class LocationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, unique = true)
    private String name;
}
