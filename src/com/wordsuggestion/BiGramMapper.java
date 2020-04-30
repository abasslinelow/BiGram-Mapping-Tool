package com.wordsuggestion;

import globals.Globals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * This class creates a map bi-grams that determines how often a word is likely to follow another
 * word. The function that created the BiGramMapper can then use getSuggestions() to generate a
 * list of the top 3 most likely words that will follow a given word. The class is designed to take
 * a list of lines from a text file of random text message data.
 */
public class BiGramMapper {

  /**
   * The bi-grams are stored with a key value of a List containing 2 elements. The first element
   * is a word from any of the text messages, and the second element is a word that follows that
   * word. COL_ORIGIN_WORD points us to the former word of the key value.
   */
  private static final int COL_ORIGIN_WORD = 0;

  /**
   * The bi-grams are stored with a key value of a List containing 2 elements. The first element
   * is a word from any of the text messages, and the second element is a word that follows that
   * word. COL_COMPARE_WORD points us to the latter word of the key value.
   */
  private static final int COL_COMPARE_WORD = 1;

  /**
   * The path to the file containing text messages. This is the file we will create bi-grams from.
   */
  private Path path;

  /**
   * Each entry in this HashMap consists of a String that represents a word, and another HashMap
   * that contains every word that follows that word, as well as the number of times it follows
   * that word throughout the entire data set.
   */
  private Map<List<String>, Integer> biGrams = new HashMap<>();

  /**
   * The number of word suggestions to display to the user. This is set to a global right now, but
   * in a real-world setting, I would have setters and an overloaded constructor that allows the
   * programmer to specify the number of suggestions to return.
   */
  private int numResults = Globals.NUM_RESULTS;

  /**
   * The number of total transactions, i.e. the number of text messages in the data set.
   */
  private int totalTransactions;

  /**
   * This List contains the most common connecting words in the English language and is used to
   * pad any list of suggestions that does not have numResults number of related words.
   */
  private List<String> connectors = new ArrayList<>(
      Arrays.asList("the", "this", "of"));



  /**
   * Default constructor. It takes a List containing lines of text and uses it to create bi-grams
   * for each word in each line of the list. Each bi-gram for a word contains a list of all words
   * in the List that follow that word, as well as the number of times it follows that word.
   *
   * @param path A Path object containing a path to a text file.
   */
  BiGramMapper(Path path) throws IOException {
    this.path = path;
    createBiGrams();
  }

  /**
   * This function creates bi-grams for each word in each line of the transactions it is given.
   * It does this by iterating through every transaction, creating a bigram for every word. Each
   * bi-gram contains a list of words that follow a given word, as well as the amount of times
   * those words follow the given word. It tallies the total number of transactions as well.
   *
   */
  private void createBiGrams() throws IOException {

    // Read the contents of the file provided by the method that created the BiGramMapper.
    // Filter out blank lines, then map all words to lowercase, split each line into an array
    // containing each word on that line, then collect the results as a List of Lists. Elements
    // in the outer list represent a line, while elements in the inner list represent a word.
    List<List<String>> messagesLines = Files.lines(path)
        .filter(line -> !line.isBlank())
        .map(String::toLowerCase)
        .map(line -> Arrays.asList(line.split("\\s+")))
        .collect(Collectors.toList());


    // For each line of text messages...
    for (int i = 0; i < messagesLines.size(); i++) {

      // For each word in that line of the text message...
      List<String> messagesWords = messagesLines.get(i);
      for (int j = 1; j < messagesWords.size(); j++) {

        // Create a new List containing two words that represent a bi-gram key. Since we are
        // tracking words that *follow* other words, order is important, so we use a List rather
        // than Map. (For example, ["dad", "tmi"] is distinct from ["tmi", "dad"], because the
        // former means "dad follows tmi", while the latter means "tmi follows dad".)
        List<String> wordCombo = new ArrayList<>(Arrays.asList(
            messagesWords.get(j - 1),
            messagesWords.get(j)
        ));

        // Now merge the word combo into the list of bi-grams. If the combo does not exist in the
        // list, insert it and assign it a value of 1. If it does exist in the list, add 1 to the
        // value that is associated with that key.
        biGrams.merge(wordCombo, 1, Integer::sum);
      }
    }
  }

