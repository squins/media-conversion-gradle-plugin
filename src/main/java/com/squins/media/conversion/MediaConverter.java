package com.squins.media.conversion;

import com.squins.media.conversion.commandline.CommandLineArgument;
import com.squins.media.conversion.commandline.InputFilePath;
import com.squins.media.conversion.commandline.Literal;
import com.squins.media.conversion.commandline.OutputFilePath;
import com.squins.media.conversion.commandline.VariantProperty;
import groovy.lang.Closure;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.FactoryNamedDomainObjectContainer;
import org.gradle.internal.reflect.DirectInstantiator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.gradle.api.internal.CollectionCallbackActionDecorator.NOOP;

public class MediaConverter implements Named {
    private String name;
    private List<CommandLineArgument> commandLineArguments = new ArrayList<>();
    private NamedDomainObjectContainer<MediaConverter> variants =
            new FactoryNamedDomainObjectContainer<>(MediaConverter.class, DirectInstantiator.INSTANCE, MediaConverter::new, NOOP);
    private List<String> optionalVariantProperties = new ArrayList<>();

    public MediaConverter(String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public boolean hasCommandLine() {
        return !commandLineArguments.isEmpty();
    }

    public List<CommandLineArgument> getCommandLineArguments() {
        return commandLineArguments;
    }

    public void commandLine(Object... arguments) {
        for (Object argument : arguments) {
            CommandLineArgument argumentAsCommandLineArgument;

            if (argument instanceof CommandLineArgument) {
                argumentAsCommandLineArgument = (CommandLineArgument) argument;
            } else if (argument instanceof String) {
                argumentAsCommandLineArgument = new Literal((String) argument);
            } else {
                throw new IllegalArgumentException("Invalid command line argument: " + argument);
            }

            commandLineArguments.add(argumentAsCommandLineArgument);
        }
    }

    public InputFilePath inputFilePath() {
        return new InputFilePath();
    }

    public InputFilePath inputFilePath(Closure transformer) {
        return new InputFilePath(transformer);
    }

    public OutputFilePath outputFilePath() {
        return new OutputFilePath();
    }

    public OutputFilePath outputFilePath(String newExtension) {
        return new OutputFilePath(newExtension);
    }

    public VariantProperty variantProperty(String propertyName) {
        return new VariantProperty(propertyName, !optionalVariantProperties.contains(propertyName));
    }

    public void variants(Closure<?> closure) {
        variants.configure(closure);
    }

    public void optionalVariantProperties(String... optionalVariantProperties) {
        this.optionalVariantProperties = Arrays.asList(optionalVariantProperties);
    }

    public NamedDomainObjectContainer<MediaConverter> getVariants() {
        return variants;
    }
}
