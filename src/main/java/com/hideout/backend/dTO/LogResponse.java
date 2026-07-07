package com.hideout.backend.dTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LogResponse {
    private Long id;
    private String content;      
    private String sourceDevice; 
    private LocalDateTime createdAt;
    private String type;       

}