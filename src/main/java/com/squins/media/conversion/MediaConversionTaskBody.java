package com.squins.media.conversion;

import com.squins.media.conversion.commandline.CommandLineArgument;
import com.squins.media.conversion.commandline.OutputFilePath;
import groovy.lang.GroovyShell;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.tooling.BuildException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.gradle.util.GFileUtils.copyFile;

class MediaConversionTaskBody implements Action<Task> {

    public static final int MAX_NUMBER_OF_CONCURRENT_CONVERSIONS = 18;

    private final Project project;
    private final MediaConversionExtensionProperties mediaConversionExtension;
    private MediaRootFolder rootFolder;
    private final File inputDirectory;
    private final File outputDirectory;
    private ExecutorService executorService;
    private List<ConversionProcessRunner> conversionProcessRunners = new ArrayList<>();
    private Map<String, String> outputToInputFileMapping = new HashMap<>();

    public MediaConversionTaskBody(Project project, MediaConversionExtensionProperties mediaConversionExtension, MediaRootFolder rootFolder, File inputDirectory, File outputDirectory) {
        this.project = project;
        this.mediaConversionExtension = mediaConversionExtension;
        this.rootFolder = rootFolder;
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        executorService = Executors.newFixedThreadPool(MAX_NUMBER_OF_CONCURRENT_CONVERSIONS);
    }

    @Override
    public void execute(@NotNull Task task) {
        processDirectory(inputDirectory, outputDirectory, "");

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        outputConversionErrorsAndFailBuildIfErrorsOccurred();
    }

    private void outputConversionErrorsAndFailBuildIfErrorsOccurred() {
        boolean hasErrorOccurredDuringConversion = false;
        for (ConversionProcessRunner conversionProcessRunner : conversionProcessRunners) {
            if (conversionProcessRunner.wasUnsuccessful()) {
                hasErrorOccurredDuringConversion = true;
                project.getLogger().error(conversionProcessRunner.getInputFile().getPath(), conversionProcessRunner.getFailureException());
                project.getLogger().error(conversionProcessRunner.getErrorOutput());
            }
        }
        failBuildIfConversionErrorsOccurred(hasErrorOccurredDuringConversion);
    }

    private void failBuildIfConversionErrorsOccurred(boolean hasErrorOccurredDuringConversion) {
        if (hasErrorOccurredDuringConversion) {
            failBuildDueToConversionErrors();
        }
    }

    private void failBuildDueToConversionErrors() {
        throw new IllegalStateException("Errors occurred during conversion of media.");
    }

    private void processDirectory(File inputDirectory, File outputDirectory, String relativeSubFolderPath) {
        ensureOutputDirectoryExists(outputDirectory);

        File[] optionalInputDirectoryEntries = inputDirectory.listFiles();
        if (optionalInputDirectoryEntries != null) {
            processDirectoryEntries(optionalInputDirectoryEntries, outputDirectory, relativeSubFolderPath);
        }
    }

