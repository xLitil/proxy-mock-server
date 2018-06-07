package com.github.xlitil.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatusDTO {

    private String expectationsDirectory;
    private String currentMockSet;
    private Set<String> mocksSet = new HashSet<>();

    private Mode mode;
    private boolean enableHeaderMatching;
    private boolean enableBodyMatching;

    private List<ExpectationDTO> activeExpectations;
    private List<ExpectationDTO> recordedExpectations;


    public String getExpectationsDirectory() {
        return expectationsDirectory;
    }

    public void setExpectationsDirectory(String expectationsDirectory) {
        this.expectationsDirectory = expectationsDirectory;
    }

    public String getCurrentMockSet() {
        return currentMockSet;
    }

    public void setCurrentMockSet(String currentMockSet) {
        this.currentMockSet = currentMockSet;
    }

    public Set<String> getMocksSet() {
        return mocksSet;
    }

    public void setMocksSet(Set<String> mocksSet) {
        this.mocksSet = mocksSet;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isEnableHeaderMatching() {
        return enableHeaderMatching;
    }

    public void setEnableHeaderMatching(boolean enableHeaderMatching) {
        this.enableHeaderMatching = enableHeaderMatching;
    }

    public boolean isEnableBodyMatching() {
        return enableBodyMatching;
    }

    public void setEnableBodyMatching(boolean enableBodyMatching) {
        this.enableBodyMatching = enableBodyMatching;
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
