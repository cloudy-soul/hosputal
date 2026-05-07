package com.hospital.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SNOMED CT Terminology Service.
 *
 * <p>SNOMED CT (Systematized Nomenclature of Medicine – Clinical Terms)
 * is the international standard for clinical concept codes used in
 * diagnosis documentation, electronic health records, and interoperability.</p>
 *
 * <p>This service provides an in-memory lookup of the most common codes.
 * In a production system this would query a FHIR terminology server
 * (e.g. HAPI FHIR, Ontoserver) or the NHS SNOMED API.</p>
 *
 * <p>The Doctor department uses this to attach coded diagnoses to
 * appointments.  The Finance department uses the stored codes to
 * map to ICD-11 billing classifications via {@link Icd11MappingService}.</p>
 */
@Service
public class SnomedCodeService {

    /**
     * Core SNOMED CT concept map.
     * Key = SCTID (SNOMED Concept Identifier)
     * Value = Fully Specified Name (FSN)
     */
    private static final Map<String, String> SNOMED_CODES = new LinkedHashMap<>();

    static {
        // Cardiovascular
        SNOMED_CODES.put("38341003",  "Hypertension (disorder)");
        SNOMED_CODES.put("22298006",  "Myocardial infarction (disorder)");
        SNOMED_CODES.put("84114007",  "Heart failure (disorder)");
        SNOMED_CODES.put("49436004",  "Atrial fibrillation (disorder)");

        // Endocrine / Metabolic
        SNOMED_CODES.put("44054006",  "Diabetes mellitus type 2 (disorder)");
        SNOMED_CODES.put("73211009",  "Diabetes mellitus type 1 (disorder)");
        SNOMED_CODES.put("14140009",  "Hyperthyroidism (disorder)");
        SNOMED_CODES.put("40930008",  "Hypothyroidism (disorder)");

        // Respiratory
        SNOMED_CODES.put("195967001", "Asthma (disorder)");
        SNOMED_CODES.put("13645005",  "Chronic obstructive lung disease (disorder)");
        SNOMED_CODES.put("233604007", "Pneumonia (disorder)");
        SNOMED_CODES.put("230572002", "Acute bronchitis (disorder)");

        // Infectious
        SNOMED_CODES.put("162864005", "Urinary tract infection (disorder)");
        SNOMED_CODES.put("235856003", "Acute tonsillitis (disorder)");
        SNOMED_CODES.put("57345002",  "Gastroenteritis (disorder)");

        // Musculoskeletal
        SNOMED_CODES.put("396275006", "Osteoarthritis (disorder)");
        SNOMED_CODES.put("69896004",  "Rheumatoid arthritis (disorder)");

        // Neurological
        SNOMED_CODES.put("230690007", "Stroke (disorder)");
        SNOMED_CODES.put("84757009",  "Epilepsy (disorder)");
        SNOMED_CODES.put("37796009",  "Migraine (disorder)");

        // Mental health
        SNOMED_CODES.put("35489007",  "Depressive disorder (disorder)");
        SNOMED_CODES.put("197480006", "Anxiety disorder (disorder)");
    }

    /**
     * Look up a SNOMED code by its SCTID.
     *
     * @param code SNOMED Concept Identifier e.g. "44054006"
     * @return Optional containing the display name, or empty if not found
     */
    public Optional<String> getDisplayName(String code) {
        return Optional.ofNullable(SNOMED_CODES.get(code));
    }

    /**
     * Search SNOMED concepts by partial term match (case-insensitive).
     *
     * @param term Search string e.g. "diabetes"
     * @return Map of matching code → display name pairs
     */
    public Map<String, String> searchByTerm(String term) {
        if (term == null || term.isBlank()) return Collections.emptyMap();
        String lower = term.toLowerCase();
        return SNOMED_CODES.entrySet().stream()
                .filter(e -> e.getValue().toLowerCase().contains(lower))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Returns true if the SCTID exists in the terminology service.
     */
    public boolean isValidCode(String code) {
        return SNOMED_CODES.containsKey(code);
    }

    /** Returns all available codes (for admin / reference endpoint). */
    public Map<String, String> getAllCodes() {
        return Collections.unmodifiableMap(SNOMED_CODES);
    }
}
