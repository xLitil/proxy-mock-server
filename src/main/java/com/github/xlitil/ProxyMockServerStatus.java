package com.github.xlitil;

public class ProxyMockServerStatus {

    private String expectationsDirectory;
    private Mode currentMode;

    public String getExpectationsDirectory() {
        return expectationsDirectory;
    }

    public void setExpectationsDirectory(String expectationsDirectory) {
        this.expectationsDirectory = expectationsDirectory;
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(Mode currentMode) {
        this.currentMode = currentMode;
    }
}
