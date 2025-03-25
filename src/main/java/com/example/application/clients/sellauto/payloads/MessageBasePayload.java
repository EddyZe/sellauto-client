package com.example.application.clients.sellauto.payloads;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageBasePayload {
    private Long messageId;

    private String senderName;

    private String message;

    private LocalDateTime createdAt;
}
