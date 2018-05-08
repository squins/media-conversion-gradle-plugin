# Media conversion Gradle plugin.

Convert resources from one format to another.

## Example to convert SVG to PNG using Inkscape

Folder `src/main/svg` contains SVG files that have to be converted to PNG and have to be added to the jar.

Add to `gradle.properties`:

    inkscapePath=/usr/local/bin/inkscape
    inkscapeDpi=90
    
    
Add to `build.gradle`:

    buildscript {
    
        dependencies {
            classpath "com.squins.media.conversion:media-conversion-gradle-plugin:0.1"
        }
    }

    ext {
        def twoDecimalsFormat = DecimalFormat.getInstance()
        twoDecimalsFormat.applyPattern("0.00")
        svgRasterizingDpi = twoDecimalsFormat.format(Integer.parseInt(inkscapeDpi) * 978.0 / 1024.0)
    }

    apply plugin:  "com.squins.media.conversion";
    media {
        converters {
            svg {
                commandLine project.inkscapePath, inputFilePath(), '-d', svgRasterizingDpi, '-e', outputFilePath('png')
                variants {
                    variantsSpecifiedByFile {
                        commandLine project.inkscapePath, inputFilePath(), '-w', variantProperty('width'), '-h', variantProperty('height'), '-e', outputFilePath('png')
                    }
                }
            }
            json {
                // empty placeholder to copy Contents.json files
            }
        }
    
        rootFolders {
            'src/main/svg' {
            }
        }
    }
    
    ext {
        jarContentsDir = new File(buildDir, 'generated/resources').path
    }
    
    sourceSets {
        main {
            resources.srcDirs jarContentsDir
        }
    }
    
    task copyGeneratedResourcesToJar(type: Copy) {
        from(convert_src_main_svg)
        into jarContentsDir
    }
    
    processResources.dependsOn copyGeneratedResourcesToJar
    
    
## Exporting PNG files in different sizes

Given a file src/main/svg/icon.svg that has to be exported in multiple sizes, a file icon.svg.groovy can be created. To export it in two sizes (512x512 and 1024x1024) contents of icon.svg.groovy have to be:
    
    
    [
            'icon-512x512.svg'       : [
                    width : 512,
                    height: 512
            ],
            'icon-1024x1024.svg'         : [
                    width : 1024,
                    height: 1024
            ]
    ]