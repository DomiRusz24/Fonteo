/*
 * Fonteo
 * Copyright (C) 2023  DomiRusz24
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.domirusz24.fonteo;

import com.google.common.base.MoreObjects;
import me.domirusz24.fonteo.api.FonteoAPI;
import me.domirusz24.fonteo.api.UnsupportedOsException;
import net.bramp.ffmpeg.FFmpeg;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class App {
    public static void main(String[] args) throws IOException, UnsupportedOsException, URISyntaxException {

        if (args.length == 0) {
            emptyMain();
            return;
        }

        Options options = new Options();

        options
                .addOption(
                        Option.builder("i")
                                .longOpt("input")
                                .argName("videoInput")
                                .hasArg()
                                .desc("use given video for splitting.")
                                .type(PatternOptionBuilder.FILE_VALUE)
                                .required().build()
                )
                .addOption(
                        Option.builder("o")
                                .longOpt("output")
                                .argName("folderOutput")
                                .hasArg()
                                .desc("use given folder name for outputting frames.")
                                .type(PatternOptionBuilder.FILE_VALUE)
                                .required().build()
                )
                .addOption(
                        Option.builder("n")
                                .longOpt("format")
                                .argName("formatName")
                                .hasArg()
                                .desc("use given name format (default: \"%x-%y-%d\").")
                                .type(PatternOptionBuilder.STRING_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("e")
                                .longOpt("extension")
                                .argName("extensionType")
                                .hasArg()
                                .desc("use given photo extension (default: \"png\").")
                                .type(PatternOptionBuilder.STRING_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("f")
                                .longOpt("fps")
                                .argName("fps")
                                .hasArg()
                                .desc("use given fps value (default: 20).")
                                .type(PatternOptionBuilder.NUMBER_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("w")
                                .longOpt("width")
                                .argName("width")
                                .hasArg()
                                .desc("use given width (default: 765).")
                                .type(PatternOptionBuilder.NUMBER_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("h")
                                .longOpt("height")
                                .argName("height")
                                .hasArg()
                                .desc("use given height (default: 510).")
                                .type(PatternOptionBuilder.NUMBER_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("c")
                                .longOpt("columns")
                                .argName("columnAmount")
                                .hasArg()
                                .desc("use given column amount (default: 3).")
                                .type(PatternOptionBuilder.NUMBER_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("r")
                                .longOpt("rows")
                                .argName("rowAmount")
                                .hasArg()
                                .desc("use given row amount (default: 2).")
                                .type(PatternOptionBuilder.NUMBER_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("ffm")
                                .longOpt("ffmpeg")
                                .argName("ffmpeg")
                                .hasArg()
                                .desc("ffmpeg executable file (defaults to default ffmpeg path).")
                                .type(PatternOptionBuilder.FILE_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("ffp")
                                .longOpt("ffprobe")
                                .argName("ffprobe")
                                .hasArg()
                                .desc("ffprobe executable file (defaults to default ffprobe path).")
                                .type(PatternOptionBuilder.FILE_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder("h")
                                .hasArg(false)
                                .longOpt("help")
                                .desc("print out usages.")
                                .build()
                )
                .addOption(
                        Option.builder()
                                .hasArg()
                                .longOpt("flatten-type")
                                .desc("flatten type (simple, char_code) (default: simple).")
                                .type(PatternOptionBuilder.NUMBER_VALUE)
                                .build()
                )
                .addOption(
                        Option.builder()
                                .hasArg(false)
                                .longOpt("use-resource")
                                .desc("use the included executables.")
                                .build()
                )
        ;

        CommandLineParser parser = new DefaultParser();
        ExpandedCommandLine cmd;
        try {
            cmd = new ExpandedCommandLine(parser.parse(options, args));
        } catch (ParseException e) {
            e.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("fonteocli", options);
            return;
        }

        if (cmd.getCmd().hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("fonteocli", options);
            return;
        }

        if (cmd.getCmd().hasOption("use-resource")) {
            FonteoAPI.initFromResource();
        } else {
            boolean success = FonteoAPI.init(cmd.getOrDefault("ffmpeg", FFmpeg.DEFAULT_PATH), cmd.getOrDefault("ffprobe", MoreObjects.firstNonNull(System.getenv("FFPROBE"), "ffprobe")));

            if (!success) {
                System.out.println("Unable to obtain ffmpeg and/or ffprobe! You are most likely using an unsupported OS (Supported: Linux, Windows)");
            }
        }

        if (cmd.getCmd().hasOption("format") || !cmd.getCmd().hasOption("flatten-type")) {
            FonteoAPI.processVideo(
                    cmd.getOrCrash("input"),
                    cmd.getOrCrash("output"),
                    cmd.getOrDefault("format", "%x-%y-%d"),
                    cmd.getOrDefault("extension", "png"),
                    cmd.getOrDefaultNumber("fps", 20).intValue(),
                    cmd.getOrDefaultNumber("width", 765).intValue(),
                    cmd.getOrDefaultNumber("height", 510).intValue(),
                    cmd.getOrDefaultNumber("columns", 3).intValue(),
                    cmd.getOrDefaultNumber("rows", 2).intValue()
            );
        } else {
            FonteoAPI.processVideo(
                    cmd.getOrCrash("input"),
                    cmd.getOrCrash("output"),
                    cmd.getOrDefault("format", "%x-%y-%d"),
                    cmd.getOrDefault("extension", "png"),
                    cmd.getOrDefaultNumber("fps", 20).intValue(),
                    cmd.getOrDefaultNumber("width", 765).intValue(),
                    cmd.getOrDefaultNumber("height", 510).intValue(),
                    cmd.getOrDefaultNumber("columns", 3).intValue(),
                    cmd.getOrDefaultNumber("rows", 2).intValue(),
                    FonteoAPI.FlattenVideoType.valueOf(cmd.getOrDefault("flatten-type", "simple").toUpperCase())
            );
        }
    }

    public static void emptyMain() throws IOException, URISyntaxException {
        FonteoAPI.init();

        File folder = new File(new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath()).getParentFile();

        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".mp4") || file.getName().endsWith(".mov")) {
                FonteoAPI.processVideo(
                        file,
                        new File(file.getName().substring(0, file.getName().length() - 4) + "-frames"),
                        "%x-%y-%d",
                        "png",
                        20,
                        765,
                        510,
                        3,
                        2,
                        FonteoAPI.FlattenVideoType.SIMPLE
                );
            }
        }
    }
}
