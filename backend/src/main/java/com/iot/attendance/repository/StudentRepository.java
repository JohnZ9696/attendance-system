package com.iot.attendance.repository;

import com.iot.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    /**
 * Finds an active student by their student identification number.
 *
 * @param mssv the student's identification number
 * @return the matching active student, if found
 */
Optional<Student> findByMssvAndIsActiveTrue(String mssv);
    /**
 * Finds an active student by UID.
 *
 * @param uid the student's UID
 * @return the matching active student, if found
 */
Optional<Student> findByUidAndIsActiveTrue(String uid);
}
