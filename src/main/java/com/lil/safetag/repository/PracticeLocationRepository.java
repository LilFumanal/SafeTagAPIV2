package com.lil.safetag.repository;

import com.lil.safetag.entity.PracticeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeLocationRepository extends JpaRepository<PracticeLocation, Long> {

    // Récupère les 50 premières adresses qui n'ont pas encore été testées
    List<PracticeLocation> findTop50ByGeocodingAttemptedFalse();
}
