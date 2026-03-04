import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlagiarismDetectionSystem {

    private final int N;
    private Map<String, Set<String>> ngramIndex = new ConcurrentHashMap<>();
    private Map<String, Integer> documentNgramCount = new ConcurrentHashMap<>();

    public PlagiarismDetectionSystem(int n) {
        this.N = n;
    }

    private List<String> generateNGrams(String text) {
        List<String> ngrams = new ArrayList<>();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+");
        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < N; j++) {
                sb.append(words[i + j]).append(" ");
            }
            ngrams.add(sb.toString().trim());
        }
        return ngrams;
    }

    public void addDocument(String documentId, String content) {
        List<String> ngrams = generateNGrams(content);
        documentNgramCount.put(documentId, ngrams.size());

        for (String ngram : ngrams) {
            ngramIndex.computeIfAbsent(ngram, k -> ConcurrentHashMap.newKeySet())
                      .add(documentId);
        }
    }

    public void analyzeDocument(String documentId, String content) {

        List<String> ngrams = generateNGrams(content);
        Map<String, Integer> matchCount = new HashMap<>();

        for (String ngram : ngrams) {
            Set<String> docs = ngramIndex.get(ngram);
            if (docs != null) {
                for (String doc : docs) {
                    matchCount.merge(doc, 1, Integer::sum);
                }
            }
        }

        System.out.println("Analyzing: " + documentId);
        System.out.println("Extracted " + ngrams.size() + " n-grams");

        String mostSimilarDoc = null;
        double highestSimilarity = 0;

        for (Map.Entry<String, Integer> entry : matchCount.entrySet()) {
            String otherDoc = entry.getKey();
            int matches = entry.getValue();
            int total = documentNgramCount.getOrDefault(otherDoc, 1);
            double similarity = (matches * 100.0) / total;

            System.out.println("Found " + matches + " matching n-grams with \"" 
                               + otherDoc + "\"");
            System.out.println("Similarity: " + String.format("%.2f", similarity) + "%");

            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                mostSimilarDoc = otherDoc;
            }
        }

        if (mostSimilarDoc != null) {
            if (highestSimilarity > 60) {
                System.out.println("Most Similar: " + mostSimilarDoc + 
                                   " → " + String.format("%.2f", highestSimilarity) +
                                   "% (PLAGIARISM DETECTED)");
            } else if (highestSimilarity > 15) {
                System.out.println("Most Similar: " + mostSimilarDoc + 
                                   " → " + String.format("%.2f", highestSimilarity) +
                                   "% (Suspicious)");
            } else {
                System.out.println("No significant plagiarism detected.");
            }
        } else {
            System.out.println("No matches found.");
        }
    }

    public static void main(String[] args) {

        PlagiarismDetectionSystem system = new PlagiarismDetectionSystem(5);

        String essay1 = "Artificial intelligence is transforming the world of technology and innovation rapidly.";
        String essay2 = "Artificial intelligence is transforming the world of technology and innovation rapidly with new advancements.";
        String essay3 = "The history of ancient civilizations provides insight into cultural evolution.";

        system.addDocument("essay_089.txt", essay1);
        system.addDocument("essay_092.txt", essay2);
        system.addDocument("essay_100.txt", essay3);

        String newEssay = "Artificial intelligence is transforming the world of technology and innovation rapidly.";

        system.analyzeDocument("essay_123.txt", newEssay);
    }
}