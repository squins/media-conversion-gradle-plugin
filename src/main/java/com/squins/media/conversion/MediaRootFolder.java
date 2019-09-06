package com.squins.media.conversion;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.FactoryNamedDomainObjectContainer;
import org.gradle.internal.reflect.DirectInstantiator;

import static org.gradle.api.internal.CollectionCallbackActionDecorator.NOOP;

public class MediaRootFolder {
    private String name;
    private NamedDomainObjectContainer<MediaSubFolder> subFolders =
            new FactoryNamedDomainObjectContainer<>(MediaSubFolder.class, DirectInstantiator.INSTANCE, MediaSubFolder::new, NOOP);

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
