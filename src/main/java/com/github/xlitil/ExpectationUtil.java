package com.github.xlitil;

import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.NottableString;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ExpectationUtil {

    public static void saveExpectation(ClientAndServer proxy, String expectationsDirectory) throws IOException {
        Expectation[] expectations = proxy.retrieveRecordedExpectations(HttpRequest.request());

        int expectationOrder = 1;
        for(Expectation expectation:expectations) {
            if (expectation.getHttpResponseClassCallback() != null) {
                continue;
            }


            String rqtHost = getFirstHeaderValue(expectation.getHttpRequest(), "host", "unknown");
            NottableString rqtPath = expectation.getHttpRequest().getPath();

            String filename = FileUtil.escapeFileName(String.format("%03d", expectationOrder ) + "_" + FileUtil.escapeFileName(rqtHost) + "_" + rqtPath.getValue()) + ".expectation.current.json";
            expectation.getHttpResponse().replaceHeader("x-pms-filename", filename);

            Path filenamePath = Paths.get(
                    expectationsDirectory,
                    filename);

            if (Files.exists(filenamePath)) {
                Files.copy(filenamePath,
                        filenamePath.resolveSibling(FileUtil.escapeFileName(rqtPath.getValue()) + ".expectation.previous.json"),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            ExpectationUtil.saveExpectation(expectation, filenamePath);
            expectationOrder++;
        }
    }

    public static String getFirstHeaderValue(HttpRequest request, String headerName, String valueIfNodFound) {
        String firstHeaderValue = request.getFirstHeader(headerName);
        if (firstHeaderValue == null || firstHeaderValue.isEmpty())
            firstHeaderValue = valueIfNodFound;
        return firstHeaderValue;
    }

    public static void saveExpectation(Expectation expectation, Path filename) throws IOException {
        File file = filename.toFile();

        ExpectationSerializer expectationSerializer = new ExpectationSerializer(new MockServerLogger());
        String serialize = expectationSerializer.serialize(expectation);
        FileUtil.saveFile(file, serialize);
    }

    public static List<Expectation> loadExpectations(String expectationsDirectory) throws IOException {
        ExpectationSerializer expectationSerializer = new ExpectationSerializer(new MockServerLogger());
        List<File> files = new ArrayList<>();
        FileUtil.search(expectationsDirectory, ".*\\.expectation\\.current\\.json", files);

        List<Expectation> loadedExpectations = new ArrayList<>();
        for (File file:files) {
            String json = FileUtil.readFile(file);

            Expectation expectation = expectationSerializer.deserialize(json);
            expectation.getHttpResponse().replaceHeader("x-pms-filename", file.getName());
            expectation.getHttpResponse().replaceHeader("x-pms-request-host", expectation.getHttpRequest().getFirstHeader("host"));

            loadedExpectations.add(expectation);
        }

        return loadedExpectations;
    }

    public static List<Expectation> getActivesExpectations(ClientAndServer client, HttpRequest httpRequest) {
        Expectation[] allActiveExpectations = client.retrieveActiveExpectations(httpRequest);

        List<Expectation> expectations = new ArrayList<>();
        for(Expectation expectation:allActiveExpectations) {
            if (expectation.getHttpResponseClassCallback() != null || "true".equals(expectation.getHttpResponse().getFirstHeader("x-pms-exclude-from-expectations"))) {
                continue;
            }

            expectations.add(expectation);

        }

        return expectations;
    }

    public static List<Expectation> getRecordedExpectations(ClientAndServer client) {
        Expectation[] allActiveExpectations = client.retrieveRecordedExpectations(null);

        List<Expectation> expectations = new ArrayList<>();
        for(Expectation expectation:allActiveExpectations) {
            if (expectation.getHttpResponseClassCallback() != null || "true".equals(expectation.getHttpResponse().getFirstHeader("x-pms-exclude-from-expectations"))) {
                continue;
            }

            expectations.add(expectation);

        }

        return expectations;
    }

    public static Expectation getExpectation(ClientAndServer client, String id) {
        List<Expectation> activeExpectations = ExpectationUtil.getActivesExpectations(
                client, null);

        Expectation found = null;
        for (Expectation expectation:activeExpectations) {
            if (id.equals(expectation.getHttpResponse().getFirstHeader("x-pms-id"))) {
                found = expectation;
                break;
            }
        }

        return found;
    }


}
