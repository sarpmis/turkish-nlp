package org.TurkishNLP.preprocessing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/*
 * Class used for writing large amount of text
 * to a file. When buffer is full it will
 * write to file. When done using buffer
 * use finish()
 */
public class WriteBuffer {
    private Path outPath;
    private int capacity;
    StringBuilder builder;

    public WriteBuffer(Path outPath, int capacityKB){
        this.outPath = outPath;
        this.capacity = capacityKB*1000; // convert to bytes
        builder = new StringBuilder(capacity);
    }

    public void add(String str) {
        int capLeft = getCapacityLeft();
        if (str.length() > capLeft) {
            builder.append(str.substring(0,capLeft));
            write();
            builder.setLength(0);
            add(str.substring(capLeft));
        }
        else builder.append(str);
    }

    public void finish() {
        write();
        builder.setLength(0);
    }

    private boolean write() {
        try {
            Files.write(outPath, builder.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("wrote");
            return true;
        } catch (IOException e){
            return false;
        }
    }

    public void setOutPath(Path newPath){
        outPath = newPath;
    }

    public int getCapacityLeft() {
        return capacity-builder.length();
    }

    public String getString(){
        return builder.toString();
    }
}