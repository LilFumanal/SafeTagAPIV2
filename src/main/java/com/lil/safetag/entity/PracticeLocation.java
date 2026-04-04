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
    private String street;        // Index 24
    private String zipCode;      // Index 35
    private String city;         // Index 37

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_rpps_id", referencedColumnName = "rppsId")
    @JsonIgnore
    private RppsPractitioner practitioner;
}
