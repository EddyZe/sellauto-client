package com.example.application.enums;

public enum EngineType {

    ELECTRO("Электро"), ENGINE("ДВС");
    private final String engine;

    EngineType(String engine) {
        this.engine = engine;
    }

    @Override
    public String toString() {
        return engine;
    }
}
