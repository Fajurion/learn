package com.fajurion.learn.util;

public class ConstantConfiguration {

    // Permission levels
    public static final int PERMISSION_LEVEL_UPLOAD_IMAGE = 10;
    public static final int PERMISSION_LEVEL_CREATE_TOPIC = 5;
    public static final int PERMISSION_LEVEL_CREATE_INVITE = 60;
    public static final int PERMISSION_LEVEL_DELETE_TOPIC = 70;
    public static final int PERMISSION_LEVEL_CREATE_POST_LOCKED = 30;
    public static final int PERMISSION_LEVEL_DELETE_POST = 40;

    // Max file size
    public static final int MAX_FILE_SIZE = 1000 /* 1 KB */  * 1024; // in bytes (currently 1 MB)

    // Session timeout delay (in ms)
    public static final long SESSION_TIMEOUT_DELAY = 1000*60*60*24;

    // Maximum concurrent sessions
    public static final int MAXIMUM_CONCURRENT_SESSIONS = 10;

    // Maximum characters for posts
    public static final int MAXIMUM_CHARACTERS_POST = 6000;
    public static final int MAXIMUM_CHARACTERS_POST_TITLE = 50;

    // Maximum characters for comments
    public static final int MAXIMUM_CHARACTERS_COMMENT = 500; // It's recommended to also change it in the schema.sql (change "content VARCHAR(512)" to "content VARCHAR(length)"

}
