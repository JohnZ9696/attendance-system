package com.iot.attendance.repository;

import com.iot.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByMssvAndIsActiveTrue(String mssv);
    Optional<Student> findByUidAndIsActiveTrue(String uid);
}
