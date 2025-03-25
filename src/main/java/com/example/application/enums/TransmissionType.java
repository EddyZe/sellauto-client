package com.example.application.enums;

public enum TransmissionType {
    AUTO("АКПП"), MECHANIC("МКПП"), ROBOT("Робот"), VARIATOR("Вариатор");

    private final String transmission;

    TransmissionType(String transmission) {
        this.transmission = transmission;
    }


    @Override
    public String toString() {
        return transmission;
    }
}
