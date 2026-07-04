package com.deb.project.airBnbApp.repository;

import com.deb.project.airBnbApp.entity.Hotel;
import com.deb.project.airBnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByOwner(User user);
}
