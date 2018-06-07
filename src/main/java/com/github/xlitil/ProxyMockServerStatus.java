package com.github.xlitil;

import com.github.xlitil.model.Mode;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class ProxyMockServerStatus {

    private String rootExpectations;
    private String currentMockSet;
    private Set<String> mocksSet = new HashSet<>();

    private Mode currentMode;
    private boolean enableBodyMatching = false;
    private boolean enableHeaderMatching = false;

    public String getExpectationsDirectory() {
        return Paths.get(rootExpectations, currentMockSet).toString();
    }

    public String getRootExpectations() {
        return rootExpectations;
    }

    public void setRootExpectations(String rootExpectations) {
        this.rootExpectations = rootExpectations;
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
