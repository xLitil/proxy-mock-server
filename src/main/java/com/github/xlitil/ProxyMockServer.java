package com.github.xlitil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xlitil.model.EditedMockDTO;
import com.github.xlitil.model.ExpectationDTO;
import com.github.xlitil.model.Mode;
import com.github.xlitil.model.StatusDTO;
import org.apache.commons.cli.*;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpClassCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProxyMockServer {
    public static final String DEFAULT_EXPECTATIONS_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir") + "/mocks").toString();
    public static final String DEFAULT_MOCK_SET = "default";

    private static int port;

    private static MockPlayer mockPlayer;

    private static ProxyMockServerStatus status;

    public static void main(String[] args) throws Exception {
        ConfigurationProperties.maxExpectations(10000);

        Options options = getCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            port = SocketUtil.getFreePort(
                    Integer.parseInt(cmd.getOptionValue("port")),
                    Integer.parseInt(cmd.getOptionValue("port")));

            status = new ProxyMockServerStatus();

            status.setRootExpectations(cmd.getOptionValue("expectationsPath", DEFAULT_EXPECTATIONS_DIRECTORY));
            status.setCurrentMockSet(cmd.getOptionValue("mockSet", DEFAULT_MOCK_SET));
            Set<File> subdirectories = FileUtil.findSubdirectories(status.getRootExpectations());
            status.setMocksSet(subdirectories
                    .stream()
                    .map(f -> f.getName())
                    .collect(Collectors.toSet()));
            status.getMocksSet().add(status.getCurrentMockSet());

            status.setCurrentMode(Mode.PLAY);
            mockPlayer = new MockPlayer(port);
            mockPlayer.start(status.getExpectationsDirectory(), status.isEnableHeaderMatching(), status.isEnableBodyMatching());
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
        options.addOption(
                Option.builder("m")
                        .hasArg()
                        .argName("mockSet")
                        .longOpt("mockSet")
                        .desc("Mock set used (sub path of expectationsPath), default : " + DEFAULT_MOCK_SET)
                        .build()
        );
        return options;
    }


    public static void registerCommands(ClientAndServer abstractClient) {
        HttpClassCallback httpClassCallback = HttpClassCallback.callback()
                .withCallbackClass(CommandCallback.class.getName());
        abstractClient
                .when(
                        HttpRequest.request()
                                .withPath("/proxy-mock-server/.*")
                )
                .respond(httpClassCallback);
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


    public static class CommandCallback implements ExpectationResponseCallback {

        @Override
        public HttpResponse handle(HttpRequest httpRequest) {
            if (httpRequest.getPath().getValue().endsWith("/record")) {
                if (status.getCurrentMode() != Mode.RECORD) {
                    status.setCurrentMode(Mode.RECORD);

                    mockPlayer.proxy.clear(null);
                    registerCommands(mockPlayer.proxy);
                }
                StatusDTO statusDTO = getStatusDTO();
                return sendJson(statusDTO);

            } else if (httpRequest.getPath().getValue().endsWith("/play")) {
                if (status.getCurrentMode() != Mode.PLAY) {
                    status.setCurrentMode(Mode.PLAY);
                    try {
                        ExpectationUtil.saveExpectation(mockPlayer.proxy, status.getExpectationsDirectory());
                        mockPlayer.proxy.clear(null);
                        mockPlayer.loadExpectations(status.getExpectationsDirectory(), status.isEnableHeaderMatching(), status.isEnableBodyMatching());
                        registerCommands(mockPlayer.proxy);
                    } catch (IOException e) {
                        throw new RuntimeException("Aïe", e);
                    }
                } else {
                    restartMockPlayer();
                }
                StatusDTO statusDTO = getStatusDTO();
                return sendJson(statusDTO);

            } else if (httpRequest.getPath().getValue().endsWith("/status")) {
                StatusDTO statusDTO = getStatusDTO();
                return sendJson(statusDTO);

            } else if (httpRequest.getPath().getValue().endsWith("/updateStatus")) {
                Boolean enableHeaderMatching = Boolean.valueOf(httpRequest.getFirstQueryStringParameter("enableHeaderMatching"));
                Boolean enableBodyMatching = Boolean.valueOf(httpRequest.getFirstQueryStringParameter("enableBodyMatching"));
                String mockSet = httpRequest.getFirstQueryStringParameter("mockSet");
                if (enableHeaderMatching != status.isEnableHeaderMatching()
                        || enableBodyMatching != status.isEnableBodyMatching()
                        || !status.getCurrentMockSet().equals(mockSet)) {
                    status.setEnableHeaderMatching(enableHeaderMatching);
                    status.setEnableBodyMatching(enableBodyMatching);
                    status.setCurrentMockSet(mockSet);
                    status.getMocksSet().add(mockSet);

                    restartMockPlayer();
                }

                StatusDTO statusDTO = getStatusDTO();
                return sendJson(statusDTO);

            } else if (httpRequest.getPath().getValue().endsWith("/expectations/edit")) {
                String filename = httpRequest.getFirstQueryStringParameter("filename");
                try {
                    String mock = FileUtil.readFile(Paths.get(filename).toFile());
                    EditedMockDTO editedMockDTO = new EditedMockDTO(
                            filename,
                            mock
                    );
                    return sendJson(editedMockDTO);
                } catch (IOException e) {
                    e.printStackTrace();
                    return HttpResponse.response()
                            .withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code());
                }


            } else if (httpRequest.getPath().getValue().endsWith("/expectations/update")) {
                String id = httpRequest.getFirstQueryStringParameter("id");
                String filename = httpRequest.getFirstQueryStringParameter("filename");
                try {
                    FileUtil.saveFile(
                            Paths.get(filename).toFile(),
                            httpRequest.getBodyAsString());

                    restartMockPlayer();
                    StatusDTO statusDTO = getStatusDTO();
                    return sendJson(statusDTO);
                } catch (IOException e) {
                    e.printStackTrace();
                    return HttpResponse.response()
                            .withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code());
                }


            } else if (httpRequest.getPath().getValue().startsWith("/proxy-mock-server")) {
                String wwwPath = httpRequest.getPath().getValue().replaceAll("/proxy-mock-server/(.*)$", "www/$1");
                try {
                    String fileContent = FileUtil.readFileFromClasspath(wwwPath);
                    return HttpResponse.response()
                            .withStatusCode(HttpStatusCode.OK_200.code())
                            .withBody(fileContent);
                } catch (FileNotFoundException e) {
                    return HttpResponse.response()
                            .withStatusCode(HttpStatusCode.NOT_FOUND_404.code());
                }

            } else {
                return HttpResponse
                        .notFoundResponse()
                        .withBody("Command not found");
            }
        }

        private void restartMockPlayer() {
            try {
                mockPlayer.proxy.clear(null);
                mockPlayer.loadExpectations(status.getExpectationsDirectory(), status.isEnableHeaderMatching(), status.isEnableBodyMatching());
                registerCommands(mockPlayer.proxy);
            } catch (IOException e) {
                throw new RuntimeException("Aïe", e);
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
            statusDTO.setCurrentMockSet(status.getCurrentMockSet());
            statusDTO.setMocksSet(status.getMocksSet());
            statusDTO.setEnableHeaderMatching(status.isEnableHeaderMatching());
            statusDTO.setEnableBodyMatching(status.isEnableBodyMatching());


            List<ExpectationDTO> activeExpectationsDTO = new ArrayList<>();
            List<Expectation> activeExpectations = ExpectationUtil.getActivesExpectations(mockPlayer.proxy, null);
            int i = 0;
            for (Expectation expectation:activeExpectations) {
                ExpectationDTO expectationDTO = new ExpectationDTO();
                expectationDTO.setIndex(i);
                expectationDTO.setId(expectation.getHttpResponse().getFirstHeader("x-pms-id"));
                expectationDTO.setHost(expectation.getHttpResponse().getFirstHeader("x-pms-request-host"));
                expectationDTO.setPath(expectation.getHttpRequest().getPath().getValue());
                expectationDTO.setFilename(
                        Paths.get(
                                status.getExpectationsDirectory(),
                                expectation.getHttpResponse().getFirstHeader("x-pms-filename")).toString()
                );
                expectationDTO.setProtocol(expectation.getHttpRequest().isSecure() != null && expectation.getHttpRequest().isSecure() ? "HTTPS" : "HTTP");
                expectationDTO.setDetail(expectation.getHttpResponse().getFirstHeader("x-pms-detail"));

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
                if (expectation.getHttpResponse().getFirstHeader("x-pms-filename") != null) {
                    expectationDTO.setFilename(
                            Paths.get(
                                    status.getExpectationsDirectory(),
                                    expectation.getHttpResponse().getFirstHeader("x-pms-filename")).toString()
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
