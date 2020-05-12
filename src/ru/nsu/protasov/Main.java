package ru.nsu.protasov;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length != 4 && args.length != 2) {
                throw new IllegalArgumentException("Invalid args\n1) 3 required: mode, input file, output file\n" +
                        "2) Special mode -m for meta file output");
            }

            if (args.length == 4) {
                if (args[0].equalsIgnoreCase("-e")) {
                    var coder = new Coder(50);
                    coder.encode(args[1], args[2], args[3]);
                    System.out.println("Encoded");
                } else if (args[0].equalsIgnoreCase("-d")) {
                    var decoder = new Decoder();
                    decoder.decode(args[1], args[2], args[3]);
                    System.out.println("Decoded");
                } else {
                    throw new IllegalArgumentException("Unknown key, -e or -d required");
                }
            } else if (args[0].equalsIgnoreCase("-m")) {
                Tools.readMeta(args[1]);
            } else {
                throw new IllegalArgumentException("Unknown key, -m required");
            }
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: " + e.getMessage() + " hasn't been found");
        } catch (IllegalArgumentException | OutOfMemoryError | FileSystemException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error while reading files, restart required");
        }
    }
}
