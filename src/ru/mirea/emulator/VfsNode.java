package ru.mirea.emulator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class VfsNode {
    private static final String DEFAULT_OWNER = "root";

    private final String name;
    private final boolean isDirectory;
    private byte[] data;
    private String owner;
    private final Map<String, VfsNode> children;

    public VfsNode(String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.data = new byte[0];
        this.owner = DEFAULT_OWNER;
        if (isDirectory) {
            this.children = new LinkedHashMap<>();
        } else {
            this.children = Collections.emptyMap();
        }
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Map<String, VfsNode> getChildren() {
        return children;
    }

    public void addChild(VfsNode child) {
        children.put(child.getName(), child);
    }
}