    private void ensureOutputDirectoryExists(File outputDirectory) {
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create directory: " + outputDirectory);
        }
    }

    private void processDirectoryEntries(File[] inputDirectoryEntries, File outputDirectory, String relativeSubFolderPath) {
        for (File inputDirectoryEntry : inputDirectoryEntries) {
            processDirectoryEntry(inputDirectoryEntry, outputDirectory, relativeSubFolderPath);
        }
    }

    private void processDirectoryEntry(File inputDirectoryEntry, File outputDirectory, String relativeSubFolderPath) {
        File outputDirectoryEntry = new File(outputDirectory, inputDirectoryEntry.getName());
        if (inputDirectoryEntry.isDirectory()) {
            processDirectory(inputDirectoryEntry, outputDirectoryEntry, relativeSubFolderPath.isEmpty() ? inputDirectoryEntry.getName() : relativeSubFolderPath + '/' + inputDirectoryEntry.getName());
        } else {
            processFileIfExtensionAvailable(inputDirectoryEntry, outputDirectoryEntry, relativeSubFolderPath);
        }
    }

    private void processFileIfExtensionAvailable(File inputFile, File outputFile, String relativeSubFolderPath) {
        String optionalExtension = FileExtension.get(inputFile.getName());
        if (optionalExtension != null) {
            copyOrConvertIfConverterDefined(optionalExtension, inputFile, outputFile, relativeSubFolderPath);
        }
    }

    private void copyOrConvertIfConverterDefined(String extension, File inputFile, File outputFile, String relativeSubFolderPath) {
        MediaConverter optionalConverter = mediaConversionExtension.getConverters().findByName(extension);
        if (optionalConverter != null) {
            copyOrConvert(optionalConverter, inputFile, outputFile, relativeSubFolderPath);
        }
    }

    private void copyOrConvert(MediaConverter converter, File inputFile, File outputFile, String relativeSubFolderPath) {
        MediaConverter actualConverter = getActualConverter(converter, inputFile, relativeSubFolderPath);
        if (actualConverter.hasCommandLine()) {
            convertIfInputIsNewer(actualConverter, inputFile, outputFile);
        } else {
            copyIfInputIsNewer(inputFile, outputFile);
        }
    }

    private MediaConverter getActualConverter(MediaConverter mainConverter, File inputFile, String relativeSubFolderPath) {
        MediaConverter result;

        if (doesFileVariantsFileExist(inputFile)) {
            result = getVariantsSpecifiedByFileVariant(mainConverter, inputFile);
        } else {
            Set<String> subFolderVariants = getOptionalConverterVariantNameAsSet(mainConverter, relativeSubFolderPath);
            result = subFolderVariants.isEmpty() ? mainConverter : mainConverter.getVariants().getByName(subFolderVariants.iterator().next());
        }

        return result;
    }

    private boolean doesFileVariantsFileExist(File inputFile) {
        return getFileVariantsFile(inputFile).exists();
    }

    private MediaConverter getVariantsSpecifiedByFileVariant(MediaConverter mainConverter, File inputFile) {
        MediaConverter result = mainConverter.getVariants().findByName("variantsSpecifiedByFile");
        failBuildIfVariantsSpecifiedByFileVariantNotFound(result, mainConverter, inputFile);

        return result;
    }

    private void failBuildIfVariantsSpecifiedByFileVariantNotFound(MediaConverter optionalVariantsSpecifiedByFileVariant, MediaConverter converter, File inputFile) {
        if (optionalVariantsSpecifiedByFileVariant == null) {
            throw new BuildException("File: " + inputFile + ", has file variants specified, "
                    + "but there is no converter variant 'variantsSpecifiedByFile' specified for converter: "
                    + converter.getName(), null);
        }
    }

    private Set<String> getOptionalConverterVariantNameAsSet(MediaConverter mainConverter, String relativeSubFolderPath) {
        Set<String> result = getSubfolderVariants(relativeSubFolderPath);
        result.retainAll(mainConverter.getVariants().getNames());

        assertOnlyOneVariantOfConverterSpecifiedForSubFolder(relativeSubFolderPath, result);

        return result;
    }

    private Set<String> getSubfolderVariants(String relativeSubFolderPath) {
        Set<String> result = Collections.emptySet();

        NamedDomainObjectContainer<MediaSubFolder> subFolders = rootFolder.getSubFolders();
        for (MediaSubFolder subFolder : subFolders) {
            if (relativeSubFolderPath.startsWith(subFolder.getName())) {
                result = new HashSet<>(subFolder.getConverterVariants());
            }
        }

        return result;
    }

    private void assertOnlyOneVariantOfConverterSpecifiedForSubFolder(String relativeSubFolderPath, Set<String> subFolderVariants) {
        if (subFolderVariants.size() >= 2) {
            throw new IllegalStateException("Multiple matching variants: " + subFolderVariants + ", found for root folder: "
                    + rootFolder.getName() + ", and sub-folder: " + relativeSubFolderPath);
        }
    }

    private void convertIfInputIsNewer(MediaConverter converter, File inputFile, File outputFile) {
        Map<String, Map<String, Object>> fileVariants = getFileVariants(inputFile);
        List<File> conversionOutputFiles = getOutputFilesOfAllVariants(converter, fileVariants, inputFile, outputFile);
        registerOutputToInputFileMappings(inputFile, conversionOutputFiles);
        if (isInputNewer(inputFile, conversionOutputFiles)) {
            convertAllFileVariants(converter, fileVariants, inputFile, outputFile);
        }
    }

    private void convertAllFileVariants(MediaConverter converter, Map<String, Map<String, Object>> fileVariants, File inputFile, File outputFile) {
        for (Map.Entry<String, Map<String, Object>> fileVariant : fileVariants.entrySet()) {
            convertFileVariant(converter, fileVariant, inputFile, outputFile);
        }
    }

    private void convertFileVariant(MediaConverter converter, Map.Entry<String, Map<String, Object>> fileVariantAsMapEntry, File inputFile, File outputFile) {
        String variantFilename = fileVariantAsMapEntry.getKey();
        Map<String, Object> fileVariantProperties = fileVariantAsMapEntry.getValue();
        convert(converter, fileVariantProperties, inputFile, new File(outputFile.getParent(), variantFilename));
    }

    private List<File> getOutputFilesOfAllVariants(MediaConverter converter, Map<String, Map<String, Object>> fileVariants, File inputFile, File outputFile) {
        List<File> result = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> fileVariant : fileVariants.entrySet()) {
            List<File> conversionOutputFilesOfVariant = getOutputFiles(converter, fileVariant, inputFile, outputFile);
            result.addAll(conversionOutputFilesOfVariant);
        }

        return result;
    }

    private List<File> getOutputFiles(MediaConverter converter, Map.Entry<String, Map<String, Object>> fileVariantAsMapEntry, File inputFile, File outputFile) {
        String variantFilename = fileVariantAsMapEntry.getKey();
        Map<String, Object> fileVariantProperties = fileVariantAsMapEntry.getValue();
        return getOutputFiles(converter, fileVariantProperties, inputFile, new File(outputFile.getParent(), variantFilename));
    }

    private Map<String, Map<String, Object>> getFileVariants(File inputFile) {
        Map<String, Map<String, Object>> result;

        File file = getFileVariantsFile(inputFile);
        if (file.exists()) {
            result = loadFileVariantsFile(file);
        } else {
            result = Collections.singletonMap(inputFile.getName(), Collections.emptyMap());
        }

        return result;
    }

    private Map<String, Map<String, Object>> loadFileVariantsFile(File file) {
        Map<String, Map<String, Object>> result;

        try {
            result = tryToLoadFileVariantsFile(file);
        } catch (IOException ioException) {
            throw new BuildException("Error occurred during reading of file variants from: " + file, ioException);
        }

        return result;
    }

    private Map<String, Map<String, Object>> tryToLoadFileVariantsFile(File file) throws IOException {
        //noinspection unchecked
        return (Map<String, Map<String, Object>>) new GroovyShell().evaluate(file);
    }

    private File getFileVariantsFile(File inputFile) {
        return new File(inputFile.getParent(), inputFile.getName() + ".groovy");
    }

    private void addConversionOutputFileIfArgumentIsOutputFilePath(CommandLineArgument commandLineArgument, Map<String, Object> fileVariantProperties, File inputFile, File outputFile, List<File> conversionOutputFiles) {
        if (commandLineArgument instanceof OutputFilePath) {
            addConversionOutputFile(commandLineArgument, fileVariantProperties, inputFile, outputFile, conversionOutputFiles);
        }
    }

    private void addConversionOutputFile(CommandLineArgument commandLineArgument, Map<String, Object> fileVariantProperties, File inputFile, File outputFile, List<File> conversionOutputFiles) {
        String outputFilePath = commandLineArgument.resolve(fileVariantProperties, inputFile.getPath(), outputFile.getPath());
        conversionOutputFiles.add(new File(outputFilePath));
    }

    private List<File> getOutputFiles(MediaConverter converter, Map<String, Object> fileVariantProperties, File inputFile, File outputFile) {
        List<File> result = new ArrayList<>();

        for (CommandLineArgument commandLineArgument : converter.getCommandLineArguments()) {
            addConversionOutputFileIfArgumentIsOutputFilePath(commandLineArgument, fileVariantProperties, inputFile, outputFile, result);
        }

        return result;
    }

    private void convert(MediaConverter converter, Map<String, Object> fileVariantProperties, File inputFile, File outputFile) {
        printLine(getFileProcessingMessage(inputFile, outputFile));
        String[] effectiveCommandLineArguments = getEffectiveCommandLineArguments(converter, fileVariantProperties, inputFile, outputFile);
        convertInBackground(inputFile, effectiveCommandLineArguments);
    }

    private String getFileProcessingMessage(File inputFile, File outputFile) {
        StringBuilder resultBuilder = new StringBuilder(inputFile.getPath());

        if (hasDifferentFilename(inputFile, outputFile)) {
            resultBuilder.append(" (variant: ").append(outputFile.getName()).append(')');
        }

        return resultBuilder.toString();
    }

    private boolean hasDifferentFilename(File inputFile, File outputFile) {
        return !inputFile.getName().equals(outputFile.getName());
    }

    private void convertInBackground(File inputFile, String[] commandLineArguments) {
        ConversionProcessRunner conversionProcessRunner = createAndRegisterConversionProcessRunner(inputFile, commandLineArguments);
        executorService.execute(conversionProcessRunner);
    }

    private ConversionProcessRunner createAndRegisterConversionProcessRunner(File inputFile, String[] effectiveCommandLineArguments) {
        ConversionProcessRunner result = new ConversionProcessRunner(effectiveCommandLineArguments, inputFile);

        conversionProcessRunners.add(result);

        return result;
    }

    private String[] getEffectiveCommandLineArguments(MediaConverter converter, Map<String, Object> fileVariantProperties, File inputFile, File outputFile) {
        List<String> result = new ArrayList<>();

        for (CommandLineArgument commandLineArgument : converter.getCommandLineArguments()) {
            String argument = commandLineArgument.resolve(fileVariantProperties, inputFile.getPath(), outputFile.getPath());

            if (argument == null) {
                // remove name.
                result.remove(result.size() - 1);
            } else {
                result.add(argument);
            }
        }

        return result.toArray(new String[0]);
    }

    private boolean isInputNewer(File inputFile, List<File> outputFiles) {
        boolean result = false;

        for (File outputFile : outputFiles) {
            result |= outputFile.lastModified() < inputFile.lastModified();
        }

        return result;
    }

    private void copyIfInputIsNewer(File inputFile, File outputFile) {
        if (outputFile.lastModified() < inputFile.lastModified()) {
            copy(inputFile, outputFile);
        }
    }

    private void copy(File inputFile, File outputFile) {
        registerOutputToInputFileMapping(inputFile, outputFile);
        printLine(inputFile);
        copyFile(inputFile, outputFile);
    }

    private void printLine(Object object) {
        printLine(object.toString());
    }

    private void printLine(String message) {
        // Using "warn" because logging at level "info" does not show output unless an extra command line parameter is passed to Gradle
        project.getLogger().warn(message);
    }

    private void registerOutputToInputFileMappings(File inputFile, List<File> conversionOutputFiles) {
        for (File conversionOutputFile : conversionOutputFiles) {
            registerOutputToInputFileMapping(inputFile, conversionOutputFile);
        }
    }

    private void registerOutputToInputFileMapping(File inputFile, File outputFile) {
        failBuildIfOutputFileGeneratedForSecondTime(inputFile, outputFile);
        outputToInputFileMapping.put(outputFile.getAbsolutePath(), inputFile.getPath());
    }

    private void failBuildIfOutputFileGeneratedForSecondTime(File inputFile, File outputFile) {
        String absoluteOutputFilePath = outputFile.getAbsolutePath();
        if (outputToInputFileMapping.containsKey(absoluteOutputFilePath)) {
            throw new IllegalStateException("Output file: " + outputFile + ", was already generated by: "
                    + outputToInputFileMapping.get(absoluteOutputFilePath) + ", but would also be generated by: "
                    + inputFile);
        }
    }
}

