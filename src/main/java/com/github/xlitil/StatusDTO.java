package com.github.xlitil;

public class StatusDTO {

    private String expectationsDirectory;
    private Mode mode;
    private int nbExpectations;

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

    public int getNbExpectations() {
        return nbExpectations;
    }

    public void setNbExpectations(int nbExpectations) {
        this.nbExpectations = nbExpectations;
    }
}
