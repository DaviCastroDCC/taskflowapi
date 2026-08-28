package com.taskflow.api.domain;

import com.taskflow.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "projects",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "project_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 120)
    private String name;

    /** Short uppercase code used as a prefix for human-readable task numbers, e.g. "TF" in TF-42. */
    @Column(name = "project_key", nullable = false, length = 10)
    private String key;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "task_sequence", nullable = false)
    @Builder.Default
    private long taskSequence = 0L;

    @Version
    private long version;

    public long nextTaskNumber() {
        taskSequence += 1;
        return taskSequence;
    }
}
