package com.squins.media.conversion;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.Task;

class MediaConversionTaskCreator {
    public static void createTaskIfNonExistent(Project project, MediaConversionExtensionProperties mediaConversionExtension, MediaRootFolder rootFolder) {
        String rootFolderName = rootFolder.getName();
        final String pathAsJavaIdentifier = convertPathToJavaIdentifier(rootFolderName);
        String taskName = "convert_" + pathAsJavaIdentifier;
        if (project.getTasksByName(taskName, false).isEmpty()) {
            createTask(project, mediaConversionExtension, rootFolderName, taskName, pathAsJavaIdentifier);
        }
    }

    private static void createTask(Project project, MediaConversionExtensionProperties mediaConversionExtension, String rootFolderName, String taskName, String pathAsJavaIdentifier) {
        Task conversionTask = project.task(taskName);

        final File inputDirectory = project.file(rootFolderName);
        conversionTask.getInputs().dir(inputDirectory);
        final File outputDirectory = new File(project.getBuildDir(), pathAsJavaIdentifier);

        // debugging info:
        // System.out.println("inputDirectory: " + inputDirectory + ", outputDirectory: " + outputDirectory);

        conversionTask.getOutputs().dir(outputDirectory);

        Task buildTask = project.getTasks().getByName("build");
        buildTask.dependsOn(conversionTask);

        conversionTask.doLast(new MediaConversionTaskBody(project, mediaConversionExtension, mediaConversionExtension.getRootFolders().getByName(rootFolderName), inputDirectory, outputDirectory));
    }

    private static String convertPathToJavaIdentifier(String path) {
        StringBuilder resultBuilder = new StringBuilder(path.length());

        for (int characterIndex = 0; characterIndex < path.length(); characterIndex++) {
            char character = path.charAt(characterIndex);
            if (Character.isJavaIdentifierPart(character)) {
                resultBuilder.append(character);
            } else {
                resultBuilder.append('_');
            }
        }

        return resultBuilder.toString();
    }
}
