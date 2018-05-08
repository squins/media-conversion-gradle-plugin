package com.squins.media.conversion;

public class FileExtension {
    private static final int NOT_FOUND_INDEX = -1;

    public static String get(String filename) {
        String result;

        int indexOfFirstDot = filename.indexOf('.');
        if (indexOfFirstDot == NOT_FOUND_INDEX) {
            result = null;
        } else {
            result = filename.substring(indexOfFirstDot + 1);
        }

        return result;
    }

    public static String remove(String path) {
        String result;

        int lastIndexOfDot = path.lastIndexOf('.');
        if (lastIndexOfDot == NOT_FOUND_INDEX) {
            result = path;
        } else {
            result = path.substring(0, lastIndexOfDot + 1);
        }

        return result;
    }
}
