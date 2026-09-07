package com.sap.adapter.generator.model.build;

import java.util.ArrayList;
import java.util.List;

public class InspectionReport {
    private boolean overallSuccess;
    private int totalChecks;
    private int passedChecks;
    private int failedChecks;
    private List<InspectionCheckItem> checkItems = new ArrayList<>();
    private String esaPath;
    private String jarPath;

    public InspectionReport() {}

    public boolean isOverallSuccess() { return overallSuccess; }
    public void setOverallSuccess(boolean overallSuccess) { this.overallSuccess = overallSuccess; }

    public int getTotalChecks() { return totalChecks; }
    public void setTotalChecks(int totalChecks) { this.totalChecks = totalChecks; }

    public int getPassedChecks() { return passedChecks; }
    public void setPassedChecks(int passedChecks) { this.passedChecks = passedChecks; }

    public int getFailedChecks() { return failedChecks; }
    public void setFailedChecks(int failedChecks) { this.failedChecks = failedChecks; }

    public List<InspectionCheckItem> getCheckItems() { return checkItems; }
    public void setCheckItems(List<InspectionCheckItem> checkItems) { this.checkItems = checkItems; }

    public String getEsaPath() { return esaPath; }
    public void setEsaPath(String esaPath) { this.esaPath = esaPath; }

    public String getJarPath() { return jarPath; }
    public void setJarPath(String jarPath) { this.jarPath = jarPath; }

    public void addCheck(int number, String name, boolean passed, String details) {
        checkItems.add(new InspectionCheckItem(number, name, passed, details));
        totalChecks++;
        if (passed) {
            passedChecks++;
        } else {
            failedChecks++;
        }
        overallSuccess = (failedChecks == 0);
    }
}
