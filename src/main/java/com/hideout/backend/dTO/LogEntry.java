package com.hideout.backend.dTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.hideout.backend.models.LogType;

@Data
public class LogEntry {

    private LogType type = LogType.CHAT;

    @NotBlank(message = "Clipboard content cannot be empty")
    private String content;
}