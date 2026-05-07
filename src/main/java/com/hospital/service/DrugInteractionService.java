package com.hospital.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Drug Interaction Checking Service.
 *
 * <p>Checks for clinically significant drug–drug interactions (DDIs).
 * The pharmacist calls this service during prescription verification
 * before dispensing.</p>
 *
 * <p>Severity levels:</p>
 * <ul>
 *   <li>CONTRAINDICATED – do not dispense, contact prescribing doctor</li>
 *   <li>MAJOR          – serious risk, consult before dispensing</li>
 *   <li>MODERATE       – monitor closely, inform patient</li>
 * </ul>
 *
 * <p>In production, integrate with a commercial database such as
 * Lexicomp, Micromedex, or the NLM Drug Interaction API.</p>
 */
@Service
public class DrugInteractionService {

    /** Immutable interaction record. */
    public record Interaction(
            String drug1,
            String drug2,
            String severity,    // CONTRAINDICATED | MAJOR | MODERATE
            String description,
            String clinicalEffect
    ) {}

    /** Interaction registry – keyed by normalised pair "DRUG_A::DRUG_B". */
    private static final List<Interaction> INTERACTIONS = List.of(

        new Interaction("warfarin", "aspirin",
            "MAJOR",
            "Warfarin + Aspirin",
            "Increased bleeding risk. Both agents inhibit haemostasis through different mechanisms. " +
            "Combination significantly raises gastrointestinal and intracranial haemorrhage risk."),

        new Interaction("lisinopril", "spironolactone",
            "MAJOR",
            "Lisinopril (ACE inhibitor) + Spironolactone (K⁺-sparing diuretic)",
            "Hyperkalaemia risk. Both agents raise serum potassium. " +
            "Monitor electrolytes closely; avoid in renal impairment."),

        new Interaction("metformin", "iodinated contrast",
            "MAJOR",
            "Metformin + Iodinated contrast media",
            "Risk of contrast-induced nephropathy leading to metformin accumulation and lactic acidosis. " +
            "Withhold metformin 48 h before/after contrast procedures."),

        new Interaction("simvastatin", "amiodarone",
            "MAJOR",
            "Simvastatin + Amiodarone",
            "Amiodarone inhibits CYP3A4, raising simvastatin plasma levels. " +
            "Risk of myopathy and rhabdomyolysis. Limit simvastatin to 20 mg/day."),

        new Interaction("methotrexate", "nsaid",
            "MAJOR",
            "Methotrexate + NSAIDs",
            "NSAIDs reduce methotrexate renal clearance leading to toxic accumulation. " +
            "Risk of bone-marrow suppression, hepatotoxicity, nephrotoxicity."),

        new Interaction("ssri", "tramadol",
            "MAJOR",
            "SSRI antidepressants + Tramadol",
            "Serotonin syndrome risk. Both agents increase central serotonin activity. " +
            "Symptoms: agitation, hyperthermia, clonus, autonomic instability."),

        new Interaction("digoxin", "amiodarone",
            "MODERATE",
            "Digoxin + Amiodarone",
            "Amiodarone increases digoxin plasma concentration. " +
            "Monitor digoxin levels; reduce digoxin dose by 30–50%."),

        new Interaction("clopidogrel", "omeprazole",
            "MODERATE",
            "Clopidogrel + Omeprazole/PPIs",
            "Omeprazole inhibits CYP2C19 reducing conversion of clopidogrel to its active metabolite. " +
            "Reduced antiplatelet efficacy. Consider pantoprazole as alternative PPI.")
    );

    /**
     * Check if a new medication interacts with any of the patient's
     * existing active medications.
     *
     * @param newMedication         medication being prescribed / dispensed
     * @param existingMedications   list of active drug names for this patient
     * @return list of found interactions (empty = no interactions detected)
     */
    public List<Interaction> checkInteractions(String newMedication,
                                               List<String> existingMedications) {
        if (newMedication == null || existingMedications == null) return Collections.emptyList();

        String newLower = newMedication.toLowerCase();
        List<Interaction> found = new ArrayList<>();

        for (String existing : existingMedications) {
            String existLower = existing.toLowerCase();
            for (Interaction ix : INTERACTIONS) {
                boolean match = (newLower.contains(ix.drug1()) && existLower.contains(ix.drug2()))
                             || (newLower.contains(ix.drug2()) && existLower.contains(ix.drug1()));
                if (match) found.add(ix);
            }
        }
        return found;
    }

    /**
     * Quick check: returns true if ANY major or contraindicated interaction exists.
     */
    public boolean hasCriticalInteraction(String newMed, List<String> existing) {
        return checkInteractions(newMed, existing).stream()
                .anyMatch(i -> "MAJOR".equals(i.severity()) || "CONTRAINDICATED".equals(i.severity()));
    }
}
