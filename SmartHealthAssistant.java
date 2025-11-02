import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class SmartHealthAssistant {

    // ======== Class 1: Symptom ========
    static class Symptom {
        private String name;
        private int severity;

        public Symptom(String name) {
            this(name, 0);
        }

        public Symptom(String name, int severity) {
            this.name = name.trim().toLowerCase();
            this.severity = Math.max(0, Math.min(10, severity));
        }

        public String getName() {
            return name;
        }

        public int getSeverity() {
            return severity;
        }

        @Override
        public String toString() {
            return name + (severity > 0 ? " (sev:" + severity + ")" : "");
        }
    }

    // ======== Class 2: Disease ========
    static class Disease {
        protected String name;
        protected Set<String> expectedSymptoms;

        public Disease(String name, Collection<String> symptoms) {
            this.name = name.trim();
            this.expectedSymptoms = new HashSet<>();
            for (String s : symptoms) {
                if (s != null && !s.trim().isEmpty()) {
                    expectedSymptoms.add(s.trim().toLowerCase());
                }
            }
        }

        public String getName() {
            return name;
        }

        public double matchScore(Collection<String> reportedSymptoms) {
            if (expectedSymptoms.isEmpty()) return 0.0;
            int matched = 0;
            for (String rs : reportedSymptoms) {
                if (rs != null && expectedSymptoms.contains(rs.trim().toLowerCase())) {
                    matched++;
                }
            }
            return (double) matched / expectedSymptoms.size();
        }

        @Override
        public String toString() {
            return name + " -> " + expectedSymptoms;
        }
    }

    // ======== Class 3: ChronicDisease (Inheritance) ========
    static class ChronicDisease extends Disease {
        private boolean chronic = true;
        private int typicalDurationMonths;

        public ChronicDisease(String name, Collection<String> symptoms, int typicalDurationMonths) {
            super(name, symptoms);
            this.typicalDurationMonths = Math.max(0, typicalDurationMonths);
        }

        public boolean isChronic() {
            return chronic;
        }

        @Override
        public String toString() {
            return super.toString() + " [chronic=" + chronic + ", durationMonths=" + typicalDurationMonths + "]";
        }
    }

    // ======== Class 4: Patient ========
    static class Patient {
        private String name;
        private int age;
        private Set<String> reportedSymptoms;

        public Patient(String name, int age) {
            this.name = name;
            this.age = age;
            this.reportedSymptoms = new HashSet<>();
        }

        public void addSymptom(String symptom) {
            if (symptom != null && !symptom.trim().isEmpty()) {
                reportedSymptoms.add(symptom.trim().toLowerCase());
            }
        }

        public Set<String> getReportedSymptoms() {
            return reportedSymptoms;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    // ======== Class 5: Diagnosis ========
    static class Diagnosis implements Comparable<Diagnosis> {
        private Disease disease;
        private double score;

        public Diagnosis(Disease disease, double score) {
            this.disease = disease;
            this.score = score;
        }

        public Disease getDisease() {
            return disease;
        }

        public double getScore() {
            return score;
        }

        @Override
        public int compareTo(Diagnosis o) {
            return Double.compare(o.score, this.score); // descending
        }

        @Override
        public String toString() {
            return String.format("%s (score: %.2f)", disease.getName(), score);
        }
    }

    // ======== Class 6: DiagnosisEngine (Object Communication) ========
    static class DiagnosisEngine {
        private List<Disease> diseases = new ArrayList<>();

        public void loadFromCSV(String path) throws IOException {
            List<String> lines = Files.readAllLines(Paths.get(path));
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length < 2) continue;

                String diseaseName = parts[0].trim();
                String[] symptoms = parts[1].split("[;|,]");
                List<String> symptomList = Arrays.stream(symptoms)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                if (diseaseName.toLowerCase().startsWith("chronic:")) {
                    diseases.add(new ChronicDisease(diseaseName.substring(8).trim(), symptomList, 12));
                } else {
                    diseases.add(new Disease(diseaseName, symptomList));
                }
            }
        }

        public List<Diagnosis> diagnose(Patient patient, int topN) {
            List<Diagnosis> results = new ArrayList<>();
            for (Disease d : diseases) {
                double score = d.matchScore(patient.getReportedSymptoms());
                if (score > 0) results.add(new Diagnosis(d, score));
            }
            Collections.sort(results);
            if (results.size() > topN) return results.subList(0, topN);
            return results;
        }
    }

    // ======== MAIN PROGRAM ========
    public static void main(String[] args) {
        DiagnosisEngine engine = new DiagnosisEngine();

        System.out.println("=== SMART HEALTH DIAGNOSIS ASSISTANT ===");
        System.out.println("Goal: SDG 3 – Good Health and Well-being");
        System.out.println("Loading data from 'disease_symptoms.csv'...\n");

        try {
            engine.loadFromCSV("disease_symptoms.csv");
            System.out.println("✔ Loaded disease data successfully.\n");
        } catch (IOException e) {
            System.out.println("⚠ Error: Could not load 'disease_symptoms.csv'. Using sample data...");
            // fallback sample data
            List<String> flu = Arrays.asList("fever", "cough", "headache", "sore throat");
            List<String> cold = Arrays.asList("cough", "sneezing", "runny nose");
            List<String> diabetes = Arrays.asList("fatigue", "increased thirst", "frequent urination");
            engine.diseases.add(new Disease("Flu", flu));
            engine.diseases.add(new Disease("Common Cold", cold));
            engine.diseases.add(new ChronicDisease("Diabetes", diabetes, 24));
        }

        String name = "";
        int age = 0;
        String symptomsLine = "";
        Scanner sc = null;

        if (args.length > 0) {
            // Non-interactive test mode: read name, age, symptoms from the provided file
            try {
                List<String> lines = Files.readAllLines(Paths.get(args[0]));
                if (lines.size() > 0) name = lines.get(0).trim();
                if (lines.size() > 1) {
                    try {
                        age = Integer.parseInt(lines.get(1).trim());
                    } catch (NumberFormatException nfe) {
                        age = 0;
                    }
                }
                if (lines.size() > 2) symptomsLine = lines.get(2);
            } catch (IOException ioe) {
                System.out.println("Could not read test input file '" + args[0] + "'. Falling back to interactive mode.");
            }
        }

        if (name.isEmpty() && symptomsLine.isEmpty()) {
            // interactive fallback
            sc = new Scanner(System.in);
            System.out.print("Enter patient name: ");
            name = sc.nextLine().trim();
            System.out.print("Enter age: ");
            try {
                age = Integer.parseInt(sc.nextLine());
            } catch (Exception e) {
                age = 0;
            }

            System.out.println("\nEnter symptoms (comma separated, e.g., fever, cough, fatigue):");
            System.out.print("Symptoms: ");
            symptomsLine = sc.nextLine();
        }

        Patient patient = new Patient(name.isEmpty() ? "Unknown" : name, age);
        for (String s : symptomsLine.split("[,;]")) {
            patient.addSymptom(s);
        }

        System.out.println("\n--- Patient Info ---");
        System.out.println("Name: " + patient.getName());
        System.out.println("Age: " + patient.getAge());
        System.out.println("Symptoms: " + patient.getReportedSymptoms());

        List<Diagnosis> results = engine.diagnose(patient, 5);

        System.out.println("\n--- Possible Diagnoses ---");
        if (results.isEmpty()) {
            System.out.println("No likely matches found. Please consult a doctor.");
        } else {
            for (Diagnosis d : results) {
                System.out.println("→ " + d);
            }
        }

        System.out.println("\nDisclaimer: This tool is for educational purposes only. Not a medical diagnostic system.");
        if (sc != null) sc.close();
    }
}