  /**
   * Returns the top 3 most likely words to follow the origin word, given that they have a
   * confidence percentage of >65%. If it cannot find 3 words that meet this criteria, it pads the
   * rest of the list with random connector words as defined by the private member ArrayList called
   * "connectors".
   *
   * @param searchTerm The word for which we want to generate suggestions for next words.
   * @return String[] The 3 most likely words to follow the origin word.
   */
  public List<String> getSuggestions(String searchTerm) {

    // Shuffles the connectors so they appear in different orders and frequencies.
    // This is not necessary, but it makes the program feel more dynamic, and it
    // allows all connectors the possibility of being be shown.
    Collections.shuffle(connectors);

    Comparator<Entry<List<String>, Integer>> comparator = Comparator.comparing(Entry::getValue);

    // To generate the word suggestions, we get a stream of all entries in the bi-grams structure.
    // We filter out all results that do not match the search term, then calculate their
    // confidence, further filtering out any results that do not have >65% confidence. We then
    // sort the list by value, putting the largest values on top, so that when we limit ourselves
    // to 3 results, they are the results with the highest confidence. Finally, we map the
    // remaining results into a List of strings that contain our suggestions.
    List<String> suggestions = biGrams.entrySet().stream()
        .filter(e -> e.getKey().get(COL_ORIGIN_WORD).equals(searchTerm))
        .filter(e -> calculateConfidence(e.getKey()) > .65)
        .sorted(comparator.reversed())
        .limit(3)
        .map(entry -> entry.getKey().get(COL_COMPARE_WORD))
        .collect(Collectors.toList());


    // Finally, we need to see if the suggestions are equal to the number of results we want
    // to return. If they are not, pad the list with the most common English connector words.
    int numMissingSuggestions = numResults - suggestions.size();
    for (int i = 0; i < numMissingSuggestions; i++) {
      suggestions.add(connectors.get(i));
    }

    return suggestions;
  }

  /**
   * This function calculates the percentage of times that the compare word follows the origin
   * word in relation to all other words that follow the origin word. In probability notation, this
   * is depicted as P(compareWord|originWord) or "the probability of compareWord, given originWord."
   *
   * <p>Note that, in a live scenario, this function would have error handling to ensure compareWord
   * exists in the bi-gram for the origin word, and that originWord exists in the list of bigrams.
   * </p>
   *
   * @param wordCombo List Represents a key in biGrams. Two elements: [originWord, compareWord]
   * @return Double The confidence percentage.
   */
  private double calculateConfidence(List<String> wordCombo) {

    double sum = sumOfNextWords(wordCombo.get(COL_ORIGIN_WORD));

    return biGrams.get(wordCombo) / sum;
  }

  /**
   *  Calculates the total number of times the origin word is followed by any other word.
   *  This is used to calculate the percentage of the time that a given word will
   *  follow the origin word.
   *
   * @param originWord Any word in the text message data.
   * @return int The number of times the origin word is followed by any other word.
   */
  private int sumOfNextWords(String originWord) {

    // Calculate the sum by filtering only the entries that have the origin word as their
    // first word, then mapping the values of those entries and summing the results.
    int sum = biGrams.entrySet().stream()
        .filter(e -> e.getKey().get(COL_ORIGIN_WORD).equals(originWord))
        .mapToInt(Entry::getValue)
        .sum();

    return sum;
  }

  /**
   * Calculates the percentage of text messages that have the origin word followed by the compare
   * word. In probably notation, this is depicted as P(originWord ∩ compareWord), or "the
   * probability that the compare word follows origin word in a text message, in relation to the
   * entire set of messages." (This is not quite accurate, as order is not considered in regards
   * to intersections, but the concept is the same.)
   *
   * <p>Note that, in a live scenario, this function would have error handling to ensure compareWord
   * exists in the bi-gram for the origin word, and that originWord exists in the list of bigrams.
   * </p>
   *
   * @param wordCombo List Represents a key in biGrams: Two elements: [originWord, compareWord]
   * @return Double The support percentage.
   */
  private double calculateSupport(List<String> wordCombo) {

    // Return the amount of times the compare word follows the origin word, divided by the
    // total number of text messages.
    return biGrams.get(wordCombo) / totalTransactions;

  }
}
