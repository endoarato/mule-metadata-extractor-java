package com.consciousconduit;

import java.util.Map;
import java.util.Set;
import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.DefaultParser;

import com.consciousconduit.ImageExtractor;
import com.consciousconduit.LightThemeApplier;
import com.consciousconduit.MappingGenerator;
import com.consciousconduit.WidgetTypeExtractor;

public class Core {
    public static Options cliOptions = new Options()
        .addOption("d", "anypoint-dir", true, "Anypoint Studio Directory")
        .addOption("o", "output", true, "Path where the generated mapping file will be written to")
        .addOption("v", null, false, "Verbosity level")
        .addOption("h", "help", false, null);
    
    public static CommandLineParser parser = new DefaultParser();
    
    public static String usage(String optionsSummary) {
        return String.join(System.lineSeparator(),
            "This is a tool for extracting information from Mule plugins",
            "",
            "Usage: mule-preview-extractor [options] action",
            "",
            "Options:",
            optionsSummary,
            "",
            "Actions:",
            "   apply-light-theme       Extract images from the light theme plugin",
            "   extract-images          Extract images from plugins",
            "   extract-widget-types    Generate a list of possible widget types",
            "   generate-mappings       Generate mappings for a mule-preview client",
            "");
    }

    public static String errorMsg(String errorMessage) {
        return "The following errors occurred while parsing your command:\n\n" +
                String.join(System.lineSeparator(), errorMessage);
    }

    public static Map<String, Object> validateArgs(String[] args) {
        try {
            CommandLine commandLine = parser.parse(cliOptions, args);
            Option[] options = commandLine.getOptions();
            Map<String, Object> optionMap = Map.of();
            for (Option option : options) {
                optionMap.put(option.getOpt(), option.getValue());
            }
            String[] arguments = commandLine.getArgs();
            String summary = String.join(System.lineSeparator(), 
                                commandLine.getOptions().toString());
            if (commandLine.hasOption("help")) {
                return Map.of("exitMessage", usage(summary), "ok?", true);
            } else if (arguments.length == 1 &&
                        Set.of("apply-light-theme", "extract-images", "extract-widget-types", "generate-mappings").contains(arguments[0]) &&
                        commandLine.hasOption("anypoint-dir") &&
                        commandLine.hasOption("output")) {
                return Map.of("action", arguments[0], "options", optionMap);
            } else {
                return Map.of("exitMessage", usage(summary), "ok?", false);
            }
        } catch (Exception e) {
            System.err.print(errorMsg(e.getLocalizedMessage()));
            return Map.of("exitMessage", e.getLocalizedMessage());
        }
    }

    public static void exit(int status, String msg) {
        System.out.println(msg);
        System.exit(status);
    }

    public static void main(String[] args) {
        Map<String, Object> validationResult = validateArgs(args);
        String exitMessage = validationResult.get("exitMessage").toString();
        Boolean ok = validationResult.get("ok?") != null;

        if (exitMessage != null) {
            exit(ok ? 0 : 1, exitMessage);
        } else {
            String action = validationResult.get("action").toString();
            Map<String, Object> options = (Map<String, Object>)validationResult.get("options");
            String anypointDir = options.get("anypoint-dir").toString();
            String outputDir = options.get("output").toString();

            new File(outputDir, "dummy").getParentFile().mkdirs();

            switch (action) {
                case "apply-light-theme":
                    Applier.applyLightThemeFromAnypointDir(anypointDir, outputDir);
                    break;
                case "extract-images":
                    Copying.scanDirectoryForPlugins(anypointDir, outputDir);
                    break;
                case "extract-widget-types":
                    Extraction.scanDirectoryForWidgetTypes(anypointDir, outputDir);
                    break;
                case "generate-mappings":
                    Generation.scanDirectoryForPlugins(anypointDir, outputDir);
                    break;
            }
        }
    }
}
