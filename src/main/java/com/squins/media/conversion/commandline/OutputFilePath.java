package com.squins.media.conversion.commandline;

import java.util.Map;

import com.squins.media.conversion.FileExtension;

public class OutputFilePath implements CommandLineArgument {

    private String optionalNewExtension;

    public OutputFilePath() {
    }

    public OutputFilePath(String newExtension) {
        optionalNewExtension = newExtension;
    }

    @Override
    public String resolve(Map<String, Object> variantProperties, String inputFilePath, String outputFilePath) {
        String result;

        if (optionalNewExtension == null) {
            result = outputFilePath;
        } else {
            result = FileExtension.remove(outputFilePath) + optionalNewExtension;
        }

        return result;
    }
}
