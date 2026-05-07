package com.hospital.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * ICD-11 Mapping Service.
 *
 * <p>Maps SNOMED CT clinical codes to ICD-11 billing codes.
 * ICD-11 (International Classification of Diseases, 11th Revision)
 * is the WHO standard used by the Finance Department for patient billing
 * and by hospitals for disease reporting to health authorities.</p>
 *
 * <p>In production this mapping is maintained by a certified clinical
 * informaticist and ideally retrieved from the WHO ICD-11 API.
 * The Finance Department will consume the stored ICD-11 code from
 * the Appointment entity to generate invoices.</p>
 */
@Service
public class Icd11MappingService {

    /** SNOMED SCTID → { icd11Code, icd11Display } */
    private static final Map<String, String[]> MAPPING = Map.ofEntries(
        // Cardiovascular
        Map.entry("38341003",  new String[]{"BA00",  "Essential hypertension"}),
        Map.entry("22298006",  new String[]{"BA41",  "Acute myocardial infarction"}),
        Map.entry("84114007",  new String[]{"BD10",  "Heart failure"}),
        Map.entry("49436004",  new String[]{"BC81.1","Atrial fibrillation"}),

        // Endocrine
        Map.entry("44054006",  new String[]{"5A11",  "Type 2 diabetes mellitus"}),
        Map.entry("73211009",  new String[]{"5A10",  "Type 1 diabetes mellitus"}),
        Map.entry("14140009",  new String[]{"5A00",  "Hyperthyroidism"}),
        Map.entry("40930008",  new String[]{"5A00.1","Hypothyroidism"}),

        // Respiratory
        Map.entry("195967001", new String[]{"CA23",  "Asthma"}),
        Map.entry("13645005",  new String[]{"CA22",  "Chronic obstructive pulmonary disease"}),
        Map.entry("233604007", new String[]{"CA40",  "Pneumonia"}),
        Map.entry("230572002", new String[]{"CA20.0","Acute bronchitis"}),

        // Infectious / Other
        Map.entry("162864005", new String[]{"GC08",  "Urinary tract infection"}),
        Map.entry("235856003", new String[]{"CA05",  "Acute tonsillitis"}),
        Map.entry("57345002",  new String[]{"DA91",  "Gastroenteritis"}),

        // Musculoskeletal
        Map.entry("396275006", new String[]{"FA00",  "Osteoarthritis"}),
        Map.entry("69896004",  new String[]{"FA20",  "Rheumatoid arthritis"}),

        // Neurological
        Map.entry("230690007", new String[]{"8B20",  "Ischaemic stroke"}),
        Map.entry("84757009",  new String[]{"8A60",  "Epilepsy"}),
        Map.entry("37796009",  new String[]{"8A80.0","Migraine"}),

        // Mental health
        Map.entry("35489007",  new String[]{"6A70",  "Single episode depressive disorder"}),
        Map.entry("197480006", new String[]{"6B00",  "Generalised anxiety disorder"})
    );

    /**
     * Returns the ICD-11 code for a given SNOMED CT code.
     * Returns empty if no mapping exists (un-mapped codes should be
     * handled by the Finance Department with a manual override).
     */
    public Optional<String[]> mapFromSnomed(String snomedCode) {
        return Optional.ofNullable(MAPPING.get(snomedCode));
    }

    public String getIcd11Code(String snomedCode) {
        String[] entry = MAPPING.get(snomedCode);
        return entry != null ? entry[0] : "UNCLASSIFIED";
    }

    public String getIcd11Display(String snomedCode) {
        String[] entry = MAPPING.get(snomedCode);
        return entry != null ? entry[1] : "Unclassified diagnosis – manual review required";
    }
}
