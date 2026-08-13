package com.iot.attendance.repository;

import com.iot.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByRfidUid(String rfidUid);
    Optional<User> findByEmail(String email);
}