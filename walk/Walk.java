package info.kgeorgiy.ja.kupriyanov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Walk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("args should be in the format like: <inputFile> <outputFile>");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];
        try {
            Path.of(inputFile);
            Path.of(outputFile);
        } catch (InvalidPathException e) {
            System.err.println("Error while processing files: " + e.getMessage());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile, StandardCharsets.UTF_8))) {
            File outputFileObj = new File(outputFile);
            File parentDirectory = outputFileObj.getParentFile();
            if (parentDirectory != null && !parentDirectory.exists()) {
                parentDirectory.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String calcHash = calculateHash(line.trim());
                    writer.write(calcHash + " " + line + "\n");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error while processing files: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Security exception occurred: " + e.getMessage());
        }
    }

    private static String calculateHash(String pathToFile) {
        try {
            String cleanedPath = pathToFile.trim();
            File file = new File(cleanedPath);
            if (!file.isFile() || !file.exists()) {
                return "00000000";
            }
            if (!file.canRead()) {
                System.err.println("Cannot read file: " + pathToFile);
                return "00000000";
            }
            if (file.length() == 0) {
                return "00000000";
            }
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(data);
            }
            int calcHash = jenkinsHash(data);
            return String.format("%08x", calcHash);
        } catch (IOException e) {
            return "00000000";
        } catch (SecurityException e) {
            System.err.println("Security error: " + e.getMessage());
            return "00000000";
        }
    }

    private static int jenkinsHash(byte[] data) {
        int calcHash = 0;
        if (data != null) {
            for (final byte b : data) {
                calcHash += b & 0xff;
                calcHash += calcHash << 10;
                calcHash ^= calcHash >>> 6;
            }
            calcHash += calcHash << 3;
            calcHash ^= calcHash >>> 11;
            calcHash += calcHash << 15;
        }
        return calcHash;
    }
}
