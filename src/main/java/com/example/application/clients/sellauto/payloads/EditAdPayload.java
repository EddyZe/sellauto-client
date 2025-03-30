package com.example.application.clients.sellauto.payloads;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditAdPayload {
    @NotEmpty
    @Size(min = 3, max = 50)
    private String title;
    @Size(min = 5, max = 1000)
    private String description;
    @Min(1)
    private Double price;
    private Boolean isActive;
}
