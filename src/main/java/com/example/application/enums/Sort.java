package com.example.application.enums;

public enum Sort {
    DESC("По убыванию"),
    ACS("По возрастанию");

    private final String type;

    Sort(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
