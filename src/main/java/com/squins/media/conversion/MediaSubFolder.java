package com.squins.media.conversion;

import org.gradle.api.Named;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class MediaSubFolder implements Named {
    private String name;
    private Set<String> converterVariants;

    public MediaSubFolder(String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public Set<String> getConverterVariants() {
        return Collections.unmodifiableSet(converterVariants);
    }
}
