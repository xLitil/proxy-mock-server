package com.github.xlitil;

import org.apache.commons.io.FileUtils;
import org.mockserver.client.serialization.ExpectationSerializer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

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

    public static String readFile(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        return new String(encoded, DEFAULT_ENCODING);
    }

    public static void saveFile(File file, String content) throws IOException {
        // Create whole path automatically when writing to a new file
        file.getParentFile().mkdirs();

        Files.write(file.toPath(), content.getBytes(DEFAULT_ENCODING), StandardOpenOption.CREATE);
    }

    public static String escapeFileName(String name) {
        return name
                .replaceAll("[^A-Za-z0-9\\.]", "_");
    }

    public static String readFileFromClasspath(String filename) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader.getResource(filename) == null) {
            throw new RuntimeException("Fichier " + filename + " non trouvé");
        }
        File file = new File(classLoader.getResource(filename).getFile());
        try {
            return FileUtils.readFileToString(file, Charset.forName("utf-8"));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le fichier depuis le classpath", e);
        }
    }
}