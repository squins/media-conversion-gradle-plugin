package com.squins.media.conversion.commandline;

import java.util.Map;

public class Literal implements CommandLineArgument {
    private String value;

    public Literal(String value) {
        this.value = value;
    }

    @Override
    public String resolve(Map<String, Object> variantProperties, String inputFilePath, String outputFilePath) {
        return value;
    }
}
