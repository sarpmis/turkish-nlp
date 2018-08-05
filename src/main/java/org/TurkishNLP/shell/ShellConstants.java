package org.TurkishNLP.shell;

public class ShellConstants {
    public static String WELCOME_MESSAGE = "";

    public static String HELP_MESSAGE =
              "help                        - Show available commands"
            + System.lineSeparator()
            + "model"
            + System.lineSeparator()
            + "     read \"modelName\"       - Read model into memory"
            + System.lineSeparator()
            + "     save \"modelName\"       - Save model to disk"
            + System.lineSeparator()
            + "     closest <n> \"word\"     - Find n closest words"
            + System.lineSeparator()
            + "     sim \"word1\" \"word2\"    - Show similarity between two words"
            + System.lineSeparator()
            + "exit                        - Exit the app"
            ;

    public static String GENERAL_ERROR_MESSAGE =
            "Invalid command. Type \"help\" to see available commands";
    public static String NO_MODEL_ERROR_MESSAGE =
            "No model in memory yet, use read \"modelName\" to read an existing model";

}
