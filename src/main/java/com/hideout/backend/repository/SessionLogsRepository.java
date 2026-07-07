package com.hideout.backend.repository;
import com.hideout.backend.models.SessionLogs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SessionLogsRepository extends JpaRepository<SessionLogs, Long> {

    List<SessionLogs> findBySessionSessionIdOrderByLogCreatedAtAsc(String sessionId);


}