package com.lil.safetag.dto;

public record AddressDTO(
        String facilityName,
        String streetNumber, // Ajout du champ manquant (String pour supporter les "bis", "ter", etc.)
        String street,
        String zipCode,
        String city,
        Double latitude,
        Double longitude
) {}