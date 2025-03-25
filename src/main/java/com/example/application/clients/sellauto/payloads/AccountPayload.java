package com.example.application.clients.sellauto.payloads;


import com.example.application.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountPayload {
    private Long accountId;

    private String email;

    private String phoneNumber;

    private boolean blocked;

    private Role role;
}
