package com.lil.safetagv2rppsservice.repository;

import com.lil.safetagv2rppsservice.entity.RppsPractitioner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RppsPractitionerRepository extends JpaRepository<RppsPractitioner, String> {

    Optional<RppsPractitioner> findByRppsId(String rppsId);

    @Query("SELECT DISTINCT p FROM RppsPractitioner p " +
            "LEFT JOIN p.locations l " +
            "WHERE (:name IS NULL OR LOWER(p.name) LIKE :name) " +
            "AND (:professionCode IS NULL OR p.professionCode = :professionCode) " +
            "AND (:specialtyCode IS NULL OR p.specialtyCode = :specialtyCode) " +
            "AND (:city IS NULL OR LOWER(l.city) LIKE :city) " +
            "AND (:useGeo = false OR " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) * " +
            "cos(radians(l.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :radius)")
    Page<RppsPractitioner> searchPractitioners(@Param("name") String name,
                                               @Param("professionCode") String professionCode,
                                               @Param("specialtyCode") String specialtyCode,
                                               @Param("city") String city,
                                               @Param("useGeo") boolean useGeo,
                                               @Param("latitude") Double latitude,
                                               @Param("longitude") Double longitude,
                                               @Param("radius") Double radius,
                                               Pageable pageable);
}
