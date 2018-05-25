import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;

import java.io.IOException;
import java.util.List;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public class MockPlayer {
    private final int port;
    private final String expectationsDirectory;

    ClientAndServer mockServer;

    public MockPlayer(int port, String expectationsDirectory) {
        this.port = port;
        this.expectationsDirectory = expectationsDirectory;
    }

    public void start() throws IOException {
        mockServer = startClientAndServer(port);

        List<Expectation> loadedExpectations = ExpectationUtil.loadExpectations(expectationsDirectory);
        for(Expectation e:loadedExpectations) {
            mockServer
                    .when(e.getHttpRequest(), e.getTimes(), e.getTimeToLive())
                    .respond(e.getHttpResponse());
        }
    }

    public void stop() {
        mockServer.stop();
    }

}
