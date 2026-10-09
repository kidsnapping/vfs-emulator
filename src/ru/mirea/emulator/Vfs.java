package ru.mirea.emulator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Vfs {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int NOT_FOUND = -1;
    private static final int MIN_ZIP_HEADER_LEN = 4;
    private static final int BUFFER_SIZE = 1024;
    private static final byte ZIP_BYTE_P = 0x50;
    private static final byte ZIP_BYTE_K = 0x4B;
    private static final String ROOT_NAME = "/";
    private static final String MOTD_NAME = "motd";

    private final String name;
    private final String sha256Hex;
    private final VfsNode root;
    private String currentPath;

    public Vfs(String name, byte[] zipBytes) throws IOException {
        if (!isValidZipHeader(zipBytes)) {
            throw new IOException("invalid zip header");
        }
        this.name = name;
        this.sha256Hex = calculateSha256(zipBytes);
        this.root = new VfsNode(ROOT_NAME, true);
        this.currentPath = ROOT_NAME;
        loadZipEntries(zipBytes);
    }

    private static boolean isValidZipHeader(byte[] data) {
        if (data.length < MIN_ZIP_HEADER_LEN) {
            return false;
        }
        return data[ZERO] == ZIP_BYTE_P && data[ONE] == ZIP_BYTE_K;
    }

    private static String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void loadZipEntries(byte[] zipBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
        try (ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] entryData = readEntryData(zis);
                addEntry(entry.getName(), entry.isDirectory(), entryData);
                zis.closeEntry();
            }
        }
    }

    private byte[] readEntryData(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = zis.read(buffer, ZERO, BUFFER_SIZE)) != NOT_FOUND) {
            baos.write(buffer, ZERO, read);
        }
        return baos.toByteArray();
    }

    private void addEntry(String path, boolean isDir, byte[] data) {
        String[] parts = path.split("/");
        VfsNode current = root;
        for (int i = ZERO; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            boolean isLast = (i == parts.length - ONE);
            if (isLast && !isDir) {
                VfsNode fileNode = new VfsNode(part, false);
                fileNode.setData(data);
                current.addChild(fileNode);
            } else {
                VfsNode dirNode = current.getChildren().get(part);
                if (dirNode == null) {
                    dirNode = new VfsNode(part, true);
                    current.addChild(dirNode);
                }
                current = dirNode;
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getSha256Hex() {
        return sha256Hex;
    }

    public VfsNode getRoot() {
        return root;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public void printMotd() {
        VfsNode motdNode = root.getChildren().get(MOTD_NAME);
        if (motdNode != null && !motdNode.isDirectory()) {
            String text = new String(motdNode.getData(), StandardCharsets.UTF_8).trim();
            if (!text.isEmpty()) {
                System.out.println(text);
            }
        }
    }

    public List<String> normalizePathSegments(String path) {
        List<String> segments = new ArrayList<>();
        if (!path.startsWith("/")) {
            String[] currentParts = currentPath.split("/");
            for (String part : currentParts) {
                if (!part.isEmpty()) {
                    segments.add(part);
                }
            }
        }
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - ONE);
                }
            } else {
                segments.add(part);
            }
        }
        return segments;
    }

    public VfsNode resolve(String path) {
        List<String> segments = normalizePathSegments(path);
        VfsNode current = root;
        for (String segment : segments) {
            VfsNode next = current.getChildren().get(segment);
            if (next == null) {
                return null;
            }
            current = next;
        }
        return current;
    }

    public String toPathString(List<String> segments) {
        if (segments.isEmpty()) {
            return ROOT_NAME;
        }
        StringBuilder sb = new StringBuilder();
        for (String seg : segments) {
            sb.append("/").append(seg);
        }
        return sb.toString();
    }
}