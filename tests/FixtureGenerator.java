import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FixtureGenerator {
    public static void main(String[] args) throws IOException {
        File dir = new File("tests");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        createMinimalZip();
        createMultiZip();
        createNestedZip();
        createCorruptedFile();
    }

    private static void createMinimalZip() throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream("tests/vfs_minimal.zip"))) {
            addFile(zos, "test.txt", "Minimal archive content");
        }
    }

    private static void createMultiZip() throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream("tests/vfs_multi.zip"))) {
            addFile(zos, "motd", "Welcome to VFS Multi-file system!");
            addFile(zos, "readme.txt", "Multi VFS description");
            addFile(zos, "config.cfg", "mode=interactive");
        }
    }

    private static void createNestedZip() throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream("tests/vfs_nested.zip"))) {
            addFile(zos, "motd", "Welcome to 3-Level Nested VFS!");
            addFile(zos, "level1/level2/level3/deep.txt", "Deep level content");
            addFile(zos, "level1/level2/mid.txt", "Mid level content");
            addFile(zos, "level1/top.txt", "Top level content");
        }
    }

    private static void createCorruptedFile() throws IOException {
        try (FileWriter writer = new FileWriter("tests/vfs_corrupted.zip")) {
            writer.write("This is a plain text file, not a zip archive!");
        }
    }

    private static void addFile(ZipOutputStream zos, String path, String text)
            throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(text.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}