package com.example.application.clients.sellauto.payloads;


import com.example.application.enums.BodyType;
import com.example.application.enums.DriveMode;
import com.example.application.enums.EngineType;
import com.example.application.enums.TransmissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class CarDetailsPayload {
    private Long carId;

    private Integer year;

    private String vin;

    private Integer mileage;

    private EngineType engineType;

    private TransmissionType transmissionType;

    private BodyType bodyType;

    private DriveMode drive;

    private BrandBasePayload brand;

    private ModelBasePayload model;

    private List<PhotoBasePayload> photos;

    private ColorBasePayload color;
}
