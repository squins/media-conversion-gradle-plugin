package com.squins.media.conversion;

import org.gradle.api.NamedDomainObjectContainer;

public interface MediaConversionExtensionProperties {
    NamedDomainObjectContainer<MediaConverter> getConverters();

    NamedDomainObjectContainer<MediaRootFolder> getRootFolders();
}
