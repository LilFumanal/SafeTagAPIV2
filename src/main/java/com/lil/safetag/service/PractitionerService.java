package com.lil.safetag.service;

import com.lil.safetag.client.RppsAPIClient;
import com.lil.safetag.dto.AddressDTO; // Import du DTO d'adresse
import com.lil.safetag.dto.PractitionerDTO;
import com.lil.safetag.entity.PracticeLocation; // Import de l'entité PracticeLocation
import com.lil.safetag.entity.RppsPractitioner;
import com.lil.safetag.repository.RppsPractitionerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors; // Nécessaire pour le stream

@Service
public class PractitionerService {

    private final RppsAPIClient rppsAPIClient;
    private final RppsPractitionerRepository repository;

    public PractitionerService(RppsAPIClient rppsAPIClient, RppsPractitionerRepository repository){
        this.rppsAPIClient = rppsAPIClient;
        this.repository = repository;
    }

    public Page<PractitionerDTO> searchPractitioners(String name, String professionCode, String specialtyCode, String city, Double latitude, Double longitude, Double radius, Pageable pageable) {
        // Préparation des paramètres (ajout des % et mise en minuscules)
        String nameParam = (name != null && !name.isBlank()) ? "%" + name.toLowerCase() + "%" : null;
        String cityParam = (city != null && !city.isBlank()) ? "%" + city.toLowerCase() + "%" : null;

        Page<RppsPractitioner> practitionersPage = repository.searchPractitioners(
                nameParam, professionCode, specialtyCode, cityParam,
                latitude, longitude, radius, pageable
        );
        return practitionersPage.map(this::mapEntityToDTO);
    }



    // 2. Consultation unitaire avec Fallback robuste
    public PractitionerDTO getAndUpdatePractitioner(String rppsId) {
        Optional<RppsPractitioner> localDataOpt = repository.findByRppsId(rppsId);
        RppsPractitioner finalPractitioner;

        try {
            // Essayer de récupérer les données fraîches de l'API
            Map<String, Object> apiData = rppsAPIClient.getPractitionerById(rppsId);
            System.out.println("REPONSE API BRUTE : " + apiData);
            // Mettre à jour l'entité en utilisant les données locales comme base et en appliquant les données de l'API
            finalPractitioner = updateFromApi(localDataOpt, apiData, rppsId);
        } catch (Exception e) {
            // Si l'API est indisponible, utiliser les données locales uniquement
            System.err.println("API indisponible. Fallback local pour : " + rppsId + ". Erreur: " + e.getMessage());
            finalPractitioner = localDataOpt.orElseThrow(() ->
                    new RuntimeException("Praticien introuvable localement et API e-santé indisponible.")
            );
            // Assurez-vous que le PractitionerDTO final contiendra les adresses locales même en cas de fallback
            // Ceci est géré par mapEntityToDTO qui prend en compte les adresses de l'entité.
        }

        // Mapper l'entité (mise à jour ou fallback) vers le DTO
        return mapEntityToDTO(finalPractitioner);
    }

    /**
     * Met à jour une entité RppsPractitioner existante ou en crée une nouvelle à partir des données de l'API.
     * Sauvegarde ensuite l'entité mise à jour.
     * @param localData Optional contenant l'entité locale si elle existe.
     * @param apiData Map contenant les données brutes de l'API.
     * @param rppsId L'identifiant RPPS du praticien.
     * @return L'entité RppsPractitioner mise à jour et sauvegardée.
     */
    private RppsPractitioner updateFromApi(Optional<RppsPractitioner> localData, Map<String, Object> apiData, String rppsId) {
        RppsPractitioner practitioner = localData.orElse(new RppsPractitioner()); // Crée une nouvelle entité si non trouvée localement

        practitioner.setRppsId(rppsId); // Définit l'ID RPPS
        practitioner.setName((String) apiData.get("name")); // Définit le nom

        // Traite les codes de profession : prend le premier s'il existe
        List<String> profCodes = (List<String>) apiData.get("professionCodes");
        if (profCodes != null && !profCodes.isEmpty()) {
            // Sauvegarde le premier code de profession comme une chaîne unique
            // Si vous avez besoin de stocker plusieurs codes, il faudrait adapter l'entité et ici.
            practitioner.setProfessionCode(profCodes.get(0));
        } else {
            practitioner.setProfessionCode(null); // Assure que le champ est nul si vide côté API
        }

        // Traite les codes de spécialité : prend le premier s'il existe
        List<String> specCodes = (List<String>) apiData.get("specialtyCodes");
        if (specCodes != null && !specCodes.isEmpty()) {
            // Sauvegarde le premier code de spécialité comme une chaîne unique
            practitioner.setSpecialtyCode(specCodes.get(0));
        } else {
            practitioner.setSpecialtyCode(null); // Assure que le champ est nul si vide côté API
        }

        // Marque la date de dernière mise à jour
        practitioner.setLastUpdated(LocalDateTime.now());

        // Sauvegarde l'entité mise à jour dans la base de données locale
        return repository.save(practitioner);
    }

    /**
     * Convertit une entité RppsPractitioner en PractitionerDTO.
     * Gère le mapping des adresses via l'entité PracticeLocation.
     * @param entity L'entité RppsPractitioner à convertir.
     * @return Le PractitionerDTO correspondant.
     */
    private PractitionerDTO mapEntityToDTO(RppsPractitioner entity) {
        if (entity == null) {
            return null; // Retourne null si l'entité est nulle
        }

        // 1. Mapping des adresses :
        // Récupère la liste des PracticeLocation associées à l'entité
        // Assurez-vous que l'entité RppsPractitioner a une méthode getLocations() qui retourne List<PracticeLocation>
        List<AddressDTO> addresses = entity.getLocations().stream()
                .map(location -> new AddressDTO(location.getFacilityName(), location.getStreetNumber(), location.getStreet(), location.getZipCode(), location.getCity(), location.getLatitude(), location.getLongitude())) // Crée un AddressDTO pour chaque location
                .collect(Collectors.toList()); // Collecte les DTO d'adresse dans une liste

        // 2. Préparation des professions et spécialités en List<String>
        // Si professionCode est non nul, le met dans une liste singleton, sinon une liste vide.
        List<String> professions = (entity.getProfessionCode() != null) ?
                Collections.singletonList(entity.getProfessionCode()) :
                Collections.emptyList();

        // Si specialtyCode est non nul, le met dans une liste singleton, sinon une liste vide.
        List<String> specialties = (entity.getSpecialtyCode() != null) ?
                Collections.singletonList(entity.getSpecialtyCode()) :
                Collections.emptyList();

        String displayModeExercice = entity.getExerciceMode(); // Assurez-vous que getExerciceMode() existe
        if (displayModeExercice == null || displayModeExercice.trim().isEmpty()) {
            displayModeExercice = "Non renseigné"; // Ou une autre valeur par défaut appropriée
        }

        // Création du PractitionerDTO final
        return new PractitionerDTO(
                entity.getRppsId(),
                entity.getName(),
                professions,
                specialties,
                addresses, // Utilisation de la liste d'AddressDTO mappée
                displayModeExercice
        );
    }
}