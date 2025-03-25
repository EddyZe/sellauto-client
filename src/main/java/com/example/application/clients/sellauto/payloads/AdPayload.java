package com.example.application.clients.sellauto.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AdPayload {
    private Long adId;

    private String title;

    private String description;

    private List<PriceBasePayload> prices;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private UserBasePayload user;

    private CarDetailsPayload car;
}
