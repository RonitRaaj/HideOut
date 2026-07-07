package com.hideout.backend.repository;

import com.hideout.backend.models.Logs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogsRepository extends JpaRepository<Logs, Long> {

}