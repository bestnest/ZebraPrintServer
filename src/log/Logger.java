package log;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public abstract class Logger {

    private static final File logFile = new File("printServer.log");

    public static void log(String stuff) {
        System.out.println(stuff);

        // Add a newline to the end of stuff
        stuff = stuff + "\n";

        // Create the log file if it doesn't already exist
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (Exception ex) {
            System.out.println("There was an exception creating the new log file: " + ex.getMessage());
        }

        // Log the stuff to the file
        try {
            Files.write(logFile.toPath(), stuff.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
            System.err.println("We got this error trying to log to the file:");
            e.printStackTrace();
        }
    }

}
