package com.github.xlitil;

import org.mockserver.integration.ClientAndProxy;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class MockPlayer {
    private final int port;
    private final String expectationsDirectory;

    ClientAndProxy proxy;

    private static ExpectationDetail[] expectationDetails = new ExpectationDetail[] {new ExpectationDetailSoap()};

    public MockPlayer(int port, String expectationsDirectory) {
        this.port = port;
        this.expectationsDirectory = expectationsDirectory;
    }

    public void start(boolean enableHeaderMatching, boolean enableBodyMatching) throws IOException {
        proxy = ClientAndProxy.startClientAndProxy(port);

        List<Expectation> loadedExpectations = ExpectationUtil.loadExpectations(expectationsDirectory);
        for(Expectation loadedExpectation:loadedExpectations) {

            String contentLengthStr = loadedExpectation.getHttpResponse().getFirstHeader("content-length");
            if (contentLengthStr != null) {
                long currentContentLength = Long.parseLong(contentLengthStr);
                int expectedContentLength = loadedExpectation.getHttpResponse().getBody().getRawBytes().length;
                boolean invalidContentLength = currentContentLength != expectedContentLength;
                if (invalidContentLength) {
                    System.err.println(
                            loadedExpectation.getHttpRequest().getFirstHeader("host") + " - " + loadedExpectation.getHttpRequest().getPath() + " - " +
                            "Invalid value for content-length header, current : " + currentContentLength + ", expected : " + expectedContentLength + ". Please fix the value");
                    loadedExpectation.getHttpResponse().replaceHeader("Content-Length", "" + expectedContentLength);
                }
            }

            HttpRequest request = HttpRequest.request();
            request
                    .withSecure(HttpRequest.request().isSecure())
                    .withMethod(loadedExpectation.getHttpRequest().getMethod())
                    .withPath(loadedExpectation.getHttpRequest().getPath())
                    .withQueryStringParameters(loadedExpectation.getHttpRequest().getQueryStringParameters())
                    .withKeepAlive(loadedExpectation.getHttpRequest().isKeepAlive());
            if (enableHeaderMatching) {
                request
                        .withHeaders(loadedExpectation.getHttpRequest().getHeaders())
                        .withCookies(loadedExpectation.getHttpRequest().getCookies());
            }
            if (enableBodyMatching) {
                request.withBody(loadedExpectation.getHttpRequest().getBody());
            }

            loadedExpectation.getHttpResponse().replaceHeader("x-pms-id", "" + UUID.randomUUID());

            for (ExpectationDetail expectationDetail:expectationDetails) {
                String detail = expectationDetail.getDetail(loadedExpectation);
                if (detail != null) {
                    loadedExpectation.getHttpResponse().replaceHeader("x-pms-detail", detail);
                    break;
                }
            }

            proxy
                    .when(request, loadedExpectation.getTimes(), loadedExpectation.getTimeToLive())
                    .respond(loadedExpectation.getHttpResponse());
        }

    }

    public void stop() {
        proxy.stop();
    }

}
