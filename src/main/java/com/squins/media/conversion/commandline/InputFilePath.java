package com.squins.media.conversion.commandline;

import java.util.Map;

import groovy.lang.Closure;

public class InputFilePath implements CommandLineArgument {
    private Closure optionalTransformer;

    public InputFilePath() {
        this(null);
    }

    public InputFilePath(Closure optionalTransformer) {
        this.optionalTransformer = optionalTransformer;
    }

    @Override
    public String resolve(Map<String, Object> variantProperties, String inputFilePath, String outputFilePath) {
        return optionalTransformer == null ? inputFilePath : optionalTransformer.call(inputFilePath).toString();
    }
}
