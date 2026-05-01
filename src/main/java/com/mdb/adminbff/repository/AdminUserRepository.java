package com.mdb.adminbff.repository;

import com.mdb.adminbff.entity.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUserEntity, UUID> {
    Optional<AdminUserEntity> findByEmail(String email);
    Optional<AdminUserEntity> findByUsername(String username);
}
