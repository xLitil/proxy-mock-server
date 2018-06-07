package com.github.xlitil;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileUtil {
    public static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

    public static void search(String directoryName, String regex, List<File> filesFound) {
        File directory = new File(directoryName);

        File[] fList = directory.listFiles();

        if (fList == null) {
            return;
        }

        for (File file : fList) {
            if (file.isFile()) {
                if (file.getName().matches(regex)) {
                    filesFound.add(file);
                }
            } else if (file.isDirectory()) {
                search(file.getAbsolutePath(), regex, filesFound);
            }
        }
    }

    public static Set<File> findSubdirectories(String directoryName) {
        Set<File> subDirectories = new HashSet<>();
        File directory = new File(directoryName);

        File[] fList = directory.listFiles();

        if (fList == null) {
            return subDirectories;
        }

        for (File file : fList) {
            if (file.isDirectory()) {
                subDirectories.add(file);
            }
        }
        return subDirectories;
    }

    public static String readFile(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        return new String(encoded, DEFAULT_ENCODING);
    }

    public static void saveFile(File file, String content) throws IOException {
        // Create whole path automatically when writing to a new file
        file.getParentFile().mkdirs();

        Files.write(file.toPath(), content.getBytes(DEFAULT_ENCODING), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static String escapeFileName(String name) {
        return name
                .replaceAll("[^A-Za-z0-9\\.]", "_");
    }

    public static String readFileFromClasspath(String filename) throws FileNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream fileStream = classLoader.getResourceAsStream(filename);
        if (fileStream == null) {
            throw new FileNotFoundException("Fichier " + filename + " non trouvé dans le classpath");
        }
        try {
            return inputStreamToString(fileStream, Charset.forName("utf-8"));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le fichier depuis le classpath", e);
        }
    }

    private static String inputStreamToString(InputStream inputStream, Charset charset) throws IOException {
        try(ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString(charset.name());
        }
    }
}
