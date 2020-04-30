package com.wordsuggestion;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Main {

  /**
   * This is the main driver of the bi-gram creator. It uses the BiGramMapper to create an object
   * representing a list of bi-grams. It gets user input for a word, then uses the bi-gram mapper to
   * print suggestions for the most likely words to follow that word.
   *
   * @param args Not used for this program.
   */
  public static void main(String[] args) {

    Scanner input = new Scanner(System.in);
    BiGramMapper biGrams = null;

    // Try to create the bi-grams from the specified file. If the file does not exist, exit the
    // program immediately, as there is no data to analyze.
    try {
      biGrams = new BiGramMapper(Paths.get("res/messages.txt"));
    } catch (IOException ioEx) {

      System.out.println("Error: \"" + ioEx.getMessage() + "\" not found. Aborting process.");
      System.exit(1);
    }

    // Used a boolean to print a different line to the user for the first word they type. This
    // is not necessary, but it is included for readability and dynamism.
    boolean first = true;

    String searchTerm;

    // This is the main program loop. The condition gets the user's input then checks to see if
    // it equals "/q". If it does, the user wants to exit, and we break the loop. If it contains
    // any other string, we accept the input continue to process.
    // their input.
    while (!(searchTerm = getInput(input, first)).contains("/q")) {

      // Gather user input for a word to give suggestions for - and since we now have the first
      // user input, we can set the "first" flag to false.
      first = false;

      // If the user types /q, they want to quit, so break from the program loop.
      if (searchTerm.equalsIgnoreCase("/q")) {
        break;
      }

      // Fetch the batch of word suggestions that applies to the last word the user entered.
      // In a real-world scenario, there would be error checking here; but since the path is hard
      // coded and we know the file always exists, we can assume biGrams was created successfully.
      List<String> results = biGrams.getSuggestions(searchTerm);

      // Print the results of the bi-gram analysis.
      printSuggestions(results);
      System.out.println();
    }
    input.close();
  }

  /**
   * A simply method to get input from the user. It accepts a scanner to read user input, as well as
   * a boolean that marks whether this is the first input being gathered from the user. If it is, it
   * prints a slightly different message. Note this is mostly for polish and is not necessary for
   * proper program functionality.
   *
   * @param input A Scanner object for capturing user input.
   * @param first A boolean which marks whether this is the first time user input is captured.
   * @return
   */
  public static final String getInput(Scanner input, boolean first) {

    if (first == true) {
      System.out
          .println("Please begin your sentence by typing the first word (type /q to quit):");
    } else {
      System.out.println("Continue the sentence by typing the next word (type /q to quit):");
    }

    // For future comparisons, make sure to convert the input to lower case before returning it.
    return input.nextLine().toLowerCase();
  }

  /**
   * Prints the suggestions given by the BiGramMapper's getSuggestions() method.
   *
   * @param results A String array containing the most likely next words.
   */
  public static final void printSuggestions(List<String> results) {

    // Print the suggestions.
    System.out.println("Suggestions for next word: ");
    for (String s : results) {
      System.out.print("| " + s + " ");
    }
    System.out.print("|");
  }
}