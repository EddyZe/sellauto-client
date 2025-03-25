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
public class PriceBasePayload {
    private Integer priceId;

    private Double price;

    private LocalDateTime createdAt;
}
