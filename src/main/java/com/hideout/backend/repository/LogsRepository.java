package com.hideout.backend.repository;

import com.hideout.backend.models.Logs;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogsRepository extends JpaRepository<Logs, Long> {

    @Query("SELECT sl.log FROM SessionLogs sl " +
           "WHERE sl.session.sessionId = :sessionId " +
           "AND sl.log.createdAt >= :cutoffTime " +
           "ORDER BY sl.log.createdAt ASC")
    List<Logs> findLogsBySessionIdAndTimeWindow(
            @Param("sessionId") String sessionId,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

}