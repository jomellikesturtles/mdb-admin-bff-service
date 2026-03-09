package com.mdb.adminbff.repository;

import com.mdb.adminbff.entity.MediaItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaItemRepository extends JpaRepository<MediaItemEntity, UUID> {
    Page<MediaItemEntity> findByTitleContaining(String title, Pageable pageable);
}
