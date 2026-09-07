package com.sap.adapter.generator.model.build;

public class InspectionCheckItem {
    private int checkNumber;
    private String checkName;
    private boolean passed;
    private String details;

    public InspectionCheckItem() {}

    public InspectionCheckItem(int checkNumber, String checkName, boolean passed, String details) {
        this.checkNumber = checkNumber;
        this.checkName = checkName;
        this.passed = passed;
        this.details = details;
    }

    public int getCheckNumber() { return checkNumber; }
    public void setCheckNumber(int checkNumber) { this.checkNumber = checkNumber; }

    public String getCheckName() { return checkName; }
    public void setCheckName(String checkName) { this.checkName = checkName; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
