package com.squins.media.conversion.commandline;

import java.util.Map;

public interface CommandLineArgument {
    String resolve(Map<String, Object> variantProperties, String inputFilePath, String outputFilePath);
}
