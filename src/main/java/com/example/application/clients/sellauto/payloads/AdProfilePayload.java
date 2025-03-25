package com.example.application.clients.sellauto.payloads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdProfilePayload {
    private Long adId;

    private String title;

    private String description;

    private List<PriceBasePayload> prices;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private CarProfilePayload car;

    private List<ChatBasePayload> chats;
}
