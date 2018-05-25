import org.mockserver.integration.ClientAndProxy;
import org.mockserver.mock.Expectation;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

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

        int expectationOrder = 1;
        for(Expectation expectation:expectations) {
            if (expectation.getHttpRequest().getPath().getValue().startsWith("/mock/command")) {
                continue;
            }


            String rqtHost = getFirstHeaderValue(expectation.getHttpRequest(), "host", "unknown");
            NottableString rqtPath = expectation.getHttpRequest().getPath();

            Path filename = Paths.get(
                    expectationsDirectory,
                    FileUtil.escapeFileName(rqtHost),
                    FileUtil.escapeFileName(expectationOrder + "_" + rqtPath.getValue()) + ".expectation.current.json");

            System.out.println("===> " + filename);
            if (Files.exists(filename)) {
                Files.copy(filename,
                        filename.resolveSibling(FileUtil.escapeFileName(rqtPath.getValue()) + ".expectation.previous.json"),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            ExpectationUtil.saveExpectation(expectation, filename);
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
