package com.fajurion.learn.util;

import java.util.concurrent.ConcurrentHashMap;

public class Configuration {

    // Permission map
    public static final ConcurrentHashMap<String, Integer> permissions = new ConcurrentHashMap<>();

    // Settings map
    public static final ConcurrentHashMap<String, Integer> settings = new ConcurrentHashMap<>();

    // Constants map
    public static final ConcurrentHashMap<String, String> constants = new ConcurrentHashMap<>();

    public static void init() {

        // Initialize permissions
        permissions.put("upload.image", 0);
        permissions.put("create.topic", 0);
        permissions.put("delete.topic", 60);
        permissions.put("create.invite", 60);
        permissions.put("create.post.locked", 30);
        permissions.put("delete.post", 40);
        permissions.put("delete.comment", 40);
        permissions.put("view.admin.panel", 70);

        // Initialize settings
        settings.put("max.file.size", 1000 /* 1 KB */ * 4024); // in bytes (currently 4 MB)
        settings.put("session.timeout", 1000*60*60*24);
        settings.put("max.sessions", 10);
        settings.put("max.characters.post", 6000);
        settings.put("max.characters.post.title", 50);
        settings.put("max.characters.comment", 500);

        // Initialize constants
        constants.put("url", "https://localhost:8880/");

    }
}
