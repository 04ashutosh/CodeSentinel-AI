package com.codesentinel.parser.repository;

import com.codesentinel.parser.entity.ParsedClass;
import com.codesentinel.parser.enums.ClassType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParsedClassRepository extends JpaRepository<ParsedClass, Long> {
    List<ParsedClass> findByProjectId(Long projectId);
    List<ParsedClass> findByProjectIdAndClassType(Long projectId, ClassType classType);
    void deleteByProjectId(Long projectId);
}