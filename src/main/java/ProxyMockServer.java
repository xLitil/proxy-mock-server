import org.apache.commons.cli.*;
import org.mockserver.client.AbstractClient;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.action.ExpectationCallback;
import org.mockserver.model.HttpClassCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;

import java.io.IOException;
import java.nio.file.Paths;

public class ProxyMockServer {
    public static final String DEFAULT_EXPECTATIONS_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir") + "/mocks").toString();
    private static int port;
    private static String expectationsDirectory;

    private static MockRecorder mockRecorder;
    private static MockPlayer mockPlayer;

    private static Mode currentMode;

    public static void main(String[] args) throws Exception {
        Options options = getCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            port = SocketUtil.getFreePort(
                    Integer.parseInt(cmd.getOptionValue("port")),
                    Integer.parseInt(cmd.getOptionValue("port")));
            expectationsDirectory = cmd.getOptionValue("expectationsPath", DEFAULT_EXPECTATIONS_DIRECTORY);

            currentMode = Mode.PLAY;
            mockPlayer = new MockPlayer(port, expectationsDirectory);
            mockPlayer.start();
            registerCommands(mockPlayer.mockServer);

        } catch (MissingOptionException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "proxy-mock-server", options );
            System.exit(1);
        }

    }

    private static Options getCommandLineOptions() {
        Options options = new Options();
        options.addOption(
            Option.builder("h")
                    .longOpt("help")
                    .desc("Print help")
                    .build()
        );
        options.addOption(
                Option.builder("p")
                        .hasArg()
                        .argName("portNumber")
                        .required()
                        .longOpt("port")
                        .desc("Listening port")
                        .build()
        );
        options.addOption(
                Option.builder("e")
                        .hasArg()
                        .argName("expectationsPath")
                        .longOpt("expectationsPath")
                        .desc("Path used to store expectation, default : " + DEFAULT_EXPECTATIONS_DIRECTORY)
                        .build()
        );
        return options;
    }


    public static void registerCommands(AbstractClient abstractClient) {
        HttpClassCallback httpClassCallback = HttpClassCallback.callback()
                .withCallbackClass(CommandCallback.class.getName());
        abstractClient
                .when(
                        HttpRequest.request()
                                .withPath("/mock/command/.*")
                )
                .callback(httpClassCallback);
        abstractClient
                .when(
                        HttpRequest.request()
                                .withPath("/")
                )
                .callback(httpClassCallback);

    }


    public static class CommandCallback implements ExpectationCallback {

        @Override
        public HttpResponse handle(HttpRequest httpRequest) {
            if (
                    "/".equals(httpRequest.getPath().getValue())
                    || httpRequest.getPath().getValue().endsWith("help")) {
                return HttpResponse.response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(
                                FileUtil.readFileFromClasspath("help.txt")
                        );

            } else if (httpRequest.getPath().getValue().endsWith("/record")) {
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

            } else if (httpRequest.getPath().getValue().endsWith("/play")) {
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
