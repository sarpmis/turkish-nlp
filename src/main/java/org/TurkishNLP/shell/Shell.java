package org.TurkishNLP.shell;

import org.TurkishNLP.word2vec.Word2VecOperations;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fusesource.jansi.AnsiConsole;
import org.jline.builtins.Completers;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;

import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 *
 */
public class Shell {
    private String[] commandsList;
    private Word2Vec w;

    public void run() {
        Completer completer = new Completers.TreeCompleter(
                node("help"),
                node("model",
                        node("read", "save", "closest", "sim")),
                node("exit"));

        Parser parser = new DefaultParser();

        AnsiConsole.systemInstall(); // needed to support ansi on Windows cmd

        LineReader reader = LineReaderBuilder.builder()
                .completer(completer)
                .appName("Turkish-NLP")
                .parser(parser)
                .build();

        String line;

        while ((line = readLine(reader, "")) != null) {
            ParsedLine pl = reader.getParser().parse(line, 0);
            String command = pl.word();
            switch(command) {
                case "help":
                    printHelp();
                    break;
                case "model":
                    if (pl.words().size() < 2) {
                        if(w == null) System.out.println(ShellConstants.NO_MODEL_ERROR_MESSAGE);
                    } else {
                        String secondCommand = pl.words().get(1);
                        switch (secondCommand) {
                            case "read":
                                if(pl.words().size() != 3) {
                                    printGeneralError();
                                } else {
                                    String modelName = pl.words().get(2);
                                    w = Word2VecOperations.readModel(modelName);
                                    System.out.println();
                                }
                                break;
                            case "save":
                            case "closest":
                                if(pl.words().size() != 4) {
                                    printGeneralError();
                                } else if (w == null) {
                                    printNoModelError();
                                } else {
                                    String thirdCommand = pl.words().get(2);
                                    String fourthCommand = pl.words().get(3);
                                    if (!thirdCommand.matches("\\d+")) {
                                        printGeneralError();
                                    } else {
                                        Word2VecOperations.printClosest(w, fourthCommand, Integer.parseInt(thirdCommand));
                                        System.out.println();
                                    }
                                }
                                break;
                            case "sim":
                                if(pl.words().size() != 4) {
                                    printGeneralError();
                                } else if (w == null) {
                                    printNoModelError();
                                } else {
                                    System.out.println("TODO");
                                }
                                break;
                        }
                        if(secondCommand.equals("read")){

                        } else if(secondCommand.equals("save")){

                        } else if(secondCommand.equals("closest")){

                        } else if(secondCommand.equals("sim")) {

                        }
                    }
                    break;
                case "corpus":

            }
            if (command.equals("help")) {

            } else if (command.equals("model")) {

            } else {
                printGeneralError();
            }
        }
        AnsiConsole.systemUninstall();
    }

    private void printHelp() {
        System.out.println(ShellConstants.HELP_MESSAGE);
    }

    private void printGeneralError() {
        System.out.println(ShellConstants.GENERAL_ERROR_MESSAGE);
    }

    private void printNoModelError() {
        System.out.println(ShellConstants.NO_MODEL_ERROR_MESSAGE);
    }

    private String readLine(LineReader reader, String promptMessage) {
                try {
                    String line = reader.readLine(promptMessage + "\nnlp> ");
                    return line.trim();
                }
                catch (UserInterruptException e) {
                    // e.g. ^C
                    return null;
                }
                catch (EndOfFileException e) {
                    // e.g. ^D
                    return null;
        }
    }

    public static void main(String[] args) {
        Shell shell = new Shell();
        shell.run();
    }
}
