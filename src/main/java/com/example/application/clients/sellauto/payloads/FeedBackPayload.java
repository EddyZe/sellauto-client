package com.example.application.clients.sellauto.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FeedBackPayload {
    private Long id;

    private Double estimation;

    private String text;

    private UserBasePayload sender;

    private UserBasePayload receiver;

    private LocalDateTime createdAt;
}
