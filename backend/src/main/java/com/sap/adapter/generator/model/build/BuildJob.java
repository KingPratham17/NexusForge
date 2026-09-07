package com.sap.adapter.generator.model.build;

import java.time.LocalDateTime;

public class BuildJob {
    public enum Status {
        INITIALIZED,
        GENERATED,
        COMPILING,
        ADK_VALIDATION,
        PACKAGING,
        SUCCESS,
        FAILURE
    }

    private String id;
    private String adapterName;
    private String workspacePath;
    private Status status = Status.INITIALIZED;
    private StringBuilder buildLogs = new StringBuilder();
    private InspectionReport inspectionReport;
    private LocalDateTime createdAt = LocalDateTime.now();
    private long durationMs;

    public BuildJob() {}

    public BuildJob(String id, String adapterName, String workspacePath) {
        this.id = id;
        this.adapterName = adapterName;
        this.workspacePath = workspacePath;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }

    public String getWorkspacePath() { return workspacePath; }
    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getBuildLogs() { return buildLogs.toString(); }
    public void appendLog(String log) { this.buildLogs.append(log).append("\n"); }

    public InspectionReport getInspectionReport() { return inspectionReport; }
    public void setInspectionReport(InspectionReport inspectionReport) { this.inspectionReport = inspectionReport; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
