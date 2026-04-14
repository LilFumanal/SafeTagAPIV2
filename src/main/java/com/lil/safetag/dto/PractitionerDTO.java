package com.lil.safetag.dto;

import java.util.List;

public record PractitionerDTO(
        String id,
        String name,
        List<String> professions,
        List<String> specialties,
        List<AddressDTO> practiceAddresses,
        String modeExercice
) {}