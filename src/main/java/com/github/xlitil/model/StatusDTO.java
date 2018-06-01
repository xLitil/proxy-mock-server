package com.github.xlitil.model;

import java.util.List;

public class StatusDTO {

    private String expectationsDirectory;
    private Mode mode;
    private List<ExpectationDTO> activeExpectations;
    private List<ExpectationDTO> recordedExpectations;

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

    public List<ExpectationDTO> getActiveExpectations() {
        return activeExpectations;
    }

    public void setActiveExpectations(List<ExpectationDTO> activeExpectations) {
        this.activeExpectations = activeExpectations;
    }

    public List<ExpectationDTO> getRecordedExpectations() {
        return recordedExpectations;
    }

    public void setRecordedExpectations(List<ExpectationDTO> recordedExpectations) {
        this.recordedExpectations = recordedExpectations;
    }
}
