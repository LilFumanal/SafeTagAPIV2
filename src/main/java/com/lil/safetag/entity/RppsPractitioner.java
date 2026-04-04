package com.lil.safetag.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "practitioner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PracticeLocation> locations = new ArrayList<>();

    // 2. La méthode utilitaire pour lier les deux côtés facilement
    public void addLocation(PracticeLocation location) {
        locations.add(location);
        location.setPractitioner(this);
    }
}
