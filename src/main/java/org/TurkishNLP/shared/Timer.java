package org.TurkishNLP.shared;

import lombok.NonNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Timer {
    private static final ConcurrentHashMap<UUID, Long> startTimes = new ConcurrentHashMap<>();

    public static TimerToken newToken() {
        TimerToken t = new TimerToken();
        Long current = System.nanoTime();
        startTimes.put(t.getUUID(), current);
        return t;
    }

    public static TimerResults checkOut(@NonNull TimerToken t) {
        if(t == null) throw new NullPointerException("Null token given to Timer!");
        UUID uuid = t.getUUID();
        synchronized (startTimes) {
            if(!startTimes.containsKey(uuid)) throw new RuntimeException("Timer doesn't recognize token!");
            long currentTime = System.nanoTime();
            long startTime = startTimes.get(uuid);
            startTimes.remove(uuid);
            return new TimerResults(startTime, currentTime);
        }
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

    public static class TimerToken {
        private final UUID uuid;

        public TimerToken() {
            this.uuid = UUID.randomUUID();
        }

        public UUID getUUID() {
            return uuid;
        }

        @Override
        public String toString() {
            return uuid.toString();
        }
    }

    public static class TimerResults {
        private final long startNano, endNano;
        public long duration;

        public TimerResults(long startNano, long endNano) {
            this.startNano = startNano;
            this.endNano = endNano;
            duration = endNano - startNano;
        }

        public long asNanos() {
            return duration;
        }

        public long asMillis() {
            return duration / 1000000;
        }

        public long asSeconds() {
            return asMillis() / 1000;
        }

        public long asMinutes() {
            return asSeconds() / 60;
        }

        public long asHours() {
            return asMinutes() / 60;
        }

        public int nanoRemainder() {
            return (int) (duration % 1000000);
        }

        public int milliRemainder() {
            return (int) (asMillis() % 1000);
        }

        public int secRemainder() {
            return (int) (asSeconds()) % 60;
        }

        public int minRemainder() {
            return (int) (asMinutes() % 60);
        }

        public int hourRemainder() {
            return (int) asHours();
        }

        public String humanReadable() {
            return ((asHours() == 0 || hourRemainder() == 0) ? "" : hourRemainder() + "h ") +
                ((asMinutes() == 0 || minRemainder() == 0) ? "" : minRemainder() + "m ") +
                ((asSeconds() == 0 || secRemainder() == 0) ? "" : secRemainder() + "s ");

        }

        public String humanReadableIncludeMillis() {
            return humanReadable() + milliRemainder() + "ms";
        }
    }
}
