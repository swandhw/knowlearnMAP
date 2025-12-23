import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CopyWithMS949 {
    public static void main(String[] args) {
        String refDir = "d:\\SpringAI\\workspace\\knowlearnMAP\\reference_java\\src\\main\\java\\com\\knowlearn\\prompt";
        String targetDir = "d:\\SpringAI\\workspace\\knowlearnMAP\\src\\main\\java\\com\\knowlearnmap\\prompt";

        try {
            // Delete target prompt directory completely
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

            // Copy all files from reference
            Path refPath = Paths.get(refDir);
            List<Path> files = new ArrayList<>();
            Files.walk(refPath).filter(Files::isRegularFile).forEach(files::add);

            System.out.println("Copying " + files.size() + " files from reference_java...\n");

            for (Path srcFile : files) {
                // Read from reference (MS949 encoding)
                String content = new String(Files.readAllBytes(srcFile), "MS949");

                // Replace package names ONLY
                content = content.replace("com.knowlearn.prompt", "com.knowlearnmap.prompt");

                // Calculate destination
                Path relativePath = refPath.relativize(srcFile);
                Path destFile = targetPath.resolve(relativePath);
                Files.createDirectories(destFile.getParent());

                // Save with MS949 encoding (for Eclipse on Korean Windows)
                Files.write(destFile, content.getBytes("MS949"));

                System.out.println("✓ " + destFile.getFileName());
            }

            System.out.println("\n=== SUCCESS ===");
            System.out.println("Copied all files with MS949 encoding");
            System.out.println("Package changed: com.knowlearn.prompt -> com.knowlearnmap.prompt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
