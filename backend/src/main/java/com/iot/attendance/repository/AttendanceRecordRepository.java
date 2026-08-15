package com.iot.attendance.repository;

import com.iot.attendance.entity.AttendanceRecord;
import com.iot.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@Deprecated
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    /**
 * Retrieves all attendance records associated with a user.
 *
 * @param user the user whose attendance records are retrieved
 * @return the user's attendance records
 */
List<AttendanceRecord> findByUser(User user);
    /**
 * Finds attendance records with check-in times within the specified range.
 *
 * @param start the beginning of the check-in time range
 * @param end   the end of the check-in time range
 * @return attendance records with check-in times between the specified bounds
 */
List<AttendanceRecord> findByCheckInTimeBetween(OffsetDateTime start, OffsetDateTime end);
    List<AttendanceRecord> findByUserAndCheckInTimeBetween(User user, OffsetDateTime start, OffsetDateTime end);
    void deleteByUserId(UUID userId);
}
