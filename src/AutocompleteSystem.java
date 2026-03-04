import java.util.*;
import java.util.concurrent.*;

public class AutocompleteSystem {

    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd;
        Set<String> queries = new HashSet<>();
    }

    private TrieNode root = new TrieNode();
    private ConcurrentHashMap<String, Integer> frequencyMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<String>> prefixCache = new ConcurrentHashMap<>();
    private final int TOP_K = 10;

    public void addQuery(String query) {
        frequencyMap.merge(query, 1, Integer::sum);
        insertIntoTrie(query);
        prefixCache.clear();
    }

    private void insertIntoTrie(String query) {
        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
            node.queries.add(query);
        }
        node.isEnd = true;
    }

    public List<String> search(String prefix) {
        if (prefixCache.containsKey(prefix)) {
            return prefixCache.get(prefix);
        }

        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) {
                return handleTypo(prefix);
            }
        }

        PriorityQueue<String> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(frequencyMap::get)
        );

        for (String q : node.queries) {
            minHeap.offer(q);
            if (minHeap.size() > TOP_K) {
                minHeap.poll();
            }
        }

        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll());
        }
        Collections.reverse(result);

        prefixCache.put(prefix, result);
        return result;
    }

    private List<String> handleTypo(String word) {
        List<String> suggestions = new ArrayList<>();
        for (String query : frequencyMap.keySet()) {
            if (editDistanceOne(word, query)) {
                suggestions.add(query);
            }
        }

        suggestions.sort((a, b) -> frequencyMap.get(b) - frequencyMap.get(a));
        return suggestions.size() > TOP_K ? suggestions.subList(0, TOP_K) : suggestions;
    }

    private boolean editDistanceOne(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return false;
        int i = 0, j = 0, edits = 0;
        while (i < a.length() && j < b.length()) {
            if (a.charAt(i) != b.charAt(j)) {
                if (++edits > 1) return false;
                if (a.length() > b.length()) i++;
                else if (a.length() < b.length()) j++;
                else { i++; j++; }
            } else {
                i++; j++;
            }
        }
        return true;
    }

    public void updateFrequency(String query) {
        frequencyMap.merge(query, 1, Integer::sum);
        prefixCache.clear();
    }

    public static void main(String[] args) {

        AutocompleteSystem system = new AutocompleteSystem();

        system.addQuery("java tutorial");
        system.addQuery("javascript");
        system.addQuery("java download");
        system.addQuery("java 21 features");
        system.addQuery("java tutorial");
        system.addQuery("java tutorial");

        System.out.println("Search results for 'jav':");
        List<String> results = system.search("jav");
        int rank = 1;
        for (String r : results) {
            System.out.println(rank++ + ". " + r + 
                " (" + system.frequencyMap.get(r) + " searches)");
        }

        system.updateFrequency("java 21 features");
        system.updateFrequency("java 21 features");
        system.updateFrequency("java 21 features");

        System.out.println("\nUpdated frequency for 'java 21 features': " +
                system.frequencyMap.get("java 21 features"));
    }
}