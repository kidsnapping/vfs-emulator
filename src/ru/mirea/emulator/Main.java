package ru.mirea.emulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final int NOT_FOUND = -1;
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final String DEFAULT_VFS = "default-vfs";
    private static final String FLAG_VFS = "--vfs";
    private static final String FLAG_SCRIPT = "--script";
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$([A-Za-z0-9_]+)");

    private final String vfsPath;
    private final String scriptPath;
    private final String vfsName;
    private final Vfs vfs;
    private boolean running;

    public Main(String vfsPath, String scriptPath) {
        this.vfsPath = vfsPath;
        this.scriptPath = scriptPath;
        this.vfs = loadVfs(vfsPath);
        if (this.vfs != null) {
            this.vfsName = this.vfs.getName();
        } else if (vfsPath != null && !vfsPath.isEmpty()) {
            this.vfsName = new File(vfsPath).getName();
        } else {
            this.vfsName = DEFAULT_VFS;
        }
        this.running = true;
    }

    public static void main(String[] args) {
        String vfs = null;
        String script = null;
        for (int i = ZERO; i < args.length; i++) {
            if (FLAG_VFS.equals(args[i]) && i + ONE < args.length) {
                vfs = args[i + ONE];
                i++;
            } else if (FLAG_SCRIPT.equals(args[i]) && i + ONE < args.length) {
                script = args[i + ONE];
                i++;
            }
        }
        Main app = new Main(vfs, script);
        app.start();
    }

    private Vfs loadVfs(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("error: vfs file not found: " + path);
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new Vfs(file.getName(), bytes);
        } catch (IOException e) {
            System.err.println("error: invalid vfs archive format: " + path);
            return null;
        }
    }

    public void start() {
        printDebugParams();
        if (vfs != null) {
            vfs.printMotd();
        }
        if (scriptPath != null) {
            runScript(scriptPath);
        }
        if (running) {
            runRepl();
        }
    }

    private void printDebugParams() {
        String displayVfs = (vfsPath != null) ? vfsPath : "not specified";
        String displayScript = (scriptPath != null) ? scriptPath : "not specified";
        System.out.println("Debug config: vfsPath = " + displayVfs);
        System.out.println("Debug config: scriptPath = " + displayScript);
    }

    private void runScript(String path) {
        File scriptFile = new File(path);
        if (!scriptFile.exists()) {
            System.err.println("error: startup script file not found: " + path);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                processScriptLine(line);
            }
        } catch (IOException e) {
            System.err.println("error: failed reading startup script: " + e.getMessage());
        }
    }

    private void processScriptLine(String rawLine) {
        String line = rawLine.trim();
        int commentIndex = line.indexOf("//");
        if (commentIndex != NOT_FOUND) {
            line = line.substring(ZERO, commentIndex).trim();
        }
        if (line.isEmpty()) {
            return;
        }
        System.out.println("[" + vfsName + "]$ " + line);
        boolean ok = execute(line);
        if (!ok) {
            System.err.println("error: script command failed: " + line);
        }
    }

    private void runRepl() {
        Scanner scanner = new Scanner(System.in);
        while (running) {
            System.out.print("[" + vfsName + "]$ ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine();
            execute(line);
        }
    }

    private boolean execute(String line) {
        String expanded = expandEnv(line).trim();
        if (expanded.isEmpty()) {
            return true;
        }
        String[] parts = expanded.split("\\s+");
        if (parts.length == ZERO) {
            return true;
        }
        String command = parts[ZERO];
        String[] args = Arrays.copyOfRange(parts, ONE, parts.length);
        return dispatch(command, args);
    }

    private String expandEnv(String input) {
        Matcher matcher = ENV_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(ONE);
            String value = System.getenv(varName);
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean dispatch(String command, String[] args) {
        switch (command) {
            case "exit":
                return handleExit(args);
            case "vfs-info":
                return handleVfsInfo(args);
            case "ls":
                return handleLs(args);
            case "cd":
                return handleCd(args);
            case "cat":
                return handleCat(args);
            case "find":
                return handleFind(args);
            case "uname":
                return handleUname(args);
            default:
                System.err.println("error: unknown command '" + command + "'");
                return false;
        }
    }

    private boolean handleExit(String[] args) {
        if (args.length > ZERO) {
            System.err.println("error: exit does not take arguments");
            return false;
        }
        this.running = false;
        return true;
    }

    private boolean handleVfsInfo(String[] args) {
        if (args.length > ZERO) {
            System.err.println("error: vfs-info takes no arguments");
            return false;
        }
        if (vfs == null) {
            System.err.println("error: no vfs loaded");
            return false;
        }
        System.out.println("vfs: " + vfs.getName());
        System.out.println("sha256: " + vfs.getSha256Hex());
        return true;
    }

    private boolean handleLs(String[] args) {
        if (vfs == null) {
            System.err.println("error: no vfs loaded");
            return false;
        }
        if (args.length > ONE) {
            System.err.println("error: ls takes at most 1 argument");
            return false;
        }
        String targetPath = (args.length == ZERO) ? vfs.getCurrentPath() : args[ZERO];
        VfsNode node = vfs.resolve(targetPath);
        if (node == null) {
            System.err.println("error: ls: cannot access '" + targetPath + "': no such file or directory");
            return false;
        }
        if (!node.isDirectory()) {
            System.out.println(node.getName());
            return true;
        }
        List<String> names = new ArrayList<>(node.getChildren().keySet());
        Collections.sort(names);
        for (String name : names) {
            System.out.println(name);
        }
        return true;
    }

    private boolean handleCd(String[] args) {
        if (vfs == null) {
            System.err.println("error: no vfs loaded");
            return false;
        }
        if (args.length > ONE) {
            System.err.println("error: cd takes at most 1 argument");
            return false;
        }
        String targetPath = (args.length == ZERO) ? "/" : args[ZERO];
        VfsNode node = vfs.resolve(targetPath);
        if (node == null) {
            System.err.println("error: cd: no such file or directory: " + targetPath);
            return false;
        }
        if (!node.isDirectory()) {
            System.err.println("error: cd: not a directory: " + targetPath);
            return false;
        }
        List<String> segments = vfs.normalizePathSegments(targetPath);
        vfs.setCurrentPath(vfs.toPathString(segments));
        return true;
    }

    private boolean handleCat(String[] args) {
        if (vfs == null) {
            System.err.println("error: no vfs loaded");
            return false;
        }
        if (args.length == ZERO) {
            System.err.println("error: cat: missing operand");
            return false;
        }
        boolean ok = true;
        for (String path : args) {
            VfsNode node = vfs.resolve(path);
            if (node == null) {
                System.err.println("error: cat: " + path + ": no such file or directory");
                ok = false;
            } else if (node.isDirectory()) {
                System.err.println("error: cat: " + path + ": is a directory");
                ok = false;
            } else {
                System.out.println(new String(node.getData(), StandardCharsets.UTF_8));
            }
        }
        return ok;
    }

    private boolean handleFind(String[] args) {
        if (vfs == null) {
            System.err.println("error: no vfs loaded");
            return false;
        }
        if (args.length > ONE) {
            System.err.println("error: find takes at most 1 argument");
            return false;
        }
        String targetPath = (args.length == ZERO) ? "." : args[ZERO];
        VfsNode startNode = vfs.resolve(targetPath);
        if (startNode == null) {
            System.err.println("error: find: '" + targetPath + "': no such file or directory");
            return false;
        }
        printFindTree(startNode, targetPath);
        return true;
    }

    private void printFindTree(VfsNode node, String currentDisplay) {
        System.out.println(currentDisplay);
        if (!node.isDirectory()) {
            return;
        }
        List<String> names = new ArrayList<>(node.getChildren().keySet());
        Collections.sort(names);
        for (String childName : names) {
            VfsNode childNode = node.getChildren().get(childName);
            String childDisplay = currentDisplay.endsWith("/")
                    ? currentDisplay + childName
                    : currentDisplay + "/" + childName;
            printFindTree(childNode, childDisplay);
        }
    }

    private boolean handleUname(String[] args) {
        System.out.println("Linux vfs-emulator 6.8.0-generic x86_64 GNU/Linux");
        return true;
    }
}