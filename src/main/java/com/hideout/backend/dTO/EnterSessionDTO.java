package com.hideout.backend.dTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnterSessionDTO {

    @NotBlank(message = "Session ID cannot be empty")
    @Size(min = 6, max = 6, message = "Session ID must be exactly 6 characters long")
    private String sessionId;
    
    @NotNull(message = "Device Name must be selected")
    private String deviceType;
}