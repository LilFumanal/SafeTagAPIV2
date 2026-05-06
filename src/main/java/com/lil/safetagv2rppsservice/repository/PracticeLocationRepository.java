package com.lil.safetagv2rppsservice.repository;

import com.lil.safetagv2rppsservice.entity.PracticeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PracticeLocationRepository extends JpaRepository<PracticeLocation, UUID> {

    // Récupère les 50 premières adresses qui n'ont pas encore été testées
    List<PracticeLocation> findTop50ByGeocodingAttemptedFalse();
}
