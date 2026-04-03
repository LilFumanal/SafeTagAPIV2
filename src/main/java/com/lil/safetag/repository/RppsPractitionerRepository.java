package com.lil.safetag.repository;

import com.lil.safetag.entity.RppsPractitioner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RppsPractitionerRepository extends JpaRepository<RppsPractitioner, Long> {
}