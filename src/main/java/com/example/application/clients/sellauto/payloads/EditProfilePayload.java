package com.example.application.clients.sellauto.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class EditProfilePayload {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
