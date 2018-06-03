package com.github.xlitil;

import org.mockserver.integration.ClientAndProxy;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.NottableString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MockRecorder {

    private int port;
    private final String expectationsDirectory;

    ClientAndProxy proxy;

    public MockRecorder(int port, String expectationsDirectory) {
        this.port = port;
        this.expectationsDirectory = expectationsDirectory;
    }

    public void start() {
        proxy = ClientAndProxy.startClientAndProxy(port);
    }

    public void stop() throws IOException {
        saveExpectation(expectationsDirectory);
        proxy.stop();
    }

    private void saveExpectation(String expectationsDirectory) throws IOException {
        Expectation[] expectations = proxy.retrieveRecordedExpectations(HttpRequest.request());

        int expectationOrder = 1;
        for(Expectation expectation:expectations) {
            if (expectation.getHttpClassCallback() != null) {
                continue;
            }


            String rqtHost = getFirstHeaderValue(expectation.getHttpRequest(), "host", "unknown");
            NottableString rqtPath = expectation.getHttpRequest().getPath();

            String filename = FileUtil.escapeFileName(expectationOrder + "_" + FileUtil.escapeFileName(rqtHost) + "_" + rqtPath.getValue()) + ".expectation.current.json";
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

    private String getFirstHeaderValue(HttpRequest request, String headerName, String valueIfNodFound) {
        String firstHeaderValue = request.getFirstHeader(headerName);
        if (firstHeaderValue == null || firstHeaderValue.isEmpty())
            firstHeaderValue = valueIfNodFound;
        return firstHeaderValue;
    }

}
