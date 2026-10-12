package com.interntrack.api.repository;

import com.interntrack.api.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByCanonicalName(String canonicalName);

    boolean existsByCanonicalName(String canonicalName);

    List<Company> findByNameContainingIgnoreCase(String name);
}
