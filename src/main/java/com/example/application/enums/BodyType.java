package com.example.application.enums;



public enum BodyType {
    SEDAN("Седан"),
    HATCHBACK("Хетчбэк"),
    UNIVERSAL("Универсал"),
    COUPE("Купе"),
    PICKUP("Пикап"),
    SUV("Внедорожник"),
    MINIVAN("Минивэн"),
    OTHER("Другой");

    private final String body;

    BodyType(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return body;
    }
}
