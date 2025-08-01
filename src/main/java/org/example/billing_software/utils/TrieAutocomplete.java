package org.example.billing_software.utils;

import java.util.*;

public class TrieAutocomplete {
    private final TrieNode root = new TrieNode();

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isWord = false;
    }

    // Capitalize only the first character
    private String capitalize(String word) {
        if (word == null || word.isBlank()) return "";
        word = word.trim();
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

    public void insert(String word) {
        word = capitalize(word);
        if (word.isEmpty()) return;

        TrieNode node = root;
        for (char c : word.toLowerCase().toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isWord = true;
    }

    public List<String> getSuggestions(String prefix) {
        List<String> results = new ArrayList<>();
        String normalizedPrefix = capitalize(prefix);
        TrieNode node = root;

        for (char c : normalizedPrefix.toLowerCase().toCharArray()) {
            node = node.children.get(c);
            if (node == null) return results;
        }

        collectSuggestionsLimited(node, new StringBuilder(normalizedPrefix), results, 5);
        // Capitalize first letter of all results before returning
        return results.stream()
                .map(this::capitalize)
                .distinct()
                .toList();
    }

    private void collectSuggestionsLimited(TrieNode node, StringBuilder prefix, List<String> results, int max) {
        if (results.size() >= max) return;
        if (node.isWord) results.add(capitalize(prefix.toString()));
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            prefix.append(entry.getKey());
            collectSuggestionsLimited(entry.getValue(), prefix, results, max);
            prefix.setLength(prefix.length() - 1);
            if (results.size() >= max) return;
        }
    }


    public void clear() {
        root.children.clear();
    }
}
