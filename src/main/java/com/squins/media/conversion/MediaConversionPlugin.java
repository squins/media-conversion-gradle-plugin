package com.squins.media.conversion;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;

public class MediaConversionPlugin implements Plugin<Project> {

    public static final String MEDIA_EXTENSION_NAME = "media";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(BasePlugin.class);
        NamedDomainObjectContainer<MediaConverter> convertersContainer = project.container(MediaConverter.class);
        NamedDomainObjectContainer<MediaRootFolder> foldersContainer = project.container(MediaRootFolder.class);
        project.getExtensions().create(MEDIA_EXTENSION_NAME, MediaConversionExtension.class, project, convertersContainer, foldersContainer);
    }
}
