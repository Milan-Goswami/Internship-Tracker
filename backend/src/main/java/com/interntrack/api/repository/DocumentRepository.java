package com.interntrack.api.repository;

import com.interntrack.api.entity.Document;
import com.interntrack.api.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserId(Long userId);

    List<Document> findByUserIdAndDocumentType(Long userId, DocumentType documentType);
}