package com.sap.adapter.generator.repository;

import com.sap.adapter.generator.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserIdOrderByLastUpdatedDesc(Long userId);
}
