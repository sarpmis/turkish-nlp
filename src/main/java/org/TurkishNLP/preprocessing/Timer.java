package org.TurkishNLP.preprocessing;

public class Timer {
    private static long startTime, endTime;

    public static void setTimer(){
        startTime = System.currentTimeMillis();
    }

    public static void endTimer() {
        endTime = System.currentTimeMillis();
    }

    public static String results() {
        long duration = endTime - startTime;
        reset();
        return millisToHumanReadable(duration);
    }

    public static void reset() {
        startTime = 0;
        endTime = 0;
    }

    private static String millisToHumanReadable(long time) {
        long totalSeconds = (time / 1000);
        long totalMinutes = totalSeconds / 60;
        long totalHours = totalMinutes / 60;
        return(
                ((totalHours != 0) ? totalHours + " hours, " : "")
                + ((totalMinutes != 0) ? totalMinutes % 60 + " minutes, " : "")
                + ((totalSeconds != 0) ? totalSeconds % 60 + " seconds" : time + " miliseconds"));
    }
}
