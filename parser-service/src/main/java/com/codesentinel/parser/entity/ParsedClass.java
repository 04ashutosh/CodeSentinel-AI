package com.codesentinel.parser.entity;

import com.codesentinel.parser.enums.ClassType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parsed_classes")
public class ParsedClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "package_name")
    private String packageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", nullable = false)
    private ClassType classType;

    @Column(name = "file_path")
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String annotations;

    @Column(name = "method_count")
    private Integer methodCount;

    @Column(name = "field_count")
    private Integer fieldCount;

    @Column(name = "implements_interfaces", columnDefinition = "TEXT")
    private String implementsInterfaces;

    @Column(name = "extends_class")
    private String extendsClass;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}