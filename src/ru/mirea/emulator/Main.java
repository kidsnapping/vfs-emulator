package ru.mirea.emulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
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
    private boolean running;

    public Main(String vfsPath, String scriptPath) {
        this.vfsPath = vfsPath;
        this.scriptPath = scriptPath;
        if (vfsPath != null && !vfsPath.isEmpty()) {
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

    public void start() {
        printDebugParams();
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
            case "ls":
                return handleLs(args);
            case "cd":
                return handleCd(args);
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

    private boolean handleLs(String[] args) {
        System.out.println("stub: ls executed with args " + Arrays.toString(args));
        return true;
    }

    private boolean handleCd(String[] args) {
        if (args.length > ONE) {
            System.err.println("error: cd takes at most 1 argument");
            return false;
        }
        System.out.println("stub: cd executed with args " + Arrays.toString(args));
        return true;
    }
}