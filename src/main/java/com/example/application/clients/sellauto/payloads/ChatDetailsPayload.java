package com.example.application.clients.sellauto.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ChatDetailsPayload {

    private Long chatId;

    private List<MessageBasePayload> messages;

    private List<UserBasePayload> users;

    private AdPayload ad;
}
