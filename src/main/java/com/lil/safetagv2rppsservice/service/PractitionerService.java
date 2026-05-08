package com.lil.safetagv2rppsservice.service;

import com.lil.safetagv2rppsservice.client.RppsAPIClient;
import com.lil.safetagv2rppsservice.dto.AddressDTO; // Import du DTO d'adresse
import com.lil.safetagv2rppsservice.dto.PractitionerDTO;
import com.lil.safetagv2rppsservice.entity.RppsPractitioner;
import com.lil.safetagv2rppsservice.repository.RppsPractitionerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional(readOnly = true)
    public Page<PractitionerDTO> searchPractitioners(String name, String professionCode, String specialtyCode, String city, Double latitude, Double longitude, Double radius, Pageable pageable) {
        // Préparation des paramètres
        String nameParam = (name != null && !name.isBlank()) ? "%" + name.toLowerCase() + "%" : null;
        String cityParam = (city != null && !city.isBlank()) ? "%" + city.toLowerCase() + "%" : null;

        // Gestion du contournement du bug Hibernate/PostgreSQL sur les nulls
        boolean useGeo = (latitude != null && longitude != null && radius != null);
        Double safeLat = useGeo ? latitude : 0.0;
        Double safeLon = useGeo ? longitude : 0.0;
        Double safeRadius = useGeo ? radius : 0.0;

        Page<RppsPractitioner> practitionersPage = repository.searchPractitioners(
                nameParam, professionCode, specialtyCode, cityParam,
                useGeo, safeLat, safeLon, safeRadius, pageable
        );

        return practitionersPage.map(this::mapEntityToDTO);
    }

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
        }
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
     * @param entity L'entité RppsPractitioner à convertir.
     * @return Le PractitionerDTO correspondant.
     */
    private PractitionerDTO mapEntityToDTO(RppsPractitioner entity) {
        if (entity == null) {
            return null; // Retourne null si l'entité est nulle
        }

        // 1. Mapping des adresses :
        List<AddressDTO> addresses = entity.getLocations().stream()
                .map(location -> new AddressDTO(location.getFacilityName(), location.getStreetNumber(), location.getStreet(), location.getZipCode(), location.getCity(), location.getLatitude(), location.getLongitude())) // Crée un AddressDTO pour chaque location
                .collect(Collectors.toList()); // Collecte les DTO d'adresse dans une liste

        // 2. Préparation des professions et spécialités en List<String>
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

    @Cacheable(value = "professions")
    public List<String> getAllProfessions() {
        return repository.findDistinctProfessionLabels();
    }
}