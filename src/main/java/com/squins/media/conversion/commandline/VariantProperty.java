package com.squins.media.conversion.commandline;

import java.util.Map;

public class VariantProperty implements CommandLineArgument {
    private String propertyName;

    public VariantProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public String resolve(Map<String, Object> variantProperties, String inputFilePath, String outputFilePath) {
        Object value = variantProperties.get(propertyName);
        assertPropertyValueSpecified(value);
        return String.valueOf(value);
    }

    private void assertPropertyValueSpecified(Object optionalValue) {
        if (optionalValue == null) {
            throw new IllegalStateException("No value specified for variant property: " + propertyName);
        }
    }
}
