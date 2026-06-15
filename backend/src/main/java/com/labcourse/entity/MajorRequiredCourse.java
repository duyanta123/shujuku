package com.labcourse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "major_required_course")
public class MajorRequiredCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public MajorRequiredCourse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}