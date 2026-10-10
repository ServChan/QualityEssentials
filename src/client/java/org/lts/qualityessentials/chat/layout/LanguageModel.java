package org.lts.qualityessentials.chat.layout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LanguageModel {
    private static final double SMOOTHING = 0.1;

    private final Map<String, Integer> grams = new HashMap<>();
    private final Map<String, Integer> contexts = new HashMap<>();
    private final Set<String> words = new HashSet<>();
    private final int symbols;

    private LanguageModel(int alphabetSize) {
        this.symbols = alphabetSize + 2;
    }

    public static LanguageModel load(InputStream gramStream, InputStream wordStream, int alphabetSize) throws IOException {
        LanguageModel model = new LanguageModel(alphabetSize);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(gramStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int space = line.lastIndexOf(' ');
                if (space <= 0) {
                    continue;
                }
                String gram = line.substring(0, space);
                int count = Integer.parseInt(line.substring(space + 1).trim());
                model.grams.merge(gram, count, Integer::sum);
                model.contexts.merge(gram.substring(0, gram.length() - 1), count, Integer::sum);
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(wordStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty()) {
                    model.words.add(word);
                }
            }
        }
        return model;
    }

    public boolean knows(String word) {
        return words.contains(word.toLowerCase(Locale.ROOT));
    }

    public double score(String word) {
        String padded = "^^" + word.toLowerCase(Locale.ROOT) + "$";
        double bigrams = 0;
        double trigrams = 0;
        int steps = padded.length() - 2;
        for (int i = 2; i < padded.length(); i++) {
            bigrams += probability(padded.substring(i - 1, i + 1));
            trigrams += probability(padded.substring(i - 2, i + 1));
        }
        return (bigrams + trigrams) / (2.0 * steps);
    }

    private double probability(String gram) {
        int count = grams.getOrDefault(gram, 0);
        int context = contexts.getOrDefault(gram.substring(0, gram.length() - 1), 0);
        return Math.log((count + SMOOTHING) / (context + SMOOTHING * symbols));
    }
}
