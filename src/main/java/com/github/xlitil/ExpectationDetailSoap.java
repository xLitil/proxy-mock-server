package com.github.xlitil;

import org.mockserver.mock.Expectation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpectationDetailSoap implements ExpectationDetail {

    private final Pattern p = Pattern.compile(":Body>\\s*<([^:\\ ]+:)?([^\\s]+)");

    @Override
    public String getDetail(Expectation expectation) {
        String bodyAsString = expectation.getHttpRequest().getBodyAsString();

        if (bodyAsString == null || ! bodyAsString.contains("http://schemas.xmlsoap.org/soap/envelope/")) {
            return null;
        }

        Matcher matcher = p.matcher(bodyAsString);
        if (! matcher.find()) {
            return null;
        }

        return "SOAP Method : " + matcher.group(2);
    }

}
