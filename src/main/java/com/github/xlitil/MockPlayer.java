package com.github.xlitil;

import org.mockserver.integration.ClientAndProxy;
import org.mockserver.mock.Expectation;

import java.io.IOException;
import java.util.List;

public class MockPlayer {
    private final int port;
    private final String expectationsDirectory;

    ClientAndProxy proxy;

    public MockPlayer(int port, String expectationsDirectory) {
        this.port = port;
        this.expectationsDirectory = expectationsDirectory;
    }

    public void start() throws IOException {
        proxy = ClientAndProxy.startClientAndProxy(port);

        List<Expectation> loadedExpectations = ExpectationUtil.loadExpectations(expectationsDirectory);
        for(Expectation e:loadedExpectations) {

            String contentLengthStr = e.getHttpResponse().getFirstHeader("content-length");
            if (contentLengthStr != null) {
                long currentContentLength = Long.parseLong(contentLengthStr);
                int expectedContentLength = e.getHttpResponse().getBody().getRawBytes().length;
                boolean invalidContentLength = currentContentLength != expectedContentLength;
                if (invalidContentLength) {
                    System.err.println(
                            e.getHttpRequest().getFirstHeader("host") + " - " + e.getHttpRequest().getPath() + " - " +
                            "Invalid value for content-length header, current : " + currentContentLength + ", expected : " + expectedContentLength + ". Please fix the value");
                    e.getHttpResponse().replaceHeader("Content-Length", "" + expectedContentLength);
                }
            }

            proxy
                    .when(e.getHttpRequest(), e.getTimes(), e.getTimeToLive())
                    .respond(e.getHttpResponse());
        }

    }

    public void stop() {
        proxy.stop();
    }

}
