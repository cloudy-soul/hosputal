package com.hospital.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LOINC Laboratory Observation Identifier Service.
 *
 * <p>LOINC (Logical Observation Identifiers Names and Codes) is the
 * international standard for identifying medical laboratory observations.
 * The Doctor department uses LOINC codes when ordering lab tests.</p>
 *
 * <p>Future integration: the IT and Logistics departments can wire this
 * to a HL7 FHIR DiagnosticReport resource for lab result transmission.</p>
 */
@Service
public class LoincCodeService {

    private static final Map<String, String> LOINC_CODES = new LinkedHashMap<>();

    static {
        // Haematology
        LOINC_CODES.put("789-8",   "Erythrocytes [#/volume] in Blood by Automated count");
        LOINC_CODES.put("718-7",   "Hemoglobin [Mass/volume] in Blood");
        LOINC_CODES.put("4548-4",  "Hemoglobin A1c/Hemoglobin.total in Blood");
        LOINC_CODES.put("6690-2",  "Leukocytes [#/volume] in Blood by Automated count");
        LOINC_CODES.put("777-3",   "Platelets [#/volume] in Blood by Automated count");

        // Lipid panel
        LOINC_CODES.put("2085-9",  "Cholesterol in HDL [Mass/volume] in Serum or Plasma");
        LOINC_CODES.put("18262-6", "Cholesterol in LDL [Mass/volume] in Serum or Plasma");
        LOINC_CODES.put("2093-3",  "Cholesterol [Mass/volume] in Serum or Plasma");
        LOINC_CODES.put("2571-8",  "Triglyceride [Mass/volume] in Serum or Plasma");

        // Renal / Metabolic
        LOINC_CODES.put("2160-0",  "Creatinine [Mass/volume] in Serum or Plasma");
        LOINC_CODES.put("3094-0",  "Urea nitrogen [Mass/volume] in Serum or Plasma");
        LOINC_CODES.put("2823-3",  "Potassium [Moles/volume] in Serum or Plasma");
        LOINC_CODES.put("2951-2",  "Sodium [Moles/volume] in Serum or Plasma");
        LOINC_CODES.put("2345-7",  "Glucose [Mass/volume] in Serum or Plasma");

        // Thyroid
        LOINC_CODES.put("3016-3",  "Thyrotropin [Units/volume] in Serum or Plasma");
        LOINC_CODES.put("3053-6",  "Thyroxine (T4) [Mass/volume] in Serum or Plasma");

        // Liver
        LOINC_CODES.put("1742-6",  "Alanine aminotransferase [Enzymatic activity/volume] in Serum or Plasma");
        LOINC_CODES.put("1920-8",  "Aspartate aminotransferase [Enzymatic activity/volume] in Serum or Plasma");

        // Urinalysis
        LOINC_CODES.put("5767-9",  "Appearance of Urine");
        LOINC_CODES.put("5778-6",  "Color of Urine");
        LOINC_CODES.put("2514-8",  "Ketones [Mass/volume] in Urine");

        // Coagulation
        LOINC_CODES.put("5902-2",  "Prothrombin time (PT) [Time] in Platelet poor plasma");
        LOINC_CODES.put("3173-2",  "aPTT in Blood by Coagulation assay");
    }

    public Optional<String> getDisplayName(String code) {
        return Optional.ofNullable(LOINC_CODES.get(code));
    }

    public Map<String, String> searchByTerm(String term) {
        if (term == null || term.isBlank()) return Collections.emptyMap();
        String lower = term.toLowerCase();
        return LOINC_CODES.entrySet().stream()
                .filter(e -> e.getValue().toLowerCase().contains(lower))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public boolean isValidCode(String code) {
        return LOINC_CODES.containsKey(code);
    }

    public Map<String, String> getAllCodes() {
        return Collections.unmodifiableMap(LOINC_CODES);
    }
}
