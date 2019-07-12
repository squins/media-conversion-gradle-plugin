package com.squins.media.conversion.commandline;

import java.util.Map;

public class VariantProperty implements CommandLineArgument {
    private String propertyName;
    private final boolean isRequired;

    public VariantProperty(String propertyName, boolean isRequired) {
        this.propertyName = propertyName;
        this.isRequired = isRequired;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public String resolve(Map<String, Object> variantProperties, String inputFilePath, String outputFilePath) {
        Object value = retrievePropertyValue(variantProperties);
        assertPropertyValueSpecified(value);

        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private Object retrievePropertyValue(Map<String, Object> variantProperties) {
        return variantProperties.get(propertyName);
    }

    private void assertPropertyValueSpecified(Object optionalValue) {
        if (optionalValue == null && isRequired) {
            throw new IllegalStateException("No value specified for variant property: " + propertyName);
        }
    }

    @Override
    public String toString() {
        return "VariantProperty{" +
                "propertyName='" + propertyName + '\'' +
                ", isRequired=" + isRequired +
                '}';
    }
}
