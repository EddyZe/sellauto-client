package com.example.application.clients.sellauto.payloads;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfilePayload {

    private Long userId;

    private String firstName;

    private String lastName;

    private Double rating;

    private AccountPayload account;

    private List<AdProfilePayload> ads;

}
