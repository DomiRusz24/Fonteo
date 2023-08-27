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
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

public class FonteoAPI {

    private static FFmpeg ffmpeg;
    private static FFprobe ffprobe;
    private static FFmpegExecutor executor;

    public static boolean init(FFmpeg ffmpeg, FFprobe ffprobe) {

        if (ffmpeg == null || ffprobe == null) {
            return false;
        }

        FonteoAPI.ffmpeg = ffmpeg;
        FonteoAPI.ffprobe = ffprobe;

        FonteoAPI.executor = new FFmpegExecutor(ffmpeg, ffprobe);

        return true;
    }

    public static boolean init() throws IOException {
        return init(ExecutableSupplier.getFFmpeg(), ExecutableSupplier.getFFprobe());
    }

    public static boolean initFromResource() throws IOException, UnsupportedOsException {
        FonteoAPI.ffmpeg = new FFmpeg(ExecutableSupplier.getFromResource("ffmpeg").get().getPath());;
        FonteoAPI.ffprobe = new FFprobe(ExecutableSupplier.getFromResource("ffprobe").get().getPath());;

        FonteoAPI.executor = new FFmpegExecutor(ffmpeg, ffprobe);

        return true;
    }

    public static boolean init(String ffmpeg, String ffprobe) throws IOException {
        return init(ExecutableSupplier.getFFmpeg(ffmpeg), ExecutableSupplier.getFFprobe(ffprobe));
    }

    public enum FlattenVideoType {
        CHAR_CODE, SIMPLE
    }

    public static void processVideo(File video, File folder, String format, String extension, int fps, int width, int height, int columns, int rows) {
        processVideo(video, folder, format, extension, fps, width, height, columns, rows, null);
    }

    public static void processVideo(File video, File folder, String format, String extension, int fps, int width, int height, int columns, int rows, FlattenVideoType flattenType) {

        if (flattenType != null) {
            format = "%x-%y-%d";
        }

        folder.delete();
        folder.mkdirs();

        FFmpegProbeResult tempIn = null;
        try {
            tempIn = ffprobe.probe(video.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final FFmpegProbeResult in = tempIn;

        int tileWidth = width / columns;
        int tileHeight = height / rows;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {

                System.out.println("Doing: x=" + x + " and y=" + y);

                FFmpegBuilder builder = new FFmpegBuilder()
                        .setInput(in)
                        .addOutput(new File(folder, format.replaceAll("%x", String.valueOf(x)).replaceAll("%y", String.valueOf(y)) + "." + extension).getAbsolutePath())
                        .setFormat("image2")
                        .setVideoFilter("scale=" + width + ":" + height + ",crop=" + tileWidth + ":" + tileHeight + ":" + tileWidth * x + ":" + tileHeight * y)
                        .setVideoFrameRate(fps)
                        .done();
                FFmpegJob job = executor.createJob(builder, new ProgressListener() {
                    final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);
                    @Override
                    public void progress(Progress progress) {
                        double percentage = progress.out_time_ns / duration_ns;

                        if (percentage > 3) {
                            percentage = 0;
                        } else if (percentage > 1) {
                            percentage = 1;
                        }

                        System.out.printf(
                                "%.0f%%\n",
                                percentage * 100
                        );
                    }
                });

                job.run();

            }
        }

        flattenFolder(video, folder, extension, columns, rows, flattenType);
    }

    private static void flattenFolder(File video, File folder, String extension, int columns, int rows, FlattenVideoType flattenType) {

        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("Folder is empty.");
            return;
        }

        if (flattenType == FlattenVideoType.CHAR_CODE) {
            flattenCharCode(extension, columns, rows, files);
        } else if (flattenType == FlattenVideoType.SIMPLE) {
            flattenSimple(video, extension, columns, rows, files);
        }
    }

    private static void flattenSimple(File video, String extension, int columns, int rows, File[] files) {
        String name = video.getName().substring(0, video.getName().length() - 4);

        for (File file : files) {
            String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));

            String[] frameData = fileName.split("-");

            int x = Integer.parseInt(frameData[0]);
            int y = Integer.parseInt(frameData[1]);
            int frame = Integer.parseInt(frameData[2]) - 1;


            String newFileName = name + "-" + ((frame * rows * columns) + x + (y * columns) + 1) + "." + extension;
            Path source = file.toPath();
            Path target = file.getParentFile().toPath().resolve(newFileName);

            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Failed to rename file: " + fileName);
                e.printStackTrace();
            }
        }
    }

    private static void flattenCharCode(String extension, int columns, int rows, File[] files) {
        for (File file : files) {
            String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));

            String[] frameData = fileName.split("-");

            int x = Integer.parseInt(frameData[0]);
            int y = Integer.parseInt(frameData[1]);
            int frame = Integer.parseInt(frameData[2]) - 1;


            String newFileName = "#" + getCharacterCode((frame * rows * columns) + x + (y * columns) + 1) + "." + extension;
            Path source = file.toPath();
            Path target = file.getParentFile().toPath().resolve(newFileName);

            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Failed to rename file: " + fileName);
                e.printStackTrace();
            }
        }
    }

    public static String getCharacterCode(int image) {
        if (image >= 1000 && image <= 9999) {
            return "u" + image;
        } else if (image >= 100 && image <= 999) {
            return "ub" + image;
        } else if (image >= 10 && image <= 99) {
            return "ucc" + image;
        } else if (image >= 1 && image <= 9) {
            return "uccc" + image;
        } else {
            return String.valueOf(image);
        }
    }
}
