package ru.mirea.emulator;

import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final String DEFAULT_VFS = "default-vfs";
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$([A-Za-z0-9_]+)");

    private final String vfsName;
    private boolean running;

    public Main(String vfsName) {
        this.vfsName = vfsName;
        this.running = true;
    }

    public static void main(String[] args) {
        Main app = new Main(DEFAULT_VFS);
        app.run();
    }

    public void run() {
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

    private void execute(String line) {
        String expanded = expandEnv(line).trim();
        if (expanded.isEmpty()) {
            return;
        }
        String[] parts = expanded.split("\\s+");
        if (parts.length == ZERO) {
            return;
        }
        String command = parts[ZERO];
        String[] args = Arrays.copyOfRange(parts, ONE, parts.length);
        dispatch(command, args);
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

    private void dispatch(String command, String[] args) {
        switch (command) {
            case "exit":
                handleExit(args);
                break;
            case "ls":
                handleLs(args);
                break;
            case "cd":
                handleCd(args);
                break;
            default:
                System.err.println("error: unknown command '" + command + "'");
                break;
        }
    }

    private void handleExit(String[] args) {
        if (args.length > ZERO) {
            System.err.println("error: exit does not take arguments");
            return;
        }
        this.running = false;
    }

    private void handleLs(String[] args) {
        System.out.println("stub: ls executed with args " + Arrays.toString(args));
    }

    private void handleCd(String[] args) {
        if (args.length > ONE) {
            System.err.println("error: cd takes at most 1 argument");
            return;
        }
        System.out.println("stub: cd executed with args " + Arrays.toString(args));
    }
}