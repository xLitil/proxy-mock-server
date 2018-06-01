package com.github.xlitil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xlitil.model.ExpectationDTO;
import com.github.xlitil.model.Mode;
import com.github.xlitil.model.StatusDTO;
import org.apache.commons.cli.*;
import org.mockserver.client.AbstractClient;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.action.ExpectationCallback;
import org.mockserver.model.HttpClassCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProxyMockServer {
    public static final String DEFAULT_EXPECTATIONS_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir") + "/mocks").toString();

    private static int port;

    private static MockRecorder mockRecorder;
    private static MockPlayer mockPlayer;

    private static ProxyMockServerStatus status;

    private static ExpectationDetail[] expectationDetails = new ExpectationDetail[] {new ExpectationDetailSoap()};

    public static void main(String[] args) throws Exception {
        ConfigurationProperties.maxExpectations(1000);

        Options options = getCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            port = SocketUtil.getFreePort(
                    Integer.parseInt(cmd.getOptionValue("port")),
                    Integer.parseInt(cmd.getOptionValue("port")));

            status = new ProxyMockServerStatus();

            status.setExpectationsDirectory(cmd.getOptionValue("expectationsPath", DEFAULT_EXPECTATIONS_DIRECTORY));

            status.setCurrentMode(Mode.PLAY);
            mockPlayer = new MockPlayer(port, status.getExpectationsDirectory());
            mockPlayer.start();
            registerCommands(mockPlayer.proxy);

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
                                .withPath("/proxy-mock-server/.*")
                )
                .callback(httpClassCallback);
        abstractClient
                .when(
                        HttpRequest.request()
                                .withPath("/")
                )
                .respond(
                        HttpResponse.response()
                                .withStatusCode(HttpStatusCode.FOUND_302.code())
                                .withHeader("Location", "/proxy-mock-server/index.html")
                                .withHeader("x-pms-exclude-from-expectations", "true")
                );

    }


    public static class CommandCallback implements ExpectationCallback {

        @Override
        public HttpResponse handle(HttpRequest httpRequest) {
            if ("/".equals(httpRequest.getPath().getValue())) {
                return HttpResponse.response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(
                                FileUtil.readFileFromClasspath("www/index.html")
                        );


            } else if (httpRequest.getPath().getValue().endsWith("/record")) {
                if (status.getCurrentMode() != Mode.RECORD) {
                    status.setCurrentMode(Mode.RECORD);

                    mockPlayer.stop();
                    if (mockRecorder == null) {
                        mockRecorder = new MockRecorder(port, status.getExpectationsDirectory());

                    }
                    mockRecorder.start();
                    registerCommands(mockRecorder.proxy);
                }
                StatusDTO statusDTO = getStatusDTO();
                return sendJson(statusDTO);

            } else if (httpRequest.getPath().getValue().endsWith("/play")) {
                if (status.getCurrentMode() != Mode.PLAY) {
                    status.setCurrentMode(Mode.PLAY);
                    try {
                        mockRecorder.stop();
                        mockPlayer.start();
                        registerCommands(mockPlayer.proxy);
                    } catch (IOException e) {
                        throw new RuntimeException("Aïe", e);
                    }
                } else {
                    try {
                        mockPlayer.stop();
                        mockPlayer.start();
                        registerCommands(mockPlayer.proxy);
                    } catch (IOException e) {
                        throw new RuntimeException("Aïe", e);
                    }
                }
                StatusDTO statusDTO = getStatusDTO();
                return sendJson(statusDTO);

            } else if (httpRequest.getPath().getValue().endsWith("/status")) {
                StatusDTO statusDTO = getStatusDTO();
                return sendJson(statusDTO);

            } else if (httpRequest.getPath().getValue().startsWith("/proxy-mock-server")) {
                String wwwPath = httpRequest.getPath().getValue().replaceAll("/proxy-mock-server/(.*)$", "www/$1");
                return HttpResponse.response()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody(
                                FileUtil.readFileFromClasspath(wwwPath)
                        );

            } else {
                return HttpResponse
                        .notFoundResponse()
                        .withBody("Command not found");
            }
        }

        private HttpResponse sendJson(Object obj) {
            String s = getJsonString(obj);
            return HttpResponse.response()
                    .withStatusCode(HttpStatusCode.OK_200.code())
                    .withHeader("content-type", "application/json")
                    .withBody(s);
        }

        private String getJsonString(Object object) {
            ObjectMapper objectMapper = new ObjectMapper();
            String s = null;
            try {
                s = objectMapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                s="";
            }
            return s;
        }

        private StatusDTO getStatusDTO() {

            StatusDTO statusDTO = new StatusDTO();
            statusDTO.setMode(status.getCurrentMode());
            statusDTO.setExpectationsDirectory(status.getExpectationsDirectory());


            List<ExpectationDTO> activeExpectationsDTO = new ArrayList<>();
            List<Expectation> activeExpectations = ExpectationUtil.getActivesExpectations(mockPlayer.proxy);
            int i = 0;
            for (Expectation expectation:activeExpectations) {
                ExpectationDTO expectationDTO = new ExpectationDTO();
                expectationDTO.setIndex(i);
                expectationDTO.setHost(expectation.getHttpRequest().getFirstHeader("host"));
                expectationDTO.setPath(expectation.getHttpRequest().getPath().getValue());
                expectationDTO.setFilename(
                        Paths.get(
                                status.getExpectationsDirectory(),
                                expectation.getHttpResponse().getFirstHeader("x-mock-filename")).toString()
                );
                expectationDTO.setProtocol(expectation.getHttpRequest().isSecure() != null && expectation.getHttpRequest().isSecure() ? "HTTPS" : "HTTP");

                for (ExpectationDetail expectationDetail:expectationDetails) {
                    String detail = expectationDetail.getDetail(expectation);
                    if (detail != null) {
                        expectationDTO.setDetail(detail);
                        break;
                    }
                }

                activeExpectationsDTO.add(expectationDTO);
                i++;
            }
            statusDTO.setActiveExpectations(activeExpectationsDTO);

            List<ExpectationDTO> recordedExpectationsDTO = new ArrayList<>();
            List<Expectation> recordedExpectations = ExpectationUtil.getRecordedExpectations(mockPlayer.proxy);
            i = 0;
            for (Expectation expectation:recordedExpectations) {
                ExpectationDTO expectationDTO = new ExpectationDTO();
                expectationDTO.setIndex(i);
                expectationDTO.setHost(expectation.getHttpRequest().getFirstHeader("host"));
                expectationDTO.setPath(expectation.getHttpRequest().getPath().getValue());
                if (expectation.getHttpResponse().getFirstHeader("x-mock-filename") != null) {
                    expectationDTO.setFilename(
                            Paths.get(
                                    status.getExpectationsDirectory(),
                                    expectation.getHttpResponse().getFirstHeader("x-mock-filename")).toString()
                    );
                }
                expectationDTO.setProtocol(expectation.getHttpRequest().isSecure() != null && expectation.getHttpRequest().isSecure() ? "HTTPS" : "HTTP");

                recordedExpectationsDTO.add(expectationDTO);
                i++;
            }
            statusDTO.setRecordedExpectations(recordedExpectationsDTO);

            return statusDTO;
        }
    }

}
