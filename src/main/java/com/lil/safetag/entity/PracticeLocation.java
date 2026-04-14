package com.lil.safetag.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "practice_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String facilityName;
    private String streetNumber; // Ajout du champ manquant (String pour supporter les "bis", "ter", etc.)
    private String street;
    private String zipCode;
    private String city;
    private Double latitude;
    private Double longitude;

    // Statut de traitement pour éviter les boucles infinies sur les adresses introuvables
    @Column(nullable = false)
    private boolean geocodingAttempted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id")
    private RppsPractitioner rppsPractitioner;

}
