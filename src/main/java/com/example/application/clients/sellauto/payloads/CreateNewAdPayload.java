package com.example.application.clients.sellauto.payloads;

import com.example.application.enums.BodyType;
import com.example.application.enums.DriveMode;
import com.example.application.enums.EngineType;
import com.example.application.enums.TransmissionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateNewAdPayload {
    @Min(1)
    @NotNull
    private Double price;
    @NotEmpty
    private String title;
    @NotEmpty
    @Size(min = 5)
    private String description;

    @Min(1900)
    @NotNull
    private Integer year;

    @NotEmpty
    private String vin;

    @Min(1)
    @NotNull
    private Integer mileage;

    @NotNull
    private EngineType engineType;

    @NotNull
    private TransmissionType transmissionType;

    @NotNull
    private BodyType bodyType;

    @NotNull
    private DriveMode drive;

    @NotEmpty
    private String brandTitle;

    @NotEmpty
    private String modelTitle;

    @NotEmpty
    private String colorTitle;
}
