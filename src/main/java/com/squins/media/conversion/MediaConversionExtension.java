package com.squins.media.conversion;

import java.util.Set;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

public class MediaConversionExtension implements MediaConversionExtensionProperties {
    private Project project;
    private NamedDomainObjectContainer<MediaConverter> converters;
    private NamedDomainObjectContainer<MediaRootFolder> rootFolders;

    public MediaConversionExtension(Project project, NamedDomainObjectContainer<MediaConverter> converters, NamedDomainObjectContainer<MediaRootFolder> rootFolders) {
        this.project = project;
        this.converters = converters;
        this.rootFolders = rootFolders;
    }

    public void converters(Closure<?> closure) {
        converters.configure(closure);
    }

    @Override
    public NamedDomainObjectContainer<MediaConverter> getConverters() {
        return converters;
    }

    public void rootFolders(Closure<?> closure) {
        rootFolders.configure(closure);
        Set<String> folderNames = rootFolders.getNames();
        for (String folderName : folderNames) {
            MediaConversionTaskCreator.createTaskIfNonExistent(project, this, rootFolders.getByName(folderName));
        }
    }

    @Override
    public NamedDomainObjectContainer<MediaRootFolder> getRootFolders() {
        return rootFolders;
    }
}
