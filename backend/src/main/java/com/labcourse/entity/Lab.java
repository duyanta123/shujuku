package com.labcourse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab")
public class Lab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "lab_name", nullable = false, length = 100)
    private String labName;
    
    @Column(name = "location", length = 200)
    private String location;
    
    @Column(name = "capacity")
    private Integer capacity;
    
    // @deprecated 计划 v2.1 删除，迁移至 collegeId
    @Size(max = 100)
    @Column(name = "college", length = 100)
    private String college;

    @Column(name = "college_id")
    private Long collegeId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Lab() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public Long getCollegeId() { return collegeId; }
    public void setCollegeId(Long collegeId) { this.collegeId = collegeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
