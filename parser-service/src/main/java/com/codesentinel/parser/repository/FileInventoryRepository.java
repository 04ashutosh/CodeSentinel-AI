package com.codesentinel.parser.repository;

import com.codesentinel.parser.entity.FileInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInventoryRepository extends JpaRepository<FileInventoryEntity, Long> {
    List<FileInventoryEntity> findByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}