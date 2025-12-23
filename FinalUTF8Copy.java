import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class FinalUTF8Copy {
    public static void main(String[] args) {
        String refDir = "d:\\SpringAI\\workspace\\knowlearnMAP\\reference_java\\src\\main\\java\\com\\knowlearn\\prompt";
        String targetDir = "d:\\SpringAI\\workspace\\knowlearnMAP\\src\\main\\java\\com\\knowlearnmap\\prompt";

        try {
            // Delete target completely
            Path targetPath = Paths.get(targetDir);
            if (Files.exists(targetPath)) {
                Files.walk(targetPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                            }
                        });
            }

            // Copy files
            Path refPath = Paths.get(refDir);
            List<Path> files = new ArrayList<>();
            Files.walk(refPath).filter(Files::isRegularFile).forEach(files::add);

            System.out.println("Copying " + files.size() + " files...");

            for (Path srcFile : files) {
                // Read from reference (MS949)
                String content = new String(Files.readAllBytes(srcFile), "MS949");

                // Fix package names
                content = content.replace("com.knowlearn.prompt", "com.knowlearnmap.prompt");

                // Destination
                Path relativePath = refPath.relativize(srcFile);
                Path destFile = targetPath.resolve(relativePath);
                Files.createDirectories(destFile.getParent());

                // Save as UTF-8
                Files.write(destFile, content.getBytes(StandardCharsets.UTF_8));

                System.out.println("OK: " + destFile.getFileName());
            }

            System.out.println("\nDone! All files saved as UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
