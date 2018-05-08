package com.squins.media.conversion;

import java.util.Collections;
import java.util.Set;

import org.gradle.api.Named;

public class MediaSubFolder implements Named {
    private String name;
    private Set<String> converterVariants;

    public MediaSubFolder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getConverterVariants() {
        return Collections.unmodifiableSet(converterVariants);
    }
}
