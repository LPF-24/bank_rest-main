package com.example.bankcards.repository;

import com.example.bankcards.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Integer> {
    Optional<Owner> findByEmail(String email);

    Optional<Owner> findById(Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
