package com.github.xlitil;

import com.github.xlitil.model.Mode;

public class ProxyMockServerStatus {

    private String expectationsDirectory;
    private Mode currentMode;
    private boolean enableBodyMatching = false;
    private boolean enableHeaderMatching = false;

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

    public boolean isEnableBodyMatching() {
        return enableBodyMatching;
    }

    public void setEnableBodyMatching(boolean enableBodyMatching) {
        this.enableBodyMatching = enableBodyMatching;
    }

    public boolean isEnableHeaderMatching() {
        return enableHeaderMatching;
    }

    public void setEnableHeaderMatching(boolean enableHeaderMatching) {
        this.enableHeaderMatching = enableHeaderMatching;
    }
}
