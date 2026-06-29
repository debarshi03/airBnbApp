package com.deb.project.airBnbApp.repository;

import com.deb.project.airBnbApp.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}