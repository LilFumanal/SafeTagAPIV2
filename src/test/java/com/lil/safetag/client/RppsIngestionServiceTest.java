package com.lil.safetag.client;

import com.lil.safetag.repository.RppsPractitionerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RppsIngestionServiceTest {

    @Mock
    private RppsPractitionerRepository repository;

    @Mock
    private CSVClient csvClient;

    @InjectMocks
    private RppsIngestionService ingestionService;

    @Test
    void shouldImportValidPractitionersAndSkipInvalid() throws Exception {
        // GIVEN: Un faux CSV en mémoire avec 1 entête, 1 médecin valide (10/SM04), 1 métier ignoré (11)
        String fakeCsv = "Header...\n" +
                // Ligne 1 : Valide (10 - SM04) - On remplit 55 colonnes avec des '|'
                "|||||||DUPONT|Jean|10||||||SM04|||Libéral|||||||||||||||||||||||||||||||||||||\n" +
                // Ligne 2 : Invalide (Profession 11 non gérée)
                "|||||||MARTIN|Paul|11||||||SM04|||||||||||||||||||||||||||||||||||||||||\n";

        InputStream fakeStream = new ByteArrayInputStream(fakeCsv.getBytes(StandardCharsets.UTF_8));
        when(csvClient.downloadRppsFile()).thenReturn(fakeStream);

        // WHEN
        ingestionService.importRppsData();

        // THEN
        // On vérifie que saveAll a été appelé 1 fois (pour la ligne valide)
        verify(repository, times(1)).saveAll(anyCollection());
    }
}
