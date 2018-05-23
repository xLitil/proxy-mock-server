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
/*
            System.out.println(String.format("-- rqtHost : %s ---------------------", rqtHost));
            System.out.println(String.format("===> " + fileName));
            System.out.println(expectationSerializer.serialize(expectation));
            System.out.println("__________________________________");
*/

        proxy.stop();
    }

    private void saveExpectation(String expectationsDirectory) throws IOException {
        Expectation[] expectations = proxy.retrieveRecordedExpectations(HttpRequest.request());
        for(Expectation expectation:expectations) {
            if (expectation.getHttpRequest().getPath().getValue().startsWith("/mock/command")) {
                continue;
            }

            String rqtHost = getFirstHeaderValue(expectation, "host", "unknown");
            NottableString rqtPath = expectation.getHttpRequest().getPath();

            Path filename = Paths.get(
                    expectationsDirectory,
                    "mocks",
                    rqtHost,
                    "path_" + FileUtil.escapeFileName(rqtPath.getValue()) + ".expectation.current.json");

            System.out.println("===> " + filename);
            if (Files.exists(filename)) {
                Files.copy(filename,
                        filename.resolveSibling("path_" + FileUtil.escapeFileName(rqtPath.getValue()) + ".expectation.previous.json"),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            ExpectationUtil.saveExpectation(expectation, filename);
        }
    }

    private String getFirstHeaderValue(Expectation e, String headerName, String valueIfNodFound) {
        String firstHeaderValue = e.getHttpRequest().getFirstHeader(headerName);
        if (firstHeaderValue == null || firstHeaderValue.isEmpty())
            firstHeaderValue = valueIfNodFound;
        return firstHeaderValue;
    }

}
