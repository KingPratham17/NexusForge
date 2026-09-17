package com.sap.adapter.generator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String technology;
    private String direction; // e.g. receiver
    
    private Integer currentStep = 1;

    private String status = "DRAFT"; // DRAFT, IN_PROGRESS, BUILDING, READY, FAILED
    
    @Column(columnDefinition = "LONGTEXT")
    private String requirement;

    @Column(columnDefinition = "LONGTEXT")
    private String adapterSpecificationJson;

    private String buildId; // Reference to GenerationSession/Build output

    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public String getAdapterSpecificationJson() { return adapterSpecificationJson; }
    public void setAdapterSpecificationJson(String adapterSpecificationJson) { this.adapterSpecificationJson = adapterSpecificationJson; }

    public String getBuildId() { return buildId; }
    public void setBuildId(String buildId) { this.buildId = buildId; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
