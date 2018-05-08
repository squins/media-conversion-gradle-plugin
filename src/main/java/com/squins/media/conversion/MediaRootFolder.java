package com.squins.media.conversion;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.FactoryNamedDomainObjectContainer;
import org.gradle.internal.reflect.DirectInstantiator;

public class MediaRootFolder {
    private String name;
    private NamedDomainObjectContainer<MediaSubFolder> subFolders = new FactoryNamedDomainObjectContainer<MediaSubFolder>(MediaSubFolder.class, DirectInstantiator.INSTANCE);

    public MediaRootFolder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void subFolders(Closure<?> closure) {
        subFolders.configure(closure);
    }

    public NamedDomainObjectContainer<MediaSubFolder> getSubFolders() {
        return subFolders;
    }
}
