package com.squins.media.conversion;

import java.io.File;
import java.io.IOException;

public class ConversionProcessRunner implements Runnable {
    private static final int EXIT_VALUE_INDICATING_SUCCESS = 0;
    private String[] commandLineArguments;
    private File inputFile;
    private String errorOutput;
    private Exception failureException;

    public ConversionProcessRunner(String[] commandLineArguments, File inputFile) {
        this.commandLineArguments = commandLineArguments;

        this.inputFile = inputFile;
    }

    @Override
    public void run() {
        try {
            tryToRunProcess(commandLineArguments, inputFile);
        } catch (InterruptedException | IOException exception) {
            failureException = exception;
        }
    }

    private void tryToRunProcess(String[] commandLineArguments, File inputFile) throws IOException, InterruptedException {

        System.out.println("Start process: " + inputFile.getName());
        Process process = Runtime.getRuntime().exec(commandLineArguments);

        System.out.println("Finished process: " + inputFile.getName());

        saveErrorOutput(process);
        markAsFailedIfExitValueIndicatesFailure(inputFile, process);
    }

    private void saveErrorOutput(Process process) throws IOException {
        StringBuilder errorOutputBuilder = readErrorOutput(process);
        if (errorOutputBuilder.length() > 0) {
            errorOutput = errorOutputBuilder.toString();
        }
    }

    private StringBuilder readErrorOutput(Process process) throws IOException {
        StringBuilder result = new StringBuilder();

        int characterAsInt = process.getErrorStream().read();
        while (characterAsInt != -1) {
            result.append((char) characterAsInt);
            characterAsInt = process.getErrorStream().read();
        }

        return result;
    }

    private void markAsFailedIfExitValueIndicatesFailure(File inputFile, Process process) throws InterruptedException {
        int exitValue = process.waitFor();
        if (exitValue != EXIT_VALUE_INDICATING_SUCCESS) {
            failureException = new IllegalStateException("Failed to convert: " + inputFile + ", process returned: " + exitValue);
        }
    }

    public boolean wasUnsuccessful() {
        return failureException != null;
    }

    public File getInputFile() {
        return inputFile;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    public Exception getFailureException() {
        return failureException;
    }
}
