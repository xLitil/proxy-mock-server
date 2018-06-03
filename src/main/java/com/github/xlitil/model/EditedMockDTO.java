package com.github.xlitil.model;

public class EditedMockDTO {

    private String mockFilename;
    private String mock;

    public EditedMockDTO(String mockFilename, String mock) {
        this.mockFilename = mockFilename;
        this.mock = mock;
    }

    public String getMockFilename() {
        return mockFilename;
    }

    public void setMockFilename(String mockFilename) {
        this.mockFilename = mockFilename;
    }

    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        this.mock = mock;
    }
}
