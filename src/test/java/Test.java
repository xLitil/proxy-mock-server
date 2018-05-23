import org.mockserver.client.AbstractClient;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.action.ExpectationCallback;
import org.mockserver.model.HttpClassCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;

import java.io.IOException;

public class Test {

    private static int port;
    private static String expectationsDirectory;

    private static MockRecorder mockRecorder;
    private static MockPlayer mockPlayer;

    private static Mode currentMode;

    public static void main(String[] args) throws IOException {
        port = SocketUtil.getFreePort(8082, 8083);
        expectationsDirectory = System.getProperty("java.io.tmpdir");

        currentMode = Mode.PLAY;
        mockPlayer = new MockPlayer(port, expectationsDirectory);
        mockPlayer.start();
        registerCommands(mockPlayer.mockServer);

    }


    public static void registerCommands(AbstractClient abstractClient) {
        abstractClient
                .when(
                        HttpRequest.request()
                                .withPath("/mock/command/.*")
                )
                .callback(
                        HttpClassCallback.callback()
                                .withCallbackClass("Test$CommandCallback")
                );

    }


    public static class CommandCallback implements ExpectationCallback {

        @Override
        public HttpResponse handle(HttpRequest httpRequest) {
            if (httpRequest.getPath().getValue().endsWith("help")) {
                return HttpResponse.response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(
                                "mock/command/mode/record\n" +
                                "mock/command/mode/play\n" +
                                "mock/command/status\n"
                        );

            } else if (httpRequest.getPath().getValue().endsWith("mode/record")) {
                if (currentMode != Mode.RECORD) {
                    currentMode = Mode.RECORD;

                    mockPlayer.stop();
                    if (mockRecorder == null) {
                        mockRecorder = new MockRecorder(port, expectationsDirectory);

                    }
                    mockRecorder.start();
                    registerCommands(mockRecorder.proxy);
                }
                return HttpResponse.response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(String.format("{ 'mode' : '%s' }", currentMode));

            } else if (httpRequest.getPath().getValue().endsWith("mode/play")) {
                if (currentMode != Mode.PLAY) {
                    currentMode = Mode.PLAY;
                    try {
                        mockRecorder.stop();
                        mockPlayer.start();
                        registerCommands(mockPlayer.mockServer);
                    } catch (IOException e) {
                        throw new RuntimeException("Aïe", e);
                    }
                } else {
                    try {
                        mockPlayer.stop();
                        mockPlayer.start();
                        registerCommands(mockPlayer.mockServer);
                    } catch (IOException e) {
                        throw new RuntimeException("Aïe", e);
                    }
                }
                return HttpResponse.response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(String.format("{ 'mode' : '%s' }", currentMode));

            } else if (httpRequest.getPath().getValue().endsWith("status")) {
                Expectation[] expectations = mockPlayer.mockServer.retrieveRecordedExpectations(HttpRequest.request());
                for(Expectation expectation:expectations) {

                }

                return HttpResponse.response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(String.format("{ 'mode' : '%s' }", currentMode));

            } else {
                return HttpResponse
                        .notFoundResponse()
                        .withBody("Command not found");
            }
        }
    }


}
