import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class RemoveBOM {
    public static void main(String[] args) {
        String promptDir = "d:\\SpringAI\\workspace\\knowlearnMAP\\src\\main\\java\\com\\knowlearnmap\\prompt";

        try {
            List<Path> javaFiles = new ArrayList<>();
            Files.walk(Paths.get(promptDir))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(javaFiles::add);

            System.out.println("Removing BOM from " + javaFiles.size() + " Java files...");

            int fixedCount = 0;
            for (Path file : javaFiles) {
                try {
                    byte[] bytes = Files.readAllBytes(file);

                    // Check for UTF-8 BOM (EF BB BF)
                    if (bytes.length >= 3 &&
                            bytes[0] == (byte) 0xEF &&
                            bytes[1] == (byte) 0xBB &&
                            bytes[2] == (byte) 0xBF) {

                        // Remove BOM
                        byte[] newBytes = Arrays.copyOfRange(bytes, 3, bytes.length);
                        Files.write(file, newBytes);
                        fixedCount++;
                        System.out.println("Fixed BOM: " + file.getFileName());
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + file + " - " + e.getMessage());
                }
            }

            System.out.println("\nFixed: " + fixedCount + "/" + javaFiles.size() + " files");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
