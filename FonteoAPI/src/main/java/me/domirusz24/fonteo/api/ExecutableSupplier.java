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

package me.domirusz24.fonteo.api;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class ExecutableSupplier {
    public static FFmpeg getFFmpeg(String ffmpeg) {
        try {
            return new FFmpeg(ffmpeg);
        } catch (IOException ignored) {}

        return getFFmpeg();
    }

    public static FFmpeg getFFmpeg() {
        try {
            return new FFmpeg();
        } catch (IOException ignored) {}

        try {
            return new FFmpeg(getFromResource("ffmpeg").get().getPath());
        } catch (IOException | UnsupportedOsException ignored) {
        }

        return null;
    }

    public static FFprobe getFFprobe(String ffmpeg) {
        try {
            return new FFprobe(ffmpeg);
        } catch (IOException ignored) {}

        return getFFprobe();
    }

    public static FFprobe getFFprobe() {
        try {
            return new FFprobe();
        } catch (IOException ignored) {}

        try {
            return new FFprobe(getFromResource("ffprobe").get().getPath());
        } catch (IOException | UnsupportedOsException ignored) {
        }

        return null;
    }

    public static Optional<File> getFromResource(String executableName) throws UnsupportedOsException, IOException {
        OsType type;
        if (SystemUtils.IS_OS_LINUX) {
            type = OsType.LINUX;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            type = OsType.WINDOWS;
        } else {
            throw new UnsupportedOsException();
        }

        if (new File(getOutputPath(executableName, type)).isFile()) {
            return Optional.of(new File(getOutputPath(executableName, type)));
        }

        String path = getResourcePath(executableName, type);

        if (FonteoAPI.class.getResource(path) == null) {
            return Optional.empty();
        }

        File outputFile = new File(getOutputPath(executableName, type));

        try (InputStream inputStream = FonteoAPI.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                return Optional.empty();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            byte[] bytes = new byte[1024];
            int pointer = 0;
            while((pointer = inputStream.read(bytes)) != -1){
                fileOutputStream.write(bytes, 0, pointer);
            }
            fileOutputStream.close();
        }

        outputFile.setExecutable(true);

        return Optional.of(outputFile);
    }

    public static String getResourcePath(String executableName, OsType type) {
        return switch (type) {
            case WINDOWS -> "/windows/" + executableName + ".exe";
            case LINUX -> "/linux/" + executableName;
        };
    }

    public static String getOutputPath(String executableName, OsType type) {
        return switch (type) {
            case WINDOWS -> executableName + ".exe";
            case LINUX -> executableName;
        };
    }

    public enum OsType {
        WINDOWS, LINUX
    }
}
