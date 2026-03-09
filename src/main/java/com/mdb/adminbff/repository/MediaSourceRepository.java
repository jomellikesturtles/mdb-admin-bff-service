package com.mdb.adminbff.repository;

import com.mdb.adminbff.entity.MediaSourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaSourceRepository extends JpaRepository<MediaSourceEntity, UUID> {
    Page<MediaSourceEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<MediaSourceEntity> findByStatus(String status, Pageable pageable);
}
