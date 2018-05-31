package com.github.xlitil;

import java.util.List;

public class StatusDTO {

    private String expectationsDirectory;
    private Mode mode;
    private List<ExpectationDTO> expectations;

    public String getExpectationsDirectory() {
        return expectationsDirectory;
    }

    public void setExpectationsDirectory(String expectationsDirectory) {
        this.expectationsDirectory = expectationsDirectory;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public List<ExpectationDTO> getExpectations() {
        return expectations;
    }

    public void setExpectations(List<ExpectationDTO> expectations) {
        this.expectations = expectations;
    }
}
