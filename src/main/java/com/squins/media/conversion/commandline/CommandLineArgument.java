package com.squins.media.conversion.commandline;

import java.util.Map;

public interface CommandLineArgument {

    default boolean isRequired() {
        return true;
    }

    String resolve(Map<String, Object> variantProperties, String inputFilePath, String outputFilePath);
}
