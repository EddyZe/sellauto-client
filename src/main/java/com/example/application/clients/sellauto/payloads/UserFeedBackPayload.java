package com.example.application.clients.sellauto.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserFeedBackPayload {
    private List<FeedBackPayload> feedbacks;
}
