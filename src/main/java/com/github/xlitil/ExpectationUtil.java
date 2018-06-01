package com.github.xlitil;

import org.mockserver.client.AbstractClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.mock.Expectation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExpectationUtil {

    public static void saveExpectation(Expectation expectation, Path filename) throws IOException {
        File file = filename.toFile();

        ExpectationSerializer expectationSerializer = new ExpectationSerializer();
        String serialize = expectationSerializer.serialize(expectation);
        FileUtil.saveFile(file, serialize);
    }

    public static List<Expectation> loadExpectations(String expectationsDirectory) throws IOException {
        ExpectationSerializer expectationSerializer = new ExpectationSerializer();
        List<File> files = new ArrayList<>();
        FileUtil.search(expectationsDirectory, ".*\\.expectation\\.current\\.json", files);

        List<Expectation> loadedExpectations = new ArrayList<>();
        for (File file:files) {
            String json = FileUtil.readFile(file);
            loadedExpectations.add(expectationSerializer.deserialize(json));
        }

        return loadedExpectations;
    }

    public static List<Expectation> getActivesExpectations(AbstractClient client) {
        Expectation[] allActiveExpectations = client.retrieveActiveExpectations(null);

        List<Expectation> expectations = new ArrayList<>();
        for(Expectation expectation:allActiveExpectations) {
            if (expectation.getHttpClassCallback() != null || "true".equals(expectation.getHttpResponse().getFirstHeader("x-pms-exclude-from-expectations"))) {
                continue;
            }

            expectations.add(expectation);

        }

        return expectations;
    }

    public static List<Expectation> getRecordedExpectations(AbstractClient client) {
        Expectation[] allActiveExpectations = client.retrieveRecordedExpectations(null);

        List<Expectation> expectations = new ArrayList<>();
        for(Expectation expectation:allActiveExpectations) {
            if (expectation.getHttpClassCallback() != null || "true".equals(expectation.getHttpResponse().getFirstHeader("x-pms-exclude-from-expectations"))) {
                continue;
            }

            expectations.add(expectation);

        }

        return expectations;
    }

}
