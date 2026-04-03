package com.lil.safetag.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rpps_practitioners")
@Getter
@Setter
@NoArgsConstructor
public class RppsPractitioner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String rppsId;

    private String lastName;
    private String firstName;
    private String professionCode;
    private String specialtyCode;

    // Date de dernière mise à jour pour savoir si la donnée est fraîche
    private LocalDateTime lastUpdated;
}